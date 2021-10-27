/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Marc Khouzam (Ericsson)  - Add an option not to checkout the new branch
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.AsynchronousListOperation;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Fetch a change from Gerrit.
 */
public class FetchGerritChangePage extends AbstractFetchFromHostPage {

	private static final String GERRIT_CHANGE_REF_PREFIX = "refs/changes/"; //$NON-NLS-1$

	private static final Pattern GERRIT_FETCH_PATTERN = Pattern.compile(
			"git fetch \"?(\\w+:[^\"\\s]+)\"? (refs/changes/\\d+/\\d+/\\d+) && git (\\w+(?:-\\w+)?(?: -b)?).* FETCH_HEAD"); //$NON-NLS-1$

	private static final Pattern GERRIT_URL_PATTERN = Pattern.compile(
			"(?:https?://\\S+?/|/)?([1-9][0-9]*)(?:/([1-9][0-9]*)(?:/([1-9][0-9]*)(?:\\.\\.\\d+)?)?)?(?:/\\S*)?"); //$NON-NLS-1$

	private static final Pattern GERRIT_CHANGE_REF_PATTERN = Pattern
			.compile("refs/changes/(\\d\\d)/([1-9][0-9]*)(?:/([1-9][0-9]*)?)?"); //$NON-NLS-1$

	private static final Pattern DIGITS = Pattern
			.compile("\\d+(?:/\\d+)?"); //$NON-NLS-1$

	private static final String WILDCARD = ".*"; //$NON-NLS-1$

	private static final SimpleDateFormat SIMPLE_TIMESTAMP = new SimpleDateFormat(
			"yyyyMMddHHmmss"); //$NON-NLS-1$

	/**
	 * Creates a new {@link FetchGerritChangePage}.
	 *
	 * @param repository
	 *            to fetch into
	 * @param initialText
	 *            initial value for the ref field
	 */
	public FetchGerritChangePage(Repository repository, String initialText) {
		super(repository, initialText, UIText.FetchGerritChangePage_ChangeLabel,
				UIText.FetchGerritChangePage_ChangeNameSingular,
				UIText.FetchGerritChangePage_ChangeNamePlural,
				true);
	}

