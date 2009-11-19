/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.egit.core.op.UpdateOperation;

/**
 * Action to update index for selected resources with content from workdir.
 *
 * @see UpdateOperation
 */
public class Update extends AbstractOperationAction {
	protected IWorkspaceRunnable createOperation(final List sel) {
		return sel.isEmpty() ? null : new UpdateOperation(sel);
	}
}
