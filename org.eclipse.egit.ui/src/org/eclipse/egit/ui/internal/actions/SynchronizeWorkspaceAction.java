/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * An action that launch workspace synchronization.
 */
public class SynchronizeWorkspaceAction extends RepositoryAction {
	/**
	 *
	 */
	public SynchronizeWorkspaceAction() {
		super(ActionCommands.SYNC_WORKSPACE_ACTION, new SynchronizeWorkspaceActionHandler());
	}
}
