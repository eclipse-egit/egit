/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com >
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.internal.op.TagOperation;

/**
 * An action for creating tag.
 *
 * @see TagOperation
 */
public class UntrackAction extends RepositoryAction {
	/**
	 *
	 */
	public UntrackAction() {
		super(ActionCommands.UNTRACK_ACTION, new UntrackActionHandler());
	}
}
