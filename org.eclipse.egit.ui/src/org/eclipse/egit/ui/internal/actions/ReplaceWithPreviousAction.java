/*******************************************************************************
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **/
package org.eclipse.egit.ui.internal.actions;

/**
 * Replace with previous revision action.
 */
public class ReplaceWithPreviousAction extends RepositoryAction {

	/**
	 * Create replace with previous revision action
	 */
	public ReplaceWithPreviousAction() {
		super(ActionCommands.REPLACE_WITH_PREVIOUS_ACTION,
				new ReplaceWithPreviousActionHandler());
	}
}
