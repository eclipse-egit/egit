/*******************************************************************************
 * Copyright (c) 2011, 2014 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Benjamin Muskalla <bmuskalla@tasktop.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.IOException;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.ProjectRecord;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;

/**
 * Various methods to compute label for different Git repository elements.
 */
public class GitLabels {
	private GitLabels() {
	}

	/**
	 * @param ref
	 * @return a description of the ref, or null if the ref does not have a
	 *         description
	 */
	public static String getRefDescription(Ref ref) {
		String name = ref.getName();
		if (name.equals(Constants.HEAD)) {
			if (ref.isSymbolic())
				return UIText.GitLabelProvider_RefDescriptionHeadSymbolic;
			else
				return UIText.GitLabelProvider_RefDescriptionHead;
		} else if (name.equals(Constants.ORIG_HEAD))
			return UIText.GitLabelProvider_RefDescriptionOrigHead;
		else if (name.equals(Constants.FETCH_HEAD))
			return UIText.GitLabelProvider_RefDescriptionFetchHead;
		else if (name.equals(Constants.R_STASH))
			return UIText.GitLabelProvider_RefDescriptionStash;
		else
			return null;
	}

	/**
	 * Format the branch tracking status suitable for displaying in decorations
	 * and labels.
	 *
	 * @param status
	 * @return the branch tracking status as a string
	 */
	public static String formatBranchTrackingStatus(BranchTrackingStatus status) {
		StringBuilder sb = new StringBuilder();
		int ahead = status.getAheadCount();
		int behind = status.getBehindCount();
		if (ahead != 0) {
			// UPWARDS ARROW
			sb.append('\u2191');
			sb.append(ahead);
		}
		if (behind != 0) {
			if (sb.length() != 0)
				sb.append(' ');
			// DOWNWARDS ARROW
			sb.append('\u2193');
			sb.append(status.getBehindCount());
		}
		return sb.toString();
	}

	/**
	 * Returns a {@link StyledString} that is initialized with "> " if the
	 * repository has any changes, empty otherwise.
	 *
	 * @param repository
	 *            to get the string for
	 * @return the {@link StyledString}
	 */
	public static @NonNull StyledString getChangedPrefix(
			@NonNull Repository repository) {
		StyledString string = new StyledString();
		if (RepositoryUtil.hasChanges(repository)) {
			string.append('>', StyledString.DECORATIONS_STYLER);
			string.append(' ');
		}
		return string;
	}

	/**
	 * Computes detailed repository label that consists of repository name,
	 * state, checked-out branch and it's status (returned by
	 * {@linkplain #formatBranchTrackingStatus(BranchTrackingStatus)})
	 *
	 * @param repository
	 * @return a styled string for the repository
	 * @throws IOException
	 */
	public static @NonNull StyledString getStyledLabel(
			@NonNull Repository repository)
			throws IOException {
		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();

		StyledString string = getChangedPrefix(repository);

		string.append(repositoryUtil.getRepositoryName(repository));

		String branch = repositoryUtil.getShortBranch(repository);
		if (branch != null) {
			string.append(' ');
			string.append('[', StyledString.DECORATIONS_STYLER);
			string.append(branch, StyledString.DECORATIONS_STYLER);

			BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(
					repository, branch);
			if (trackingStatus != null
					&& (trackingStatus.getAheadCount() != 0 || trackingStatus
							.getBehindCount() != 0)) {
				String formattedTrackingStatus = GitLabels
						.formatBranchTrackingStatus(trackingStatus);
				string.append(' ');
				string.append(formattedTrackingStatus,
						StyledString.DECORATIONS_STYLER);
			}

			RepositoryState repositoryState = repository.getRepositoryState();
			if (repositoryState != RepositoryState.SAFE) {
				string.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
				string.append(repositoryState.getDescription(),
						StyledString.DECORATIONS_STYLER);
			}
			string.append(']', StyledString.DECORATIONS_STYLER);
		}

		return string;
	}

