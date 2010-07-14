/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.team.core.Team;

/** Action for ignoring files via .gitignore. */
public class IgnoreAction extends RepositoryAction {
	@SuppressWarnings("restriction")
	@Override
	public void execute(IAction action) {
		final IResource[] resources = getSelectedResources();
		if (resources.length == 0)
			return;

		WorkspaceJob job = new WorkspaceJob(UIText.IgnoreAction_jobName) {
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				monitor.beginTask(UIText.IgnoreAction_taskName, resources.length);
				try {
					for (IResource resource : resources) {
						// TODO This is pretty inefficient; multiple ignores in
						// the same directory cause multiple writes.

						// NB This does the same thing in
						// DecoratableResourceAdapter, but neither currently
						// consult .gitignore
						if (!RepositoryMapping.isIgnored(resource) && !Team.isIgnoredHint(resource)) {
							//The file is neither ignored by Team nor by the ignore cache
							addIgnore(monitor, resource);
						}

						monitor.worked(1);
					}
					monitor.done();
				} catch (CoreException e) {
					throw e;
				} catch (Exception e) {
					throw new CoreException(
							new Status(
									IStatus.ERROR,
									"org.eclipse.egit.ui", UIText.IgnoreAction_error, e)); //$NON-NLS-1$
				}
				return Status.OK_STATUS;
			}

			private void addIgnore(IProgressMonitor monitor, IResource resource)
					throws UnsupportedEncodingException, CoreException {
				IContainer container = resource.getParent();
				IFile gitignore = container.getFile(new Path(
						Constants.GITIGNORE_FILENAME));
				String entry = "/" + resource.getName() + "\n"; //$NON-NLS-1$  //$NON-NLS-2$
				ByteArrayInputStream entryBytes = asStream(entry);

				if (gitignore.exists())
					gitignore.appendContents(entryBytes, true, true, monitor);
				else
					gitignore.create(entryBytes, true, monitor);
			}

			private ByteArrayInputStream asStream(String entry)
					throws UnsupportedEncodingException {
				return new ByteArrayInputStream(entry
						.getBytes(Constants.CHARACTER_ENCODING));
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
			// NB This does the same thing in DecoratableResourceAdapter, but
			// neither currently consult .gitignore
			if (!Team.isIgnoredHint(resource))
				return true;
		}
		return false;
	}
}
