/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Rebase interactive"
 */
public class RebaseInteractiveCommand extends AbstractRebaseCommandHandler {

	/**
	 * Default constructor
	 */
	public RebaseInteractiveCommand() {
		super(RebaseCommand.Operation.BEGIN, "some jobname", //$NON-NLS-1$
				RebaseHelper.DEFAULT_CANCEL_DIALOG_MESSAGE, true);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO: do some checks?
		Repository repository = getRepository(event);
		BasicConfigurationDialog.show(repository);
		return super.execute(event);
	}

}
