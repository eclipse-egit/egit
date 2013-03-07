/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.api.RebaseCommand.Operation;

/**
 * Implements "Continue Rebase"
 */
public class ContinueRebaseCommand extends AbstractRebaseCommandHandler {
	/**
	 * Default constructor
	 */
	public ContinueRebaseCommand() {
		super(Operation.CONTINUE, UIText.ContinueRebaseCommand_JobName,
				UIText.ContinueRebaseCommand_CancelDialogMessage);
	}
}
