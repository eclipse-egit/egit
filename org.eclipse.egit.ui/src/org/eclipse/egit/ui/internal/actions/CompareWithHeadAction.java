/*******************************************************************************
 * Copyright (C) 2009, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Compares the working tree content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareWithHeadAction extends RepositoryAction {

	/**
	 *
	 */
	public CompareWithHeadAction() {
		super(ActionCommands.COMPARE_WITH_HEAD_ACTION);
	}
}
