/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <caniszczyk@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Resolves the ${project_git_branch} variable to the current branch
 */
public class GitBranchVariableResolver implements IDynamicVariableResolver {

	/**
	 * Resolves the project_git_branch variable
	 */
	public GitBranchVariableResolver() {
		// nothing
	}

	public String resolveValue(IDynamicVariable variable, String argument)
			throws CoreException {
		IResource resource = null;
		if (argument == null) {
			resource = getResource();
		} else {
			resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(argument));
		}
		if (resource != null) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			try {
				if (mapping != null)
					return mapping.getRepository().getBranch();
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Returns the currently selected resource
	 *
	 * @return the currently selected <code>IResource</code>, or <code>null</code> if none.
	 */
	protected IResource getResource() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if(display.getThread().equals(Thread.currentThread()))
			return getSelectedResource();
		else {
			final IResource[] resource = new IResource[1];
			display.syncExec(new Runnable() {
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
					resource = (IResource) epart.getEditorInput().getAdapter(IResource.class);
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
										resource = (IResource) Platform.getAdapterManager().getAdapter(next, IResource.class);
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
