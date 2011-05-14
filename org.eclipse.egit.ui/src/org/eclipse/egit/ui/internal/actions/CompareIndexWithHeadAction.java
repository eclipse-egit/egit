/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Compares the index content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareIndexWithHeadAction extends RepositoryAction {

	/**
	 *
	 */
	public CompareIndexWithHeadAction() {
		super(ActionCommands.COMPARE_INDEX_WITH_HEAD_ACTION, new CompareIndexWithHeadActionHandler());
	}
}
