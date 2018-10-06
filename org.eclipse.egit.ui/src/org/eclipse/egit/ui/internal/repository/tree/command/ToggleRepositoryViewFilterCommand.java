/*******************************************************************************
 *  Copyright (c) 2011, 2017 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  copy of {@link ToggleBranchHierarchyCommand} with IDs for filter commands
 *
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

/**
 * Toggles a repository view filter preference.
 *
 */
public class ToggleRepositoryViewFilterCommand extends AbstractToggleCommand {

	// the toggle state id is the same as for ToggleBranchHierarchyCommand...
	/**
	 * The toggle show tags
	 */
	public static final String TOGGLE_TAGS_ID = "org.eclipse.egit.ui.RepositoriesToggleShowTags"; //$NON-NLS-1$

	/**
	 * The toggle show references
	 */
	public static final String TOGGLE_REFS_ID = "org.eclipse.egit.ui.RepositoriesToggleShowRefs"; //$NON-NLS-1$

	/**
	 * The toggle show remotes
	 */
	public static final String TOGGLE_REMOTES_ID = "org.eclipse.egit.ui.RepositoriesToggleShowRemotes"; //$NON-NLS-1$

	/**
	 * The toggle show working tree
	 */
	public static final String TOGGLE_WORKTREE_ID = "org.eclipse.egit.ui.RepositoriesToggleShowWorktree"; //$NON-NLS-1$

	/**
	 * The toggle show stashed commits
	 */
	public static final String TOGGLE_STASHES_ID = "org.eclipse.egit.ui.RepositoriesToggleShowStashes"; //$NON-NLS-1$

	/**
	 * The toggle show sub modules
	 */
	public static final String TOGGLE_SUBMODULES_ID = "org.eclipse.egit.ui.RepositoriesToggleShowSubmodules"; //$NON-NLS-1$

}
