/*******************************************************************************
 * Copyright (C) 2011, 2019 Robin Stocker <robin@nibor.org> and others.
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
import java.util.Objects;

import org.eclipse.compare.IResourceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorInput;

/**
 * Editable revision backed by an {@link IFile}. Used for conflict resolutions
 * with stage 2 (previous HEAD) as input; updating the working tree file.
 * <p>
 * Use {@link LocationEditableRevision} if you just have a path (for
 * non-workspace files).
 */
public class ResourceEditableRevision extends EditableRevision
		implements IResourceProvider {

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
	public ResourceEditableRevision(IFileRevision fileRevision,
			@NonNull IFile file, @NonNull IRunnableContext runnableContext) {
		super(fileRevision, CompareCoreUtils.getResourceEncoding(file));
		this.file = file;
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
				if (file.exists() && ((IResource) rule).isConflicting(file)) {
					fork = false;
				}
			}
			runnableContext.run(fork, false, monitor -> {
				try {
					file.setContents(new ByteArrayInputStream(newContent),
							false, true, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
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

	@Override
	public IResource getResource() {
		return file;
	}

	@Override
	protected <T> T adaptEditorInput(IEditorInput editorInput,
			Class<T> adapter) {
		if (adapter == IResource.class || adapter == IFile.class) {
			return adapter.cast(file);
		}
		return super.adaptEditorInput(editorInput, adapter);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + Objects.hash(file, runnableContext);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !super.equals(obj) || getClass() != obj.getClass()) {
			return false;
		}
		ResourceEditableRevision other = (ResourceEditableRevision) obj;
		return Objects.equals(file, other.file)
				&& Objects.equals(runnableContext, other.runnableContext);
	}

}
