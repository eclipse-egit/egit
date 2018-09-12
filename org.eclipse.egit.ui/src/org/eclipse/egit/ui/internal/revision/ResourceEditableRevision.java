/*******************************************************************************
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Editable revision backed by an {@link IFile}.
 * <p>
 * Use {@link LocationEditableRevision} if you just have a path (for
 * non-workspace files).
 */
public class ResourceEditableRevision extends EditableRevision {

	private final IFile file;

	private final IRunnableContext runnableContext;

	/**
	 * Create a new FileEditableRevision.
	 *
	 * @param fileRevision
	 * @param file
	 *            the file to write the changed contents to
	 * @param runnableContext
	 *            the context to use for the file write operation
	 */
	public ResourceEditableRevision(IFileRevision fileRevision, IFile file,
			IRunnableContext runnableContext) {
		super(fileRevision, CompareCoreUtils.getResourceEncoding(file));
		this.file = file;
		Assert.isNotNull(runnableContext);
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
				if (file.exists() && ((IResource) rule).isConflicting(file))
					fork = false;
			}
			runnableContext.run(fork, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor myMonitor)
						throws InvocationTargetException, InterruptedException {
					try {
						file.setContents(new ByteArrayInputStream(newContent),
								false, true, myMonitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				Activator.showErrorStatus(e.getCause().getLocalizedMessage(),
						((CoreException) e.getCause()).getStatus());
			} else {
				Activator.showError(e.getCause().getLocalizedMessage(),
						e.getCause());
			}
		} catch (InterruptedException e) {
			// ignore here
		}
	}

	/**
	 * @return the resource of this revision
	 */
	public IFile getFile() {
		return file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result
				+ ((runnableContext == null) ? 0 : runnableContext.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceEditableRevision other = (ResourceEditableRevision) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (runnableContext == null) {
			if (other.runnableContext != null)
				return false;
		} else if (!runnableContext.equals(other.runnableContext))
			return false;
		return true;
	}

}
