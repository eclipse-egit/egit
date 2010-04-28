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

import org.eclipse.egit.core.op.AssumeUnchangedOperation;
import org.eclipse.egit.core.op.IEGitOperation;

/**
 * This operation sets the assume-valid bit in the index for the
 * selected resources.
 *
 * @see AssumeUnchangedOperation
 */
public class AssumeUnchanged extends AbstractOperationAction {
	protected IEGitOperation createOperation(final List sel) {
		return sel.isEmpty() ? null : new AssumeUnchangedOperation(sel);
	}
}
