/*******************************************************************************
 * Copyright (C) 2012, Manuel Doninger <manuel.doninger@googlemail.com>
 * Copyright (C) 2012, Steffen Pingel <steffen.pingel@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

/**
 * A branch name provider suggests a branch name based on current context.
 */
public interface IBranchNameProvider {

	/**
	 * @return a branch name suggestion
	 */
	public String getBranchNameSuggestion();

}
