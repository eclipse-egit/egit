/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Common functionality resource based EGit operations.
 *
 */
public abstract class AbstractResourceOperationAction implements IObjectActionDelegate {
	/**
	 * The active workbench part
	 */
	protected IWorkbenchPart wp;

	private IEGitOperation op;

	private List selection;

	public void selectionChanged(final IAction act, final ISelection sel) {
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			selection = ((IStructuredSelection) sel).toList();
		} else {
			selection = Collections.EMPTY_LIST;
		}
	}

	public void setActivePart(final IAction act, final IWorkbenchPart part) {
		wp = part;
	}

	/**
	 * Instantiate an operation on an action on provided objects.
	 * @param selection
	 *
	 * @return a {@link IEGitOperation} for invoking this operation later on
	 */
	protected abstract IEGitOperation createOperation(final List<IResource> selection);

	/**
	 * @return the name of the execution Job
	 */
	protected abstract String getJobName();

	/**
	 * A method to invoke when the operation is finished.
	 * The method is called outside the UI thread.
	 */
	protected void postOperation() {
		// Empty
	}

	public void run(final IAction act) {
		op = createOperation(getSelectedResources());
		if(op==null)
			return;
		String jobname = getJobName();
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					postOperation();
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
	}

	private List<IResource> getSelectedResources() {
		List<IResource> resources = new ArrayList<IResource>();
		for(Object object: selection) {
			if(object instanceof IResource)
				resources.add((IResource) object);
		}
		return resources;
	}
}
