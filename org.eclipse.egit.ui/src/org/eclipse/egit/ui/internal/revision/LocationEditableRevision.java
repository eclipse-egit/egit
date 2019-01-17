/*******************************************************************************
 * Copyright (C) 2013, 2019 Robin Stocker <robin@nibor.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Editable revision backed by a file outside of the workspace (just IPath).
 * Used for conflict resolutions with stage 2 (previous HEAD) as input; updating
 * the working tree file.
 * <p>
 * If you have a backing {@link IFile}, use {@link ResourceEditableRevision}.
 */
public class LocationEditableRevision extends EditableRevision {

	private final IPath location;

	private final IRunnableContext runnableContext;

	/**
	 * @param fileRevision
	 * @param location
	 * @param runnableContext
	 */
	public LocationEditableRevision(IFileRevision fileRevision,
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
				IFileStore store = EFS.getLocalFileSystem().getStore(location);
				try (BufferedOutputStream out = new BufferedOutputStream(
						store.openOutputStream(EFS.NONE, monitor))) {
					out.write(newContent);
				} catch (CoreException | IOException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getTargetException().getMessage(),
					e.getTargetException(), true);
		} catch (InterruptedException e) {
			// ignore here
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
		LocationEditableRevision other = (LocationEditableRevision) obj;
		return Objects.equals(location, other.location)
				&& Objects.equals(runnableContext, other.runnableContext);
	}

}
