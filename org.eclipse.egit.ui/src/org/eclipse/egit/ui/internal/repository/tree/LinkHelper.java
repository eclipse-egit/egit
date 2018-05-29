/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;

/**
 * Link Helper for Git Repositories View
 */
public class LinkHelper implements ILinkHelper {
	@Override
	public void activateEditor(IWorkbenchPage aPage,
			IStructuredSelection aSelection) {

		try {
			FileNode node = (FileNode) aSelection.getFirstElement();

			File file = node.getObject();

			for (IEditorReference ref : aPage.getEditorReferences()) {
				IEditorPart part = ref.getEditor(false);
				if (part != null) {
					IEditorInput input = part.getEditorInput();
					if (input instanceof IFileEditorInput) {
						IFile r = ((IFileEditorInput) input).getFile();
						if (r.getLocation().toFile().equals(file)) {
							aPage.activate(part);
							return;
						}
					}
					if (input instanceof IURIEditorInput) {
						if (((IURIEditorInput) input).getURI().equals(
								file.toURI())) {
							aPage.activate(part);
							return;
						}
					}
				}
			}
		} catch (Exception e) {
			// simply ignore here
		}
	}

	/**
	 * TODO javadoc missing
	 */
	@Override
	@SuppressWarnings("unchecked")
	public IStructuredSelection findSelection(IEditorInput anInput) {
		if (!(anInput instanceof IURIEditorInput)) {
			return null;
		}

		URI uri = ((IURIEditorInput) anInput).getURI();

		if (!uri.getScheme().equals("file")) //$NON-NLS-1$
			return null;

		File file = new File(uri.getPath());

		if (!file.exists())
			return null;

		RepositoryUtil config = Activator.getDefault().getRepositoryUtil();

		List<String> repos = config.getConfiguredRepositories();
		for (String repo : repos) {
			Repository repository;
			try {
				repository = FileRepositoryBuilder.create(new File(repo));
			} catch (IOException e) {
				continue;
			}
			if (repository.isBare())
				continue;
			if (file.getPath().startsWith(repository.getWorkTree().getPath())) {
				RepositoriesViewContentProvider cp = new RepositoriesViewContentProvider();

				RepositoryNode repoNode = new RepositoryNode(null, repository);
				RepositoryTreeNode result = null;

				for (Object child : cp.getChildren(repoNode)) {
					if (child instanceof WorkingDirNode) {
						result = (WorkingDirNode) child;
						break;
					}
				}

				if (result == null)
					return null;

				IPath remainingPath = new Path(file.getPath().substring(
						repository.getWorkTree().getPath().length()));
				for (String segment : remainingPath.segments()) {
					for (Object child : cp.getChildren(result)) {
						RepositoryTreeNode<File> fileNode;
						try {
							fileNode = (RepositoryTreeNode<File>) child;
						} catch (ClassCastException e) {
							return null;
						}
						if (fileNode.getObject().getName().equals(segment)) {
							result = fileNode;
							break;
						}
					}
				}

				return new StructuredSelection(result);
			}
		}
		return null;
	}
}
