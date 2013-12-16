/*******************************************************************************
 * Copyright (c) 2011, 2013 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Muskalla <bmuskalla@tasktop.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.ProjectRecord;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Common label provider for git related model objects
 *
 */
public class GitLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private ResourceManager imageCache;

	private LabelProvider workbenchLabelProvider;

	/**
	 * Format the branch tracking status suitable for displaying in decorations and labels.
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

	@Override
	public String getText(Object element) {
		if (element instanceof Repository)
			return getSimpleTextFor((Repository) element);

		if (element instanceof RefNode)
			return getSimpleTextFor((RefNode) element);

		if (element instanceof Ref)
			return ((Ref) element).getName();

		if (element instanceof ProjectRecord)
			return ((ProjectRecord) element).getProjectLabel();

		if (element instanceof GitModelObject)
			return ((GitModelObject) element).getName();

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Repository)
			return RepositoryTreeNodeType.REPO.getIcon();

		if (element instanceof RefNode)
			return getRefIcon(((RefNode) element).getObject());

		if (element instanceof Ref)
			return getRefIcon((Ref) element);

		if (element instanceof GitModelBlob || element instanceof GitModelTree) {
			Object adapter = ((IAdaptable) element).getAdapter(IResource.class);
			return getWorkbenchLabelProvider().getImage(adapter);
		}

		if (element instanceof GitModelCommit
				|| element instanceof GitModelCache
				|| element instanceof GitModelWorkingTree
				|| element instanceof RepositoryCommit)
			return getChangesetIcon();

		if (element instanceof GitModelRepository)
			return getImage(((GitModelRepository) element).getRepository());

		if (element instanceof ProjectRecord)
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(SharedImages.IMG_OBJ_PROJECT);

		return super.getImage(element);
	}

	public StyledString getStyledText(Object element) {
		try {
			if (element instanceof Repository)
				return getStyledTextFor((Repository) element);

			if (element instanceof GitModelRepository)
				return getStyledTextFor(((GitModelRepository) element)
						.getRepository());

		} catch (IOException e) {
			Activator.logError(NLS.bind(
					UIText.GitLabelProvider_UnableToRetrieveLabel,
					element.toString()), e);
		}
		return new StyledString(getText(element));
	}

	/**
	 * @param repository
	 * @return a styled string for the repository
	 * @throws IOException
	 */
	public static StyledString getStyledTextFor(Repository repository)
			throws IOException {
		File directory = repository.getDirectory();

		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();

		StyledString string = new StyledString();
		string.append(repositoryUtil.getRepositoryName(repository));

		String branch = repositoryUtil
				.getShortBranch(repository);
		if (branch != null) {
			string.append(' ');
			string.append('[', StyledString.DECORATIONS_STYLER);
			string.append(branch, StyledString.DECORATIONS_STYLER);

			RepositoryState repositoryState = repository.getRepositoryState();
			if (repositoryState != RepositoryState.SAFE) {
				string.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
				string.append(repositoryState.getDescription(),
						StyledString.DECORATIONS_STYLER);
			}

			BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, branch);
			if (trackingStatus != null
					&& (trackingStatus.getAheadCount() != 0 || trackingStatus
							.getBehindCount() != 0)) {
				String formattedTrackingStatus = formatBranchTrackingStatus(trackingStatus);
				string.append(' ');
				string.append(formattedTrackingStatus, StyledString.DECORATIONS_STYLER);
			}
			string.append(']', StyledString.DECORATIONS_STYLER);
		}

		string.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		string.append(directory.getAbsolutePath(), StyledString.QUALIFIER_STYLER);

		return string;
	}

	/**
	 * Returns the common icon for a changeset.
	 *
	 * @return an image
	 */
	protected Image getChangesetIcon() {
		return getImageCache().createImage(UIIcons.CHANGESET);
	}

	private Image getRefIcon(Ref ref) {
		String name = ref.getName();
		if (name.startsWith(Constants.R_HEADS)
				|| name.startsWith(Constants.R_REMOTES))
			return RepositoryTreeNodeType.REF.getIcon();
		else if (name.startsWith(Constants.R_TAGS))
			return RepositoryTreeNodeType.TAG.getIcon();
		else
			return RepositoryTreeNodeType.ADDITIONALREF.getIcon();
	}

	private LabelProvider getWorkbenchLabelProvider() {
		if (workbenchLabelProvider == null)
			workbenchLabelProvider = new WorkbenchLabelProvider();
		return workbenchLabelProvider;
	}

	private ResourceManager getImageCache() {
		if (imageCache == null)
			imageCache = new LocalResourceManager(JFaceResources.getResources());
		return imageCache;
	}

	private String getSimpleTextFor(RefNode refNode) {
		return refNode.getObject().getName();
	}

	/**
	 * @param repository
	 * @return simple text for repository
	 */
	public static String getSimpleTextFor(Repository repository) {
		String name = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		File directory = repository.getDirectory();
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(" - "); //$NON-NLS-1$
		sb.append(directory.getAbsolutePath());
		return sb.toString();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (imageCache != null)
			imageCache.dispose();
		if(workbenchLabelProvider != null)
			workbenchLabelProvider.dispose();
	}
}
