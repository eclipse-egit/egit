/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

/**
 * Action for selecting a commit and merging it with the current branch.
 */
public class MergeAction extends RepositoryAction {

	/**
	 *
	 */
	public MergeAction() {
		super(ActionCommands.MERGE_ACTION, new MergeActionHandler());
	}
}
