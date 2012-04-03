/*******************************************************************************
 * Copyright (C) 2010, Thorsten Kamann <thorsten@kamann.info>
 * Copyright (C) 2012, Manuel Doninger <manuel.doninger@googlemail.com>
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

/**
 * This interface must be implemented to be a branch name provider. A branch name
 * provider provides informations related to creating branches.
 */
public interface IBranchNameProvider extends ICommitMessageProvider {

	/**
	 * @return a branch name suggestion for the Creation wizard
	 */
	public String getBranchNameSuggestion();

	/**
	 * @return default source reference from the Preferences
	 */
	public String getDefaultSourceReference();

}
