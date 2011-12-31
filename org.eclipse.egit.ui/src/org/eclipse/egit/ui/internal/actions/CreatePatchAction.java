/*******************************************************************************
 * Copyright (C) 2011, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Action to generate a patch file using the diff command.
 */
public class CreatePatchAction extends RepositoryAction {

	/**
	 *
	 */
	public CreatePatchAction() {
		super(ActionCommands.CREATE_PATCH, new CreatePatchActionHandler());
	}
}