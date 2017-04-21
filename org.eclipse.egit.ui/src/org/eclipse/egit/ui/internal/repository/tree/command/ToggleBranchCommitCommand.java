/*******************************************************************************
 *  Copyright (c) 2011, 2017 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - factor out AbstractToggleCommand
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

/**
 * Toggles the "Display Latest Branch Commit" preference.
 */
public class ToggleBranchCommitCommand extends AbstractToggleCommand {

	/**
	 * The toggle branch latest commit command id
	 */
	public static final String ID = "org.eclipse.egit.ui.RepositoriesToggleBranchCommit"; //$NON-NLS-1$

	/**
	 * The toggle state of this command
	 */
	public static final String TOGGLE_STATE = "org.eclipse.ui.commands.toggleState"; //$NON-NLS-1$

}
