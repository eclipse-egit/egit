/*******************************************************************************
 * Copyright (C) 2011, Robin Rosenberg
 * Copyright (C) 2011, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.variables;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Resolves Git related information so launchers can use them
 */
public class GitVariableResolver implements IDynamicVariableResolver {

	/** Resolves to the repo-relative path of the argument. */
	public static final String GIT_REPO_RELATIVE_PATH = "git_repo_relative_path"; //$NON-NLS-1$

	/** Resolves to the .git directory of the repository of the argument. */
	public static final String GIT_DIR = "git_dir"; //$NON-NLS-1$

	/**
	 * Resolves to the working tree root directory of the repository of the
	 * argument.
	 */
	public static final String GIT_WORK_TREE = "git_work_tree"; //$NON-NLS-1$

	/**
	 * Resolves to the short name of the currently checked-out branch in the
	 * repository of the argument.
	 */
	public static final String GIT_BRANCH = "git_branch"; //$NON-NLS-1$

	@Override
	public String resolveValue(IDynamicVariable variable, String argument)
			throws CoreException {
		if (variable.getName().equals(GIT_DIR))
			return getGitDir(argument);
		if (variable.getName().equals(GIT_REPO_RELATIVE_PATH))
			return getGitRepoRelativePath(argument);
		if (variable.getName().equals(GIT_WORK_TREE))
			return getGitWorkTree(argument);
		if (variable.getName().equals(GIT_BRANCH))
			return getGitBranch(argument);
		throw new CoreException(new Status(IStatus.ERROR, Activator
				.getPluginId(), UIText.GitVariableResolver_InternalError));
	}

	private String getGitRepoRelativePath(String argument) throws CoreException {
		IResource res = getResource(argument);
		RepositoryMapping mapping = RepositoryMapping.getMapping(res);
		if (mapping != null) {
			String repoRelativePath = mapping.getRepoRelativePath(res);
			if (repoRelativePath == null) {
				return ""; //$NON-NLS-1$
			}
			if (repoRelativePath.isEmpty())
				return "."; //$NON-NLS-1$
			else
				return repoRelativePath;
		}
		return ""; //$NON-NLS-1$
	}

	private String getGitDir(String argument) throws CoreException {
		IResource res = getResource(argument);
		RepositoryMapping mapping = RepositoryMapping.getMapping(res);
		if (mapping != null)
			return mapping.getRepository().getDirectory().getAbsolutePath();
		else
			return ""; //$NON-NLS-1$
	}

	@NonNull
	private String getGitWorkTree(String argument) throws CoreException {
		IResource res = getResource(argument);
		RepositoryMapping mapping = RepositoryMapping.getMapping(res);
		if (mapping != null) {
			File workTree = mapping.getWorkTree();
			if (workTree != null) {
				return workTree.getAbsolutePath();
			}
		}
		return ""; //$NON-NLS-1$
	}

	private String getGitBranch(String argument) throws CoreException {
		IResource res = getResource(argument);
		RepositoryMapping mapping = RepositoryMapping.getMapping(res);
		if (mapping != null)
			try {
				return mapping.getRepository().getBranch();
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.getPluginId(), e.getMessage()));
			}
		else
			return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the currently selected or specified resource
	 *
	 * @param argument
	 *            named resource or null for selected
	 *
	 * @return the currently selected <code>IResource</code>.
	 * @throws CoreException
	 *             thrown if no resource is selected
	 */
	@NonNull
	private IResource getResource(String argument) throws CoreException {
		IResource res;
		if (argument == null) {
			res = getResource();
			if (res == null)
				throw new CoreException(new Status(IStatus.ERROR, Activator
						.getPluginId(), UIText.GitVariableResolver_NoSelectedResource));
		} else {
			res = ResourcesPlugin.getWorkspace().getRoot().findMember(argument);
			if (res == null || !res.exists()) {
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								Activator.getPluginId(),
								NLS.bind(
										UIText.GitVariableResolver_VariableReferencesNonExistentResource,
										argument)));
			}
		}
		return res;
	}

	private IResource getResource() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if(display.getThread().equals(Thread.currentThread()))
			return getSelectedResource();
		else {
			final IResource[] resource = new IResource[1];
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					resource[0] = getSelectedResource();
				}
			});
			return resource[0];
		}
	}

	private IResource getSelectedResource() {
		IResource resource = null;
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = null;
		if (workbench != null)
			window = workbench.getActiveWorkbenchWindow();
		if(window != null) {
			IWorkbenchPage page  = window.getActivePage();
			if(page != null) {
				IWorkbenchPart part = page.getActivePart();
				if(part instanceof IEditorPart) {
					IEditorPart epart = (IEditorPart) part;
					resource = AdapterUtils
							.adaptToAnyResource(epart.getEditorInput());
				}
				else if(part != null) {
					IWorkbenchPartSite site = part.getSite();
					if(site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if(provider != null) {
							ISelection selection = provider.getSelection();
							if(selection instanceof IStructuredSelection) {
								IStructuredSelection ss = (IStructuredSelection) selection;
								if(!ss.isEmpty()) {
									Iterator iterator = ss.iterator();
									while (iterator.hasNext() && resource == null) {
										Object next = iterator.next();
										resource = AdapterUtils
												.adaptToAnyResource(next);
									}
								}
							}
						}
					}
				}
			}
		}
		return resource;
	}

}
