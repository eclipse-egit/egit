/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.op.RebaseOperation;

/**
 * An action to rebase the current branch on top of selected one.
 *
 * @see RebaseOperation
 */
public class RebaseAction extends RepositoryAction {

	/**
	 *
	 */
	public RebaseAction() {
		super(ActionCommands.REBASE_ACTION, new RebaseActionHandler());
	}

}
