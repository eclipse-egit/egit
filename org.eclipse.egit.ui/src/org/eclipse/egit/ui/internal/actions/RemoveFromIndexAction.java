/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.eclipse.egit.ui.internal.actions.ActionCommands.REMOVE_FROM_INDEX;

import org.eclipse.egit.core.op.RemoveFromIndexOperation;


/**
 * An action to remove files from Git index.
 *
 * @see RemoveFromIndexOperation
 */
public class RemoveFromIndexAction extends RepositoryAction {

	/**
	 * Constructs this action
	 */
	public RemoveFromIndexAction() {
		super(REMOVE_FROM_INDEX, new RemoveFromIndexActionHandler());
	}

}
