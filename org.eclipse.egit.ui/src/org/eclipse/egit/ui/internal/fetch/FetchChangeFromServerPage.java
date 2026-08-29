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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.eclipse.egit.core.internal.hosts.PullRequestInfo;
import org.eclipse.egit.core.settings.GitSettings;
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
import org.eclipse.swt.widgets.Control;

/**
 * Fetch a change from a git server of a particular {@link ServerType}.
 */
public class FetchChangeFromServerPage extends AbstractFetchFromHostPage {

	private static final Pattern DIGITS = Pattern.compile("\\d+"); //$NON-NLS-1$

	private static final String WILDCARD = ".*"; //$NON-NLS-1$

	private static final int LOOKUP_DELAY_MILLIS = 300;

	private final GitServer server;

	private final Repository repository;

	/** Source branches by URI and change number; empty if unknown. */
	private final Map<String, String> sourceBranches = new HashMap<>();

	private Job lookupJob;

	/**
	 * Creates a new {@link FetchChangeFromServerPage}.
	 *
	 * @param server
	 *            {@ServerType} to fetch from
	 * @param repository
	 *            to fetch into
	 * @param initialText
	 *            initial value for the ref field
	 */
	public FetchChangeFromServerPage(GitServer server, Repository repository,
			String initialText) {
		super(repository, initialText, server.getChangeLabel(),
				server.getChangeNameSingular(), server.getChangeNamePlural(),
				false);
		this.server = server;
		this.repository = repository;
	}

	@Override
	public void dispose() {
		cancelLookup();
		super.dispose();
	}

	@Override
	boolean doFetch() {
		cancelLookup();
		return super.doFetch();
	}

	@Override
	protected void changeSelected(String uri, Change change) {
		if (uri == null || uri.isEmpty()
				|| !GitSettings.isGitServerBranchNameLookupEnabled()) {
			return;
		}
		long number = change.getChangeNumber();
		String key = uri + '\n' + number;
		String known = sourceBranches.get(key);
		if (known != null) {
			if (!known.isEmpty()) {
				suggestBranchName(uri, change, branchName(number, known));
			}
			return;
		}
		cancelLookup();
		Job job = new Job(MessageFormat.format(
				UIText.FetchChangeFromServerPage_BranchNameLookupJob,
				server.getChangeNameSingular())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String branch = lookupSourceBranch(uri, number);
				Control control = getControl();
				if (monitor.isCanceled() || control == null
						|| control.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				control.getDisplay().asyncExec(() -> {
					if (control.isDisposed()) {
						return;
					}
					sourceBranches.put(key, branch);
					if (!branch.isEmpty()) {
						suggestBranchName(uri, change,
								branchName(number, branch));
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		lookupJob = job;
		job.schedule(LOOKUP_DELAY_MILLIS);
	}

	private void cancelLookup() {
		if (lookupJob != null) {
			lookupJob.cancel();
			lookupJob = null;
		}
	}

	private String lookupSourceBranch(String uri, long number) {
		try {
			PullRequestInfo info = PullRequestInfo.lookup(repository,
					server.getType(), new URIish(uri), number);
			return info == null ? "" : info.getSourceBranch(); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			return ""; //$NON-NLS-1$
		}
	}

	private String branchName(long number, String sourceBranch) {
		return MessageFormat.format(server.getBranchNameWithSource(),
				Long.toString(number), sourceBranch);
	}

	@Override
	protected String getSettingsKey() {
		return '.' + server.getType().getId();
	}

	@Override
	Set<String> determineUris(Repository repo, String defaultUri) {
		Set<String> uris = new HashSet<>();
		try {
			GitHosts.getServerConfigs(repo.getConfig(), server.getType())
					.forEach(rc -> {
				uris.add(rc.getURIs().get(0).toPrivateString());
				for (URIish u : rc.getPushURIs()) {
							if (server.getType()
									.uriMatches(u.toPrivateString())) {
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
		return new ChangeList(repo, uri, this::changeFromRef);
	}

	@Override
	Change changeFromRef(String refName) {
		long changeId = server.getType().fromRef(refName);
		if (changeId < 0) {
			return null;
		}
		return new PullRequest(changeId, server);
	}

	@Override
	Change changeFromString(String input) {
		long changeId = server.getType().fromString(input);
		if (changeId < 0) {
			return null;
		}
		return new PullRequest(changeId, server);
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
					return Pattern.compile(server.getType().getRefPrefix()
							+ matcher.group() + WILDCARD);
				}
			} else {
				changeNumber = change.getChangeNumber();
			}
			if (changeNumber > 0) {
				return Pattern.compile(server.getType().getRefPrefix()
						+ changeNumber + WILDCARD);
			}
		} catch (PatternSyntaxException e) {
			// Ignore and return default pattern below.
		}
		return UIUtils.createProposalPattern(input);
	}

	private static class PullRequest implements Change {

		private final long prNumber;

		private final GitServer server;

		public PullRequest(long prNumber, GitServer server) {
			this.prNumber = prNumber;
			this.server = server;
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
			return server.getType().toFetchRef(prNumber);
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
			String label = MessageFormat.format(server.getProposalLabel(),
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
			return MessageFormat.format(server.getBranchName(),
					Long.toString(getChangeNumber()));
		}

		@Override
		public String completeId() {
			return Long.toString(getChangeNumber());
		}
	}

	private static class ChangeList extends AsynchronousListOperation<Change> {

		private final Function<String, ? extends Change> fromRef;

		public ChangeList(Repository repository, String uriText,
				Function<String, ? extends Change> fromRef) {
			super(repository, uriText);
			this.fromRef = fromRef;
		}

		@Override
		protected Collection<Change> convert(Collection<Ref> refs) {
			List<Change> changes = new ArrayList<>();
			for (Ref ref : refs) {
				Change change = fromRef.apply(ref.getName());
				if (change != null) {
					changes.add(change);
				}
			}
			Collections.sort(changes, Collections.reverseOrder());
			return new LinkedHashSet<>(changes);
		}
	}
}