	/**
	 * Tries to return label produced by
	 * {@linkplain #getStyledLabel(Repository)}. If
	 * {@linkplain #getStyledLabel(Repository)} throws <code>Exception</code>
	 * then logs it and falls back to {@linkplain #getPlainShortLabel(Object)}
	 * result.
	 *
	 * @param repository
	 * @return repository label
	 */
	public static @NonNull StyledString getStyledLabelSafe(
			@NonNull Repository repository) {
		try {
			return getStyledLabel(repository);
		} catch (IOException e) {
			logLabelRetrievalFailure(repository, e);
		}
		return new StyledString(getPlainShortLabel(repository));
	}

	/**
	 * <p>
	 * If the <code>element</code> represents a repository then tries to return
	 * it's styled label (created by {@link #getStyledLabel(Repository)}). If
	 * the <code>element</code> is not a repository or if
	 * {@link #getStyledLabel(Repository)} threw <code>IOException</code> then
	 * this method returns element's simple label (
	 * {@link #getPlainShortLabel(Object)}). If the element is a repository then
	 * the returned label is appended with absolute path to this repository.
	 * </p>
	 * <p>
	 * IOException thrown by {@link #getStyledLabel(Repository)} is logged.
	 * </p>
	 *
	 * @param element
	 * @return element's label
	 */
	public static @NonNull StyledString getStyledLabelExtendedSafe(
			Object element) {
		Repository repo = asRepository(element);

		if (repo != null) {
			try {
				StyledString text = getStyledLabel(repo);
				text.append(getLabelExtension(repo),
						StyledString.QUALIFIER_STYLER);
				return text;
			} catch (IOException e) {
				logLabelRetrievalFailure(element, e);
			}
		}
		return new StyledString(getPlainShortLabelExtended(element));
	}

	/**
	 * @param element
	 * @return label computed by {@linkplain #getPlainShortLabel(Object)} with
	 *         appended path to the repository if element represents a
	 *         repository
	 */
	public static String getPlainShortLabelExtended(Object element) {
		return getPlainShortLabel(element) + getLabelExtension(element);
	}

	private static void logLabelRetrievalFailure(Object element, IOException e) {
		Activator.logError(
				NLS.bind(UIText.GitLabelProvider_UnableToRetrieveLabel,
						element.toString()), e);
	}

	private static String getLabelExtension(Object element) {
		Repository repo = asRepository(element);

		if (repo != null)
			return " - " + getRepositoryAbsolutePath(repo); //$NON-NLS-1$
		else
			return ""; //$NON-NLS-1$
	}

	/**
	 * @param element
	 * @return simple short label of the Git element (repository, reference,
	 *         changeset, etc.).
	 */
	public static String getPlainShortLabel(Object element) {
		if (element instanceof Repository)
			return getRepositorySimpleLabel((Repository) element);

		if (element instanceof RefNode)
			return getRefNodeSimpleLabel((RefNode) element);

		if (element instanceof Ref)
			return ((Ref) element).getName();

		if (element instanceof ProjectRecord)
			return ((ProjectRecord) element).getProjectLabel();

		if (element instanceof GitModelObject)
			return ((GitModelObject) element).getName();

		return (element != null ? element.toString() : ""); //$NON-NLS-1$
	}

	private static String getRefNodeSimpleLabel(RefNode refNode) {
		return refNode.getObject().getName();
	}

	private static String getRepositorySimpleLabel(Repository repository) {
		return Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
	}

	private static String getRepositoryAbsolutePath(Repository repository) {
		return repository.getDirectory().getAbsolutePath();
	}

	private static Repository asRepository(Object element) {
		if (element instanceof Repository)
			return (Repository) element;
		else if (element instanceof GitModelRepository)
			return ((GitModelRepository) element).getRepository();
		else
			return null;
	}
}
