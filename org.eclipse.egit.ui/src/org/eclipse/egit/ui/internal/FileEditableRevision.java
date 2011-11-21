/*******************************************************************************
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Editable revision backed by an {@link IFile}.
 */
public class FileEditableRevision extends EditableRevision {

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
	public FileEditableRevision(IFileRevision fileRevision, IFile file,
			IRunnableContext runnableContext) {
		super(fileRevision);
		this.file = file;
		Assert.isNotNull(runnableContext);
		this.runnableContext = runnableContext;
	}

	@Override
	public void setContent(final byte[] newContent) {
		try {
			runnableContext.run(false, false, new IRunnableWithProgress() {
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
			Activator.handleError(e.getTargetException().getMessage(),
					e.getTargetException(), true);
		} catch (InterruptedException e) {
			// ignore here
		}
	}
}
