/*******************************************************************************
 * Copyright (C) 2007, David Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Compare the resources filtered in the history view with the current revision.
 */
public class CompareWithRevisionAction extends RepositoryAction {
	/**
	 *
	 */
	public CompareWithRevisionAction() {
		super(ActionCommands.COMPARE_WITH_REVISION_ACTION);
	}
}
