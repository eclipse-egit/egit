/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FormatJob.FormatResult;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.Region;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.osgi.util.NLS;

/**
 * Class to build and format commit info in History View
 */
public class CommitInfoBuilder {

	private static final String LF = "\n"; //$NON-NLS-1$

	private static final int MAXBRANCHES = 20;

	private PlotCommit<?> commit;

	private final Repository db;

	private final boolean fill;

	private final Collection<Ref> allRefs;

	private final PreferenceBasedDateFormatter dateFormatter;

	/**
	 * @param db the repository
	 * @param commit the commit the info should be shown for
	 * @param fill whether to fill the available space
	 * @param allRefs all Ref's to examine regarding marge bases
	 */
	public CommitInfoBuilder(Repository db, PlotCommit commit, boolean fill,
			Collection<Ref> allRefs) {
		this.db = db;
		this.commit = commit;
		this.fill = fill;
		this.allRefs = allRefs;
		this.dateFormatter = PreferenceBasedDateFormatter.create();
	}

	/**
	 * Retrieves and formats the commit info.
	 *
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return formatted commit info
	 * @throws IOException
	 */
	public FormatResult format(IProgressMonitor monitor) throws IOException {
		boolean trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
		monitor.setTaskName(UIText.CommitMessageViewer_FormattingMessageTaskName);
		final StringBuilder d = new StringBuilder();
		final PersonIdent author = commit.getAuthorIdent();
		final PersonIdent committer = commit.getCommitterIdent();
		List<GitCommitReference> hyperlinks = new ArrayList<>();
		d.append(UIText.CommitMessageViewer_commit);
		d.append(' ');
		d.append(commit.getId().name());
		d.append(LF);

		addPersonIdent(d, author, UIText.CommitMessageViewer_author);
		addPersonIdent(d, committer, UIText.CommitMessageViewer_committer);

		for (int i = 0; i < commit.getParentCount(); i++) {
			addCommit(d, (SWTCommit) commit.getParent(i),
					UIText.CommitMessageViewer_parent, hyperlinks);
		}

		for (int i = 0; i < commit.getChildCount(); i++) {
			addCommit(d, (SWTCommit) commit.getChild(i),
					UIText.CommitMessageViewer_child, hyperlinks);
		}

		if(Activator.getDefault().getPreferenceStore().getBoolean(
				UIPreferences.HISTORY_SHOW_BRANCH_SEQUENCE)) {
			try (RevWalk rw = new RevWalk(db)) {
				List<Ref> branches = getBranches(commit, allRefs, db, monitor);
				Collections.sort(branches,
						CommonUtils.REF_ASCENDING_COMPARATOR);
				if (!branches.isEmpty()) {
					d.append(UIText.CommitMessageViewer_branches);
					d.append(": "); //$NON-NLS-1$
					int count = 0;
					for (Iterator<Ref> i = branches.iterator(); i.hasNext();) {
						Ref head = i.next();
						RevCommit p;
						p = rw.parseCommit(head.getObjectId());
						addLink(d, formatHeadRef(head), hyperlinks, p);
						if (i.hasNext()) {
							if (count++ <= MAXBRANCHES) {
								d.append(", "); //$NON-NLS-1$
							} else {
								d.append(NLS.bind(UIText.CommitMessageViewer_MoreBranches, Integer.valueOf(branches.size() - MAXBRANCHES)));
								break;
							}
						}
					}
					d.append(LF);
				}
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		String tagsString = getTagsString();
		if (tagsString.length() > 0) {
			d.append(UIText.CommitMessageViewer_tags);
			d.append(": "); //$NON-NLS-1$
			d.append(tagsString);
			d.append(LF);
		}

		if (Activator.getDefault().getPreferenceStore().getBoolean(
				UIPreferences.HISTORY_SHOW_TAG_SEQUENCE)) {
			try (RevWalk rw = new RevWalk(db)) {
				monitor.setTaskName(UIText.CommitMessageViewer_GettingPreviousTagTaskName);
				addTag(d, UIText.CommitMessageViewer_follows, rw,
						getNextTag(false, monitor), hyperlinks);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}

			try (RevWalk rw = new RevWalk(db)) {
				monitor.setTaskName(UIText.CommitMessageViewer_GettingNextTagTaskName);
				addTag(d, UIText.CommitMessageViewer_precedes, rw,
						getNextTag(true, monitor), hyperlinks);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		d.append(LF);
		int headerEnd = d.length();
		String msg = commit.getFullMessage().trim();
		// Find start of footer:
		int footerStart = CommonUtils.getFooterOffset(msg);
		if (footerStart >= 0) {
			if (fill) {
				String footer = msg.substring(footerStart);
				msg = msg.substring(0, footerStart);
				msg = msg.replaceAll("([\\w.,; \t])\n(\\w)", "$1 $2") //$NON-NLS-1$ //$NON-NLS-2$
						+ footer;
				footerStart = headerEnd + msg.length() - footer.length();
			} else {
				footerStart = headerEnd + footerStart;
			}
		} else if (fill) {
			msg = msg.replaceAll("([\\w.,; \t])\n(\\w)", "$1 $2"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		d.append(msg);
		if (!msg.endsWith(LF))
			d.append(LF);

		if (trace)
			GitTraceLocation.getTrace().traceExit(
					GitTraceLocation.HISTORYVIEW.getLocation());
		return new FormatResult(d.toString(), hyperlinks, headerEnd,
				footerStart >= 0 ? footerStart : d.length());
	}

	private void addLink(StringBuilder d, String linkLabel,
			Collection<GitCommitReference> hyperlinks, RevCommit to) {
		if (to != null) {
			hyperlinks.add(new GitCommitReference(to,
					new Region(d.length(), linkLabel.length())));
		}
		d.append(linkLabel);
	}

	private void addLink(StringBuilder d, Collection<GitCommitReference> hyperlinks,
 RevCommit to) {
		addLink(d, to.getId().name(), hyperlinks, to);
	}

	private void addPersonIdent(StringBuilder d, PersonIdent ident,
			String label) {
		if (ident != null) {
			d.append(label).append(": "); //$NON-NLS-1$
			d.append(ident.getName().trim());
			d.append(" <").append(ident.getEmailAddress().trim()).append("> "); //$NON-NLS-1$ //$NON-NLS-2$
			d.append(dateFormatter.formatDate(ident));
			d.append(LF);
		}
	}

	private void addCommit(StringBuilder d, SWTCommit gitcommit, String label,
			List<GitCommitReference> hyperlinks) throws IOException {
		if (gitcommit != null) {
			d.append(label).append(": "); //$NON-NLS-1$
			gitcommit.parseBody();
			addLink(d, hyperlinks, gitcommit);
			d.append(" (").append(gitcommit.getShortMessage()).append(')'); //$NON-NLS-1$
			d.append(LF);
		}
	}

	private void addTag(StringBuilder d, String label, RevWalk walk, Ref tag,
			List<GitCommitReference> hyperlinks) throws IOException {
		if (tag != null) {
			d.append(label).append(": "); //$NON-NLS-1$
			RevCommit p = walk.parseCommit(tag.getObjectId());
			addLink(d, formatTagRef(tag), hyperlinks, p);
			d.append(LF);
		}
	}

	/**
	 * @param commit
	 * @param allRefs
	 * @param db
	 * @param monitor
	 * @return List of heads from those current commit is reachable
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private static List<Ref> getBranches(RevCommit commit,
			Collection<Ref> allRefs, Repository db, IProgressMonitor monitor)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		EclipseGitProgressTransformer progress = new EclipseGitProgressTransformer(
				SubMonitor.convert(monitor, allRefs.size()));
		try (RevWalk revWalk = new RevWalk(db)) {
			revWalk.setRetainBody(false);
			return RevWalkUtils.findBranchesReachableFrom(commit, revWalk,
					allRefs, progress);
		}
	}

	private String formatHeadRef(Ref ref) {
		final String name = ref.getName();
		if (name.startsWith(Constants.R_HEADS))
			return name.substring(Constants.R_HEADS.length());
		else if (name.startsWith(Constants.R_REMOTES))
			return name.substring(Constants.R_REMOTES.length());
		return name;
	}

	private String formatTagRef(Ref ref) {
		final String name = ref.getName();
		if (name.startsWith(Constants.R_TAGS))
			return name.substring(Constants.R_TAGS.length());
		return name;
	}

	private String getTagsString() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (Ref ref : db.getRefDatabase().getRefsByPrefix(Constants.R_TAGS)) {
			ObjectId target = ref.getPeeledObjectId();
			if (target == null) {
				target = ref.getObjectId();
			}
			if (target != null && target.equals(commit)) {
				if (sb.length() > 0) {
					sb.append(", "); //$NON-NLS-1$
				}
				sb.append(formatTagRef(ref));
			}
		}
		return sb.toString();
	}

	/**
	 * Finds next door tagged revision. Searches forwards (in descendants) or
	 * backwards (in ancestors)
	 *
	 * @param searchDescendant
	 *            if <code>false</code>, will search for tagged revision in
	 *            ancestors
	 * @param monitor
	 * @return {@link Ref} or <code>null</code> if no tag found
	 * @throws IOException
	 * @throws OperationCanceledException
	 */
	private Ref getNextTag(boolean searchDescendant, IProgressMonitor monitor)
			throws IOException, OperationCanceledException {
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		try (RevWalk revWalk = new RevWalk(db)) {
			revWalk.setRetainBody(false);
			Ref tagRef = null;

			for (Ref ref : db.getRefDatabase()
					.getRefsByPrefix(Constants.R_TAGS)) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				// both RevCommits must be allocated using same RevWalk
				// instance, otherwise isMergedInto returns wrong result!
				RevCommit current = revWalk.parseCommit(commit);
				// tags can point to any object, we only want tags pointing at
				// commits
				RevObject any = revWalk
						.peel(revWalk.parseAny(ref.getObjectId()));
				if (!(any instanceof RevCommit))
					continue;
				RevCommit newTag = (RevCommit) any;
				if (newTag.getId().equals(commit))
					continue;

				// check if newTag matches our criteria
				if (isMergedInto(revWalk, newTag, current, searchDescendant)) {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					if (tagRef != null) {
						RevCommit oldTag = revWalk
								.parseCommit(tagRef.getObjectId());

						// both oldTag and newTag satisfy search criteria, so
						// taking the closest one
						if (isMergedInto(revWalk, oldTag, newTag,
								searchDescendant))
							tagRef = ref;
					} else
						tagRef = ref;
				}
			}
			return tagRef;
		}
	}

	/**
	 * @param rw
	 * @param base
	 * @param tip
	 * @param swap
	 *            if <code>true</code>, base and tip arguments are swapped
	 * @return <code>true</code> if there is a path directly from tip to base
	 *         (and thus base is fully merged into tip); <code>false</code>
	 *         otherwise.
	 * @throws IOException
	 */
	private boolean isMergedInto(final RevWalk rw, final RevCommit base,
			final RevCommit tip, boolean swap) throws IOException {
		return !swap ? rw.isMergedInto(base, tip) : rw.isMergedInto(tip, base);
	}

}
