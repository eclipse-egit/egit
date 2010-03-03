/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.jface.action.IAction;

/**
 * An action to add files to a Git index.
 *
 * @see AddToIndexOperation
 */
public class AddToIndexAction extends AbstractOperationAction {
	protected IWorkspaceRunnable createOperation(final IAction act,
			final List sel) {
		return sel.isEmpty() ? null : new AddToIndexOperation(sel);
	}
}
