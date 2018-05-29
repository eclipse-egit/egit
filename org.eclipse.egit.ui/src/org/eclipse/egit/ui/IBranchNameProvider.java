/*******************************************************************************
 * Copyright (C) 2012, Manuel Doninger <manuel.doninger@googlemail.com>
 * Copyright (C) 2012, Steffen Pingel <steffen.pingel@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
