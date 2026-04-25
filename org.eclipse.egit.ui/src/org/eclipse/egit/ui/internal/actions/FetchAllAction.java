/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
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
 * Action for "Fetch all" — fetches from all configured remotes.
 */
public class FetchAllAction extends RepositoryAction {

	/**
	 *
	 */
	public FetchAllAction() {
		super(ActionCommands.FETCH_ALL_ACTION, new FetchAllActionHandler());
	}
}
