/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;

/**
 *	Action to disassociate a project from its Git repository.
 *
 *  @see DisconnectProviderOperation
 */
public class Disconnect extends AbstractOperationAction {
	protected IEGitOperation createOperation(final List sel) {
		return sel.isEmpty() ? null : new DisconnectProviderOperation(sel);
	}

	protected void postOperation() {
		GitLightweightDecorator.refresh();
	}
}
