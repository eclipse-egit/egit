/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
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
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
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

	private final ResourceManager imageCache = new LocalResourceManager(
			JFaceResources.getResources());

	private LabelProvider workbenchLabelProvider;

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

		if (element instanceof RefNode || element instanceof Ref)
			return RepositoryTreeNodeType.REF.getIcon();

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
	protected StyledString getStyledTextFor(Repository repository)
			throws IOException {
		File directory = repository.getDirectory();
		StyledString string = new StyledString();
		if (!repository.isBare())
			string.append(directory.getParentFile().getName());
		else
			string.append(directory.getName());
		string.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		string.append(directory.getAbsolutePath(), StyledString.QUALIFIER_STYLER);

		String branch = repository.getBranch();
		string.append(" "); //$NON-NLS-1$
		string.append("[", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		string.append(branch, StyledString.DECORATIONS_STYLER);

		RepositoryState repositoryState = repository.getRepositoryState();
		if (repositoryState != RepositoryState.SAFE) {
			string.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			string.append(repositoryState.getDescription(), StyledString.DECORATIONS_STYLER);
		}

		string.append("]", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		return string;
	}

	/**
	 * Returns the common icon for a changeset.
	 *
	 * @return an image
	 */
	protected Image getChangesetIcon() {
		return imageCache.createImage(UIIcons.CHANGESET);
	}

	private LabelProvider getWorkbenchLabelProvider() {
		if (workbenchLabelProvider == null)
			workbenchLabelProvider = new WorkbenchLabelProvider();
		return workbenchLabelProvider;
	}

	private String getSimpleTextFor(RefNode refNode) {
		return refNode.getObject().getName();
	}

	private String getSimpleTextFor(Repository repository) {
		File directory;
		if (!repository.isBare())
			directory = repository.getDirectory().getParentFile();
		else
			directory = repository.getDirectory();
		StringBuilder sb = new StringBuilder();
		sb.append(directory.getName());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(directory.getAbsolutePath());
		return sb.toString();
	}

	@Override
	public void dispose() {
		super.dispose();
		imageCache.dispose();
		if(workbenchLabelProvider != null)
			workbenchLabelProvider.dispose();
	}
}
