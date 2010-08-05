/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.ui.internal.commands.SharedCommands;

/**
 * Check out a commit
 */
public class CheckoutCommitAction extends RepositoryAction {
	/**
	 * Constructs this action
	 */
	public CheckoutCommitAction() {
		super(SharedCommands.CHECKOUT, new CheckoutCommitActionHandler());
	}
}
