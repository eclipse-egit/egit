/*******************************************************************************
 * Copyright (C) 2011, Ilya Ivanov <ilya.ivanov@intland.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.op.DiscardChangesOperation.ReplaceType;

/**
 * Checkout all selected dirty files.
 */
public class ReplaceResourcesWithHeadAction extends RepositoryAction {

	/***/
	public ReplaceResourcesWithHeadAction() {
		super(ActionCommands.CHECKOUT_FROM_HEAD_ACTION, new DiscardChangesActionHandler(ReplaceType.HEAD));
	}
}
