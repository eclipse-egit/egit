/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, IBM Corporation
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.factories;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.history.FileDiffWorkbenchAdapter;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.GitHistoryPageSource;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeWorkbenchAdapter;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitModelWorkbenchAdapter;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitObjectMapping;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.part.IShowInSource;

/**
 * This class is an intelligent "cast" operation for getting
 * an instance of a suitable object from another for a specific
 * purpose.
 */
public class GitAdapterFactory implements IAdapterFactory {

	private GitModelWorkbenchAdapter gitModelWorkbenchAdapter;

	private static final IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType.isAssignableFrom(IHistoryPageSource.class)) {
			return adapterType.cast(GitHistoryPageSource.INSTANCE);
		}

		if (IWorkbenchAdapter.class == adapterType) {
			if (adaptableObject instanceof RepositoryTreeNode) {
				return adapterType
						.cast(RepositoryTreeNodeWorkbenchAdapter.INSTANCE);
			} else if (adaptableObject instanceof FileDiff) {
				return adapterType.cast(FileDiffWorkbenchAdapter.INSTANCE);
			}

			if (gitModelWorkbenchAdapter == null) {
				gitModelWorkbenchAdapter = new GitModelWorkbenchAdapter();
			}
			return adapterType.cast(gitModelWorkbenchAdapter);
		}

		if (adaptableObject instanceof IHistoryView
				&& IShowInSource.class == adapterType) {
			IHistoryView historyView = (IHistoryView) adaptableObject;
			IHistoryPage historyPage = historyView.getHistoryPage();
			if (historyPage instanceof GitHistoryPage) {
				return adapterType.cast(historyPage);
			}
		}

		if (adaptableObject instanceof IURIEditorInput
				&& adapterType == Repository.class) {
			return adapterType
					.cast(getRepository((IURIEditorInput) adaptableObject));
		}

		if (adaptableObject instanceof IURIEditorInput
				&& adapterType == File.class) {
			return adapterType.cast(getFile((IURIEditorInput) adaptableObject));
		}

		if (adaptableObject instanceof GitModelObject
				&& adapterType == ResourceMapping.class) {
			return adapterType.cast(
					GitObjectMapping.create((GitModelObject) adaptableObject));
		}

		if (adaptableObject instanceof GitModelObject
				&& adapterType == IResource.class) {
			GitModelObject obj = (GitModelObject) adaptableObject;

			if (obj instanceof GitModelBlob) {
				IResource res = ResourceUtil
						.getFileForLocation(obj.getLocation(), false);
				if (res == null) {
					// Deleted resource?
					res = getWorkspaceResourceFromGitPath(obj.getLocation());
				}

				return adapterType.cast(res);
			}

			if (obj instanceof GitModelTree) {
				IResource res = root.getContainerForLocation(obj.getLocation());
				if (res == null) {
					res = root.getFolder(obj.getLocation());
				}

				return adapterType.cast(res);
			}
		}

		if (adapterType == Repository.class) {
			ResourceMapping m = AdapterUtils.adapt(adaptableObject,
					ResourceMapping.class);
			if (m != null) {
				return adapterType.cast(SelectionUtils
						.getRepository(new StructuredSelection(m)));
			}
		}

		return null;
	}

	@Nullable
	private IResource getWorkspaceResourceFromGitPath(IPath gitPath) {
		Repository repository = Activator.getDefault().getRepositoryCache()
				.getRepository(gitPath);
		if (repository == null || repository.isBare()) {
			return null;
		}
		IPath repoRelativePath = gitPath.makeRelativeTo(
				new Path(repository.getWorkTree().getAbsolutePath()));
		IProject[] projects = ProjectUtil.getProjectsContaining(repository,
				Collections.singleton(repoRelativePath.toString()));
		if (projects.length > 0) {
			IPath projectRelativePath = gitPath
					.makeRelativeTo(projects[0].getLocation());
			if (projectRelativePath.isEmpty()) {
				return projects[0];
			} else {
				return projects[0].getFile(projectRelativePath);
			}
		}
		return root.getFile(gitPath);
	}

	@Nullable
	private static Repository getRepository(IURIEditorInput uriInput) {
		File file = getFile(uriInput);
		if (file == null) {
			return null;
		}
		Path path = new Path(file.getAbsolutePath());
		RepositoryMapping mapping = RepositoryMapping.getMapping(path);
		if (mapping != null) {
			return mapping.getRepository();
		}
		Repository repository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().getRepository(path);
		return repository;
	}

	@Nullable
	private static File getFile(IURIEditorInput uriInput) {
		URI uri = uriInput.getURI();
		if (uri == null) {
			return null;
		}
		try {
			IFileStore store = EFS.getStore(uri);
			if (store != null) {
				return store.toLocalFile(EFS.NONE, new NullProgressMonitor());
			}
		} catch (CoreException ce) {
			// ignore
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IHistoryPageSource.class,
				ISynchronizationCompareAdapter.class, ResourceMapping.class,
				IResource.class, IWorkbenchAdapter.class, IShowInSource.class,
				Repository.class, File.class, IHistoryPageSource.class};
	}
}
