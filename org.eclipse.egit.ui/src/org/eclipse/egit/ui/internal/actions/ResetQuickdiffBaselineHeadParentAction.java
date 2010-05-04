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

/**
 * Changes the reference for the quickdiff to the (first) parent of HEAD
 */
public class ResetQuickdiffBaselineHeadParentAction extends AbstractRevObjectAction {

	@Override
	protected IEGitOperation createOperation(List selection) {
		return new QuickdiffBaselineOperation(getActiveRepository(), "HEAD^1"); //$NON-NLS-1$
	}
}
