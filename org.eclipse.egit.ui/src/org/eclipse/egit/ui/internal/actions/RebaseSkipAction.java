/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/** An action for skip rebase command */
public class RebaseSkipAction extends RepositoryAction {

	/** */
	public RebaseSkipAction() {
		super(ActionCommands.REBASE_SKIP_ACTION, new CommandActionHandler(
				"org.eclipse.egit.ui.RepositoriesViewSkipRebase")); //$NON-NLS-1$
	}

}
