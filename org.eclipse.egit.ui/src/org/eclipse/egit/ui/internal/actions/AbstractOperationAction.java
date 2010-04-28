/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Common functionality for EGit operations.
 */
public abstract class AbstractOperationAction implements IObjectActionDelegate {
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
	protected abstract IEGitOperation createOperation(final List selection);

	/**
	 * A method to invoke when the operation is finished.
	 */
	protected void postOperation() {
		// Empty
	}

	public void run(final IAction act) {
		op = createOperation(selection);
		if (op != null) {
			try {
				try {
					wp.getSite().getWorkbenchWindow().run(true, false,
							new IRunnableWithProgress() {
								public void run(final IProgressMonitor monitor)
										throws InvocationTargetException {
									try {
										op.execute(monitor);
									} catch (CoreException ce) {
										throw new InvocationTargetException(ce);
									}
								}
							});
				} finally {
					postOperation();
				}
			} catch (Throwable e) {
				final String msg = NLS.bind(UIText.GenericOperationFailed, act
						.getText());
				final IStatus status;

				if (e instanceof InvocationTargetException) {
					e = e.getCause();
				}

				if (e instanceof CoreException) {
					status = ((CoreException) e).getStatus();
				} else {
					status = new Status(IStatus.ERROR, Activator.getPluginId(),
							1, msg, e);
				}

				Activator.logError(msg, e);
				ErrorDialog.openError(wp.getSite().getShell(), act.getText(),
						msg, status, status.getSeverity());
			}
		}
	}
}
