/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.team.core.Team;

/**
 * Action for ignoring files via .gitignore
 *
 */
public class IgnoreAction extends RepositoryAction {
	
	@SuppressWarnings("restriction")
	@Override
	public void run(IAction action) {
		final IResource[] resources = getSelectedResources();
		if (resources.length == 0)
			return;
		
		WorkspaceJob job = new WorkspaceJob("Ignore Git resources") { 
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("Ignoring Git resources", resources.length);
				try {
					for (IResource resource : resources) {
						// TODO This is pretty inefficient; multiple ignores in the same directory cause multiple writes.
						// NB This does the same thing in DecoratableResourceAdapter, but neither currently consult .gitignore
						if (!Team.isIgnoredHint(resource)) {
							IContainer container = resource.getParent();
							IFile gitignore = container.getFile(new Path(Constants.GITIGNORE_FILENAME));
							String entry = "/" + resource.getName() + "\n"; //$NON-NLS-1$  //$NON-NLS-2$
							// TODO What is the character set and new-line convention?
							if(gitignore.exists()) {
								// This is ugly. CVS uses an internal representation of the .gitignore to re-write/overwrite each time.
								ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
								out.write(entry.getBytes(Constants.CHARACTER_ENCODING)); // TODO Default encoding?
								gitignore.appendContents(new ByteArrayInputStream(out.toByteArray()),true,true,monitor);
							} else {
								ByteArrayInputStream bais = new ByteArrayInputStream( entry.getBytes(Constants.CHARACTER_ENCODING) ); 
								gitignore.create( bais,true,monitor);					
							}
						}
						monitor.worked(1);
					}
					monitor.done();
				} catch (CoreException e) {
					throw e;
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.egit.ui", "Unable to ignore resources", e)); //$NON-NLS-1$
				}
				return Status.OK_STATUS;			
			}
		};
		job.schedule();		
	}

	@SuppressWarnings("restriction")
	@Override
	public boolean isEnabled() {
		if (getProjectsInRepositoryOfSelectedResources().length == 0)
			return false;

		IResource[] resources = getSelectedResources();
		for (IResource resource : resources) {
			// NB This does the same thing in DecoratableResourceAdapter, but neither currently consult .gitignore
			if (!Team.isIgnoredHint(resource))
				return true;
		}
		return false;
	}
}
