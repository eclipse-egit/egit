/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

/**
 * Commit search query class that runs a {@link RevWalk} for all
 * {@link Repository} objects included in the {@link CommitSearchSettings} and
 * matches all {@link RevCommit} objects against the search settings.
 */
public class CommitSearchQuery implements ISearchQuery {

	private abstract class SearchMatcher {

		abstract boolean matches(Pattern pattern, RevCommit commit);

		protected boolean matches(Pattern pattern, String input) {
			return input != null && input.length() > 0
					&& pattern.matcher(input).find();
		}

	}

	private class AuthorMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			PersonIdent author = commit.getAuthorIdent();
			if (author != null)
				return matches(pattern, author.getName())
						|| matches(pattern, author.getEmailAddress());
			else
				return false;
		}
	}

	private class CommitterMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			PersonIdent committer = commit.getCommitterIdent();
			if (committer != null)
				return matches(pattern, committer.getName())
						|| matches(pattern, committer.getEmailAddress());
			else
				return false;
		}
	}

	private class MessageMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			return matches(pattern, commit.getFullMessage());
		}
	}

	private class CommitNameMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			return matches(pattern, commit.name());
		}

	}

	private class TreeMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			RevTree tree = commit.getTree();
			return tree != null ? matches(pattern, tree.name()) : false;
		}
	}

	private class ParentMatcher extends SearchMatcher {

		@Override
		public boolean matches(Pattern pattern, RevCommit commit) {
			for (RevCommit parent : commit.getParents())
				if (matches(pattern, parent.name()))
					return true;
			return false;
		}

	}

	private CommitSearchResult result = new CommitSearchResult(this);

	private CommitSearchSettings settings;

	private List<SearchMatcher> matchers = new LinkedList<>();

	/**
	 * Create git search query
	 *
	 * @param settings
	 */
	public CommitSearchQuery(CommitSearchSettings settings) {
		this.settings = settings;

		if (this.settings.isMatchAuthor())
			matchers.add(new AuthorMatcher());
		if (this.settings.isMatchCommitter())
			matchers.add(new CommitterMatcher());
		if (this.settings.isMatchMessage())
			matchers.add(new MessageMatcher());
		if (this.settings.isMatchCommit())
			matchers.add(new CommitNameMatcher());
		if (this.settings.isMatchTree())
			matchers.add(new TreeMatcher());
		if (this.settings.isMatchParents())
			matchers.add(new ParentMatcher());
	}

	/**
	 * Get text pattern being searched for
	 *
	 * @return pattern
	 */
	public String getPattern() {
		return this.settings.getTextPattern();
	}

	private Repository getRepository(String name) throws IOException {
		Repository repository = null;
		File path = new File(name);
		if (path.exists())
			repository = Activator.getDefault().getRepositoryCache()
					.lookupRepository(path);
		return repository;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor)
			throws OperationCanceledException {
		this.result.removeAll();

		Pattern pattern = PatternUtils.createPattern(
				this.settings.getTextPattern(),
				this.settings.isCaseSensitive(), this.settings.isRegExSearch());
		List<String> paths = settings.getRepositories();
		try {
			for (String path : paths) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();

				Repository repo = getRepository(path);
				if (repo == null)
					continue;

				monitor.setTaskName(MessageFormat.format(
						UIText.CommitSearchQuery_TaskSearchCommits, repo
								.getDirectory().getParentFile().getName()));
				walkRepository(repo, pattern, monitor);
			}
		} catch (IOException e) {
			org.eclipse.egit.ui.Activator.handleError(
					"Error searching commits", e, true); //$NON-NLS-1$
		}
		return Status.OK_STATUS;
	}

	private void walkRepository(Repository repository, Pattern pattern,
			IProgressMonitor monitor) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			walk.setRetainBody(true);
			List<RevCommit> commits = new LinkedList<>();
			if (this.settings.isAllBranches()) {
				for (Ref ref : repository.getRefDatabase()
						.getRefsByPrefix(Constants.R_HEADS))
					if (!ref.isSymbolic())
						commits.add(walk.parseCommit(ref.getObjectId()));
				for (Ref ref : repository.getRefDatabase()
						.getRefsByPrefix(Constants.R_REMOTES))
					if (!ref.isSymbolic())
						commits.add(walk.parseCommit(ref.getObjectId()));
			} else {
				ObjectId headCommit = repository.resolve(Constants.HEAD);
				if (headCommit != null)
					commits.add(walk.parseCommit(headCommit));
			}

			if (!commits.isEmpty()) {
				walk.markStart(commits);
				for (RevCommit commit : walk) {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					for (SearchMatcher matcher : this.matchers)
						if (matcher.matches(pattern, commit)) {
							result.addResult(new RepositoryCommit(repository,
									commit));
							break;
						}
				}
			}
		}
	}

	/**
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	@Override
	public String getLabel() {
		return UIText.CommitSearchQuery_Label;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	@Override
	public boolean canRerun() {
		return true;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	@Override
	public boolean canRunInBackground() {
		return true;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	@Override
	public ISearchResult getSearchResult() {
		return this.result;
	}

}
