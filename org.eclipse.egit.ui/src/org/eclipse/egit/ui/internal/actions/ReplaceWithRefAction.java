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
 * Replace with reference action
 */
public class ReplaceWithRefAction extends RepositoryAction {

	/** Create action */
	public ReplaceWithRefAction() {
		super(ActionCommands.REPLACE_WITH_REF_ACTION,
				new ReplaceWithRefActionHandler());
	}

}
