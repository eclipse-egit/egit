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

import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jgit.api.RebaseCommand;

/**
 * Implements PROCESS_STEPS operation
 *
 */
public class ProcessStepsRebaseCommand extends AbstractRebaseCommandHandler {

	/**
	 * Default constructor
	 */
	public ProcessStepsRebaseCommand() {
		// TODO: externalize jobname, define proper dialog cancel message
		super(RebaseCommand.Operation.PROCESS_STEPS,
				"some jobname", RebaseHelper.DEFAULT_CANCEL_DIALOG_MESSAGE, true); //$NON-NLS-1$
	}
}
