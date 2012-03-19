/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
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
