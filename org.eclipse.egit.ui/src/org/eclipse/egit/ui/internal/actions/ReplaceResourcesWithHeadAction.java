/*******************************************************************************
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.ui.internal.actions.DiscardChangesActionHandler.Replace;

/**
 * Checkout all selected dirty files.
 */
public class ReplaceResourcesWithHeadAction extends RepositoryAction {
	/**
	 *
	 */
	public ReplaceResourcesWithHeadAction() {
		super(ActionCommands.CHECKOUT_FROM_HEAD_ACTION, new DiscardChangesActionHandler(Replace.HEAD));
	}
}
