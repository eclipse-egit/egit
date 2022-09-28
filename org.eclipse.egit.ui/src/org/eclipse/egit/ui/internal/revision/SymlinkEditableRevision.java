/*******************************************************************************
 * Copyright (C) 2013, 2022 Robin Stocker <robin@nibor.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Editable revision backed by a symlink (inside or outside the workspace). Used
 * for conflict resolutions with stage 2 (previous HEAD) as input; updating the
 * working tree file (which is supposed to be a symlink).
 * <p>
 * If you have a backing {@link IFile}, use {@link ResourceEditableRevision}.
 * </p>
 * <p>
 * Git always places a file in the working tree in link/file or file/link
 * conflicts. Only in link/link conflicts, we will have a symlink in the working
 * tree.
 * </p>
 */
public class SymlinkEditableRevision extends EditableRevision {

	private final IPath location;

	private final IRunnableContext runnableContext;

	/**
	 * @param fileRevision
	 * @param location
	 * @param runnableContext
	 */
	public SymlinkEditableRevision(IFileRevision fileRevision,
			@NonNull IPath location,
			@NonNull IRunnableContext runnableContext) {
		super(fileRevision, null);
		this.location = location;
		this.runnableContext = runnableContext;
	}

	@Override
	public void setContent(final byte[] newContent) {
		try {
			// Don't fork: if we are called from a thread which locked
			// workspace our *forked* operation will never complete because it
			// requires file lock which cannot be acquired from another thread
			ISchedulingRule rule = Job.getJobManager().currentRule();
			boolean fork = true;
			if (rule instanceof IResource) {
				IFile ourFile = ResourcesPlugin.getWorkspace().getRoot()
						.getFile(location);
				if (ourFile.exists()
						&& ((IResource) rule).isConflicting(ourFile))
					fork = false;
			}
			runnableContext.run(fork, false, monitor -> {
				int eol = RawParseUtils.nextLF(newContent, 0);
				String target = new String(newContent, 0, eol,
						StandardCharsets.UTF_8).trim();
				try {
					File linkFile = location.toFile();
					boolean wasBrokenLink = !linkFile.exists();
					java.nio.file.Path link = FileUtils.createSymLink(linkFile,
							target);
					updateLinkResource(wasBrokenLink, link);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				}
			});
			fireContentChanged();
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getTargetException().getMessage(),
					e.getTargetException(), true);
		} catch (InterruptedException e) {
			// ignore here
		}
	}

	private void updateLinkResource(boolean wasBroken,
			java.nio.file.Path link) {
		boolean brokenNow = !Files.exists(link);
		if (brokenNow == wasBroken) {
			// If the state doesn't change, we don't care, either Eclipse
			// doesn's see broken link and we can't do anything or it is not
			// broken and Eclipse handles the change
			return;
		}
		// refresh the parent if either the link was broken before or broken
		// just now
		IPath parentPath = location.removeLastSegments(1);
		@SuppressWarnings("null")
		final IContainer parent = ResourceUtil
				.getContainerForLocation(parentPath, true);
		if (parent != null) {
			WorkspaceJob job = new WorkspaceJob("Refreshing " + parentPath) { //$NON-NLS-1$

				@Override
				public IStatus runInWorkspace(IProgressMonitor m)
						throws CoreException {
					parent.refreshLocal(IResource.DEPTH_ONE, m);
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + Objects.hash(location, runnableContext);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !super.equals(obj) || getClass() != obj.getClass()) {
			return false;
		}
		SymlinkEditableRevision other = (SymlinkEditableRevision) obj;
		return Objects.equals(location, other.location)
				&& Objects.equals(runnableContext, other.runnableContext);
	}

}
