/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.AsynchronousListOperation;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Fetch a pull request from Github.
 */
public class FetchGithubPullRequestPage extends AbstractFetchFromHostPage {

	private static final Pattern GITHUB_PR_URL_PATTERN = Pattern
			.compile("https?://github.com/([^/]+/)+pull/(\\d+)"); //$NON-NLS-1$

	private static final Pattern GITHUB_PR_REF_PATTERN = Pattern
			.compile("refs/pull/(\\d+)/head"); //$NON-NLS-1$

	private static final Pattern GITHUB_PR_INPUT_PATTERN = Pattern
			.compile("refs/pull/(\\d+)(?:/head)?"); //$NON-NLS-1$

	private static final String GITHUB_PR_REF = "refs/pull/{0}/head"; //$NON-NLS-1$

	private static final String GITHUB_PR_PREFIX = "refs/pull/"; //$NON-NLS-1$

	private static final Pattern DIGITS = Pattern.compile("\\d+"); //$NON-NLS-1$

	private static final String WILDCARD = ".*"; //$NON-NLS-1$

	/**
	 * Creates a new {@link FetchGithubPullRequestPage}.
	 *
	 * @param repository
	 *            to fetch into
	 * @param initialText
	 *            initial value for the ref field
	 */
	public FetchGithubPullRequestPage(Repository repository, String initialText) {
		super(repository, initialText,
				UIText.FetchGithubPullRequestPage_ChangeLabel,
				UIText.FetchGithubPullRequestPage_ChangeNameSingular,
				UIText.FetchGithubPullRequestPage_ChangeNamePlural,
				false);
	}

	@Override
	Set<String> determineUris(Repository repo, String defaultUri) {
		Set<String> uris = new HashSet<>();
		try {
			GitHosts.getGithubConfigs(repo.getConfig()).forEach(rc -> {
				uris.add(rc.getURIs().get(0).toPrivateString());
				for (URIish u : rc.getPushURIs()) {
					if (GitHosts.isGithubUri(u.toPrivateString())) {
						uris.add(u.toPrivateString());
					}
				}
			});
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, false);
			setErrorMessage(e.getMessage());
			return Collections.emptySet();
		}
		return uris;
	}

	@Override
	ChangeList createChangeList(Repository repo, String uri) {
		return new ChangeList(repo, uri);
	}

	static Change fromRef(String refName) {
		try {
			if (refName == null) {
				return null;
			}
			Matcher m = GITHUB_PR_REF_PATTERN.matcher(refName);
			if (!m.matches() || m.group(1) == null) {
				return null;
			}
			return new GithubChange(Long.parseLong(m.group(1)));
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			// if we can't parse this, just return null
			return null;
		}
	}

	@Override
	Change changeFromRef(String refName) {
		return fromRef(refName);
	}

	static Change fromString(String input) {
		if (input == null) {
			return null;
		}
		try {
			Matcher matcher = GITHUB_PR_URL_PATTERN.matcher(input);
			if (matcher.matches()) {
				return new GithubChange(Long.parseLong(matcher.group(2)));
			}
			matcher = GITHUB_PR_INPUT_PATTERN.matcher(input);
			if (matcher.matches()) {
				return new GithubChange(Long.parseLong(matcher.group(1)));
			}
			matcher = DIGITS.matcher(input);
			if (matcher.matches()) {
				return new GithubChange(Long.parseLong(input));
			}
		} catch (NumberFormatException e) {
			// Numerical overflow?
		}
		return null;
	}

	@Override
	Change changeFromString(String input) {
		return fromString(input);
	}

	@Override
	Defaults getDefaults(String initialText) {
		if (initialText == null) {
			return null;
		}
		return new Defaults(null, null, changeFromString(initialText.trim()));
	}

	@Override
	Pattern getProposalPattern(String input) {
		Change change = changeFromString(input);
		long changeNumber = -1;
		try {
			if (change == null) {
				Matcher matcher = DIGITS.matcher(input);
				if (matcher.find()) {
					return Pattern.compile(
							GITHUB_PR_PREFIX + matcher.group() + WILDCARD);
				}
			} else {
				changeNumber = change.getChangeNumber();
			}
			if (changeNumber > 0) {
				return Pattern
						.compile(GITHUB_PR_PREFIX + changeNumber + WILDCARD);
			}
		} catch (PatternSyntaxException e) {
			// Ignore and return default pattern below.
		}
		return UIUtils.createProposalPattern(input);
	}

	private static class GithubChange implements Change {

		private final long prNumber;

		public GithubChange(long prNumber) {
			this.prNumber = prNumber;
		}

		@Override
		public String getRefName() {
			return toString();
		}

		@Override
		public long getChangeNumber() {
			return prNumber;
		}

		@Override
		public String toString() {
			return MessageFormat.format(GITHUB_PR_REF, Long.toString(prNumber));
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Change)) {
				return false;
			}
			return compareTo((Change) obj) == 0;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(prNumber);
		}

		@Override
		public int compareTo(Change o) {
			return Long.compare(this.prNumber, o.getChangeNumber());
		}

		@Override
		public IContentProposal getProposal() {
			String label = MessageFormat.format(
					UIText.FetchGithubPullRequestPage_ContentAssistLabel,
					Long.toString(getChangeNumber()));
			return new ContentProposal(getRefName(), label, null, 0);
		}

		@Override
		public boolean isComplete() {
			return true;
		}

		@Override
		public Change complete(CancelableFuture<Collection<Change>> list,
				String uri, IProgressMonitor monitor) {
			return this;
		}

		@Override
		public Change complete(Collection<Change> changes) {
			return null;
		}

		@Override
		public String getBranchSuggestion() {
			return MessageFormat.format(
					UIText.FetchGithubPullRequestPage_SuggestedRefNamePattern,
					Long.toString(getChangeNumber()));
		}

		@Override
		public String completeId() {
			return Long.toString(getChangeNumber());
		}
	}

	private static class ChangeList extends AsynchronousListOperation<Change> {

		public ChangeList(Repository repository, String uriText) {
			super(repository, uriText);
		}

		@Override
		protected Collection<Change> convert(Collection<Ref> refs) {
			List<Change> changes = new ArrayList<>();
			for (Ref ref : refs) {
				Change change = fromRef(ref.getName());
				if (change != null) {
					changes.add(change);
				}
			}
			Collections.sort(changes, Collections.reverseOrder());
			return new LinkedHashSet<>(changes);
		}
	}
}
