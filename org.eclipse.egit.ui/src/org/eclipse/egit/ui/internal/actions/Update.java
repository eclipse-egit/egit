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

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.UpdateOperation;
import org.eclipse.egit.ui.UIText;

/**
 * Action to update index for selected resources with content from workdir.
 *
 * @see UpdateOperation
 */
public class Update extends AbstractResourceOperationAction {
	protected IEGitOperation createOperation(final List<IResource> sel) {
		return sel.isEmpty() ? null : new UpdateOperation(sel);
	}

	@Override
	protected String getJobName() {
		return UIText.Update_update;
	}
}
