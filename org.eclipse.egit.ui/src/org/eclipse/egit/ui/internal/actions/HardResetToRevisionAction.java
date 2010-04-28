/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.jgit.lib.AnyObjectId;

/**
 * Hard reset to selected revision
 */
public class HardResetToRevisionAction extends AbstractRevObjectAction {

	@Override
	protected IEGitOperation createOperation(List selection) {
		return new ResetOperation(getActiveRepository(),
				((AnyObjectId) selection.get(0)).name(),
				ResetOperation.ResetType.HARD);
	}
}