	@Override
	Set<String> determineUris(Repository repo, String defaultUri) {
		Set<String> uris = new HashSet<>();
		try {
			for (RemoteConfig rc : RemoteConfig
					.getAllRemoteConfigs(repo.getConfig())) {
				if (GerritUtil.isGerritFetch(rc)) {
					if (rc.getURIs().size() > 0) {
						uris.add(rc.getURIs().get(0).toPrivateString());
					}
					for (URIish u : rc.getPushURIs()) {
						uris.add(u.toPrivateString());
					}
				}

			}
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
			Matcher m = GERRIT_CHANGE_REF_PATTERN.matcher(refName);
			if (!m.matches() || m.group(3) == null) {
				return null;
			}
			Integer subdir = Integer.valueOf(m.group(1));
			int changeNumber = Integer.parseInt(m.group(2));
			if (subdir.intValue() != changeNumber % 100) {
				return null;
			}
			Integer patchSetNumber = Integer.valueOf(m.group(3));
			return new GerritChange(refName, changeNumber, patchSetNumber);
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
			Matcher matcher = GERRIT_URL_PATTERN.matcher(input);
			if (matcher.matches()) {
				String first = matcher.group(1);
				String second = matcher.group(2);
				String third = matcher.group(3);
				if (second != null && !second.isEmpty()) {
					if (third != null && !third.isEmpty()) {
						return new GerritChange(Integer.parseInt(second),
								Integer.parseInt(third));
					} else if (input.startsWith("http")) { //$NON-NLS-1$
						// A URL ending with two digits: take the first as
						// change number
						return new GerritChange(Integer.parseInt(first),
								Integer.parseInt(second));
					} else {
						// Take the numerically larger. Might be a fragment like
						// /10/65510 as in refs/changes/10/65510/6, or /65510/6
						// as in https://git.eclipse.org/r/#/c/65510/6. This is
						// a heuristic, it might go wrong on a Gerrit where
						// there are not many changes (yet), and one of them has
						// many patch sets.
						int firstNum = Integer.parseInt(first);
						int secondNum = Integer.parseInt(second);
						if (firstNum > secondNum) {
							return new GerritChange(firstNum, secondNum);
						} else {
							return new GerritChange(secondNum);
						}
					}
				} else {
					return new GerritChange(Integer.parseInt(first));
				}
			}
			matcher = GERRIT_CHANGE_REF_PATTERN.matcher(input);
			if (matcher.matches()) {
				int firstNum = Integer.parseInt(matcher.group(2));
				String second = matcher.group(3);
				if (second != null) {
					return new GerritChange(firstNum, Integer.parseInt(second));
				} else {
					return new GerritChange(firstNum);
				}
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
		String defaultUri = null;
		CheckoutMode defaultCommand = CheckoutMode.CREATE_BRANCH;
		Change defaultChange = null;
		Matcher matcher = GERRIT_FETCH_PATTERN.matcher(initialText);
		if (matcher.matches()) {
			defaultUri = matcher.group(1);
			defaultChange = changeFromRef(matcher.group(2));
			String cmd = matcher.group(3);

			if ("checkout".equals(cmd)) { //$NON-NLS-1$
				defaultCommand = CheckoutMode.CHECKOUT_FETCH_HEAD;
			} else if ("cherry-pick".equals(cmd)) { //$NON-NLS-1$
				defaultCommand = CheckoutMode.CHERRY_PICK;
			}
		} else {
			defaultChange = changeFromString(initialText.trim());
		}
		return new Defaults(defaultUri, defaultCommand, defaultChange);
	}

	@Override
	Pattern getProposalPattern(String input) {
		Change change = changeFromString(input);
		long changeNumber = -1;
		try {
			if (change == null) {
				Matcher matcher = DIGITS.matcher(input);
				if (matcher.find()) {
					return Pattern.compile(GERRIT_CHANGE_REF_PREFIX + "(../)?" //$NON-NLS-1$
							+ matcher.group() + WILDCARD);
				} else if (input.startsWith(GERRIT_CHANGE_REF_PREFIX)
						|| GERRIT_CHANGE_REF_PREFIX.startsWith(input)) {
					return null; // Match all
				}
			} else {
				changeNumber = change.getChangeNumber();
			}
			if (changeNumber > 0) {
				return Pattern.compile(GERRIT_CHANGE_REF_PREFIX + "../" //$NON-NLS-1$
						+ changeNumber + WILDCARD);
			}
		} catch (PatternSyntaxException e) {
			// Ignore and return default pattern below.
		}
		return UIUtils.createProposalPattern(input);
	}

	static class GerritChange implements Change {

		private final String refName;

		private final long changeNumber;

		private final Integer patchSetNumber;

		public GerritChange(long changeNumber) {
			this(null, changeNumber, null);
		}

		public GerritChange(long changeNumber, int patchSetNumber) {
			this(null, changeNumber, Integer.valueOf(patchSetNumber));
		}

		public GerritChange(String refName, long changeNumber,
				Integer patchSetNumber) {
			String ref = refName;
			if (ref == null && patchSetNumber != null) {
				int subDir = (int) (changeNumber % 100);
				ref = GERRIT_CHANGE_REF_PREFIX
						+ String.format("%02d", Integer.valueOf(subDir)) //$NON-NLS-1$
						+ '/' + changeNumber + '/' + patchSetNumber;
			}
			this.refName = ref;
			this.changeNumber = changeNumber;
			this.patchSetNumber = patchSetNumber;
		}

		@Override
		public String getRefName() {
			return refName;
		}

		@Override
		public long getChangeNumber() {
			return changeNumber;
		}

		public Integer getPatchSetNumber() {
			return patchSetNumber;
		}

		@Override
		public String toString() {
			return refName;
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
			return Long.hashCode(changeNumber) * 31
					+ Objects.hash(patchSetNumber);
		}

		@Override
		public int compareTo(Change o) {
			int changeDiff = Long.compare(this.changeNumber,
					o.getChangeNumber());
			if (changeDiff == 0 && o instanceof GerritChange) {
				GerritChange other = (GerritChange) o;
				if (patchSetNumber == null) {
					return other.getPatchSetNumber() != null ? -1 : 0;
				} else if (other.getPatchSetNumber() == null) {
					return 1;
				}
				changeDiff = this.patchSetNumber
						.compareTo(other.getPatchSetNumber());
			}
			return changeDiff;
		}

		@Override
		public IContentProposal getProposal() {
			String label = Long.toString(getChangeNumber()) + " - " //$NON-NLS-1$
					+ getPatchSetNumber();
			String description = MessageFormat.format(
					UIText.FetchGerritChangePage_ContentAssistDescription,
					getPatchSetNumber().toString(),
					Long.toString(getChangeNumber()));
			return new ContentProposal(getRefName(), label, description, 0);
		}

		@Override
		public boolean isComplete() {
			return getPatchSetNumber() != null;
		}

		@Override
		public Change complete(CancelableFuture<Collection<Change>> list,
				String uri, IProgressMonitor monitor) {
			if (!isComplete()) {
				monitor.subTask(MessageFormat.format(
						UIText.AsynchronousRefProposalProvider_FetchingRemoteRefsMessage,
						uri));
				Collection<Change> changes;
				try {
					changes = list.get();
				} catch (InvocationTargetException | InterruptedException e) {
					throw new OperationCanceledException();
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				Change highest = findHighestPatchSet(changes,
						getChangeNumber());
				if (highest != null) {
					return highest;
				}
			}
			return this;
		}

		@Override
		public Change complete(Collection<Change> changes) {
			if (isComplete()) {
				return null;
			}
			return findHighestPatchSet(changes, getChangeNumber());
		}

		private Change findHighestPatchSet(Collection<Change> changes,
				long chgNumber) {
			if (changes == null) {
				return null;
			}
			// We know that the result is sorted by change and
			// patch set number descending
			for (Change fromGerrit : changes) {
				long num = fromGerrit.getChangeNumber();
				if (num < chgNumber) {
					return null; // Doesn't exist
				} else if (chgNumber == num) {
					// Must be the one with the highest patch
					// set number.
					return fromGerrit;
				}
			}
			return null;
		}

		@Override
		public String getBranchSuggestion() {
			Object ps = getPatchSetNumber();
			if (ps == null) {
				ps = SIMPLE_TIMESTAMP.format(new Date());
			}
			return MessageFormat.format(
					UIText.FetchGerritChangePage_SuggestedRefNamePattern,
					Long.toString(getChangeNumber()), ps);
		}

		@Override
		public String completeId() {
			if (isComplete()) {
				return Long.toString(getChangeNumber()) + '/'
						+ getPatchSetNumber();
			}
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
