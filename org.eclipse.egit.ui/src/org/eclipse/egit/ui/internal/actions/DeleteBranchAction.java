/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Action for deleting a branch
 */
public class DeleteBranchAction extends RepositoryAction {
	/**
	 * Constructs this action
	 */
	public DeleteBranchAction() {
		super(ActionCommands.DELETE_BRANCH_ACTION,
				new DeleteBranchActionHandler());
	}
}
