/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - factor out AbstractToggleCommand
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

/**
 * Toggles the "Hierarchical Branch Representation" preference.
 */
public class ToggleBranchHierarchyCommand extends AbstractToggleCommand {

	/**
	 * The toggle branch hierarchy command id
	 */
	public static final String ID = "org.eclipse.egit.ui.RepositoriesToggleBranchHierarchy"; //$NON-NLS-1$

	/**
	 * The toggle state of this command
	 */
	public static final String TOGGLE_STATE = "org.eclipse.ui.commands.toggleState"; //$NON-NLS-1$

}
