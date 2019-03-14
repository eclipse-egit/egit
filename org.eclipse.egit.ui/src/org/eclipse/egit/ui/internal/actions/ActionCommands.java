/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Commands wrapped by actions for menu contributions
 */
public class ActionCommands {
	/** Add to index action command id */
	public static final String ADD_TO_INDEX = "org.eclipse.egit.ui.team.AddToIndex"; //$NON-NLS-1$

	/** Remove from index action command id */
	public static final String REMOVE_FROM_INDEX = "org.eclipse.egit.ui.team.RemoveFromIndex"; //$NON-NLS-1$

	/** "Apply patch" action command id */
	public static final String APPLY_PATCH = "org.eclipse.egit.ui.team.ApplyPatch"; //$NON-NLS-1$

	/** "Create patch" action command id */
	public static final String CREATE_PATCH = "org.eclipse.egit.ui.team.CreatePatch"; //$NON-NLS-1$

	/** "Branch" action command id */
	public static final String BRANCH_ACTION = "org.eclipse.egit.ui.team.Branch"; //$NON-NLS-1$

	/**
	 * "Create Branch" command id. Because of bug 415382 we use a legacy command
	 * id here.
	 */
	public static final String BRANCH_CREATE = "org.eclipse.egit.ui.RepositoriesViewCreateBranch"; //$NON-NLS-1$

	/** "Check out" command id. */
	public static final String CHECK_OUT = "org.eclipse.egit.ui.CheckoutCommand"; //$NON-NLS-1$

	/** "Commit" action command id */
	public static final String COMMIT_ACTION = "org.eclipse.egit.ui.team.Commit"; //$NON-NLS-1$

	/** "Compare with head" action command id */
	public static final String COMPARE_WITH_HEAD_ACTION = "org.eclipse.egit.ui.team.CompareWithHead"; //$NON-NLS-1$

	/** "Compare index with head" action command id */
	public static final String COMPARE_INDEX_WITH_HEAD_ACTION = "org.eclipse.egit.ui.team.CompareIndexWithHead"; //$NON-NLS-1$

	/** "Compare with index" action command id */
	public static final String COMPARE_WITH_INDEX_ACTION = "org.eclipse.egit.ui.team.CompareWithIndex"; //$NON-NLS-1$

	/** "Compare with Ref" action command id */
	public static final String COMPARE_WITH_REF_ACTION = "org.eclipse.egit.ui.team.CompareWithRef"; //$NON-NLS-1$

	/** "Compare with Commit" action command id */
	public static final String COMPARE_WITH_COMMIT_ACTION = "org.eclipse.egit.ui.team.CompareWithCommit"; //$NON-NLS-1$

	/** "Compare with previous" action command id */
	public static final String COMPARE_WITH_PREVIOUS_ACTION = "org.eclipse.egit.ui.team.CompareWithPrevious"; //$NON-NLS-1$

	/** "Configure Fetch" action command id */
	public static final String CONFIGURE_FETCH = "org.eclipse.egit.ui.team.ConfigureFetch"; //$NON-NLS-1$

	/** "Configure push" action command id */
	public static final String CONFIGURE_PUSH = "org.eclipse.egit.ui.team.ConfigurePush"; //$NON-NLS-1$

	/** "Delete Branch" action command id */
	public static final String DELETE_BRANCH_ACTION = "org.eclipse.egit.ui.team.DeleteBranch"; //$NON-NLS-1$

	/** "Discard changes" action command id */
	public static final String DISCARD_CHANGES_ACTION = "org.eclipse.egit.ui.team.Discard"; //$NON-NLS-1$

	/** "Replace with HEAD" action command id */
	public static final String REPLACE_WITH_HEAD_ACTION = "org.eclipse.egit.ui.team.ReplaceWithHead"; //$NON-NLS-1$

	/** "Replace with Commit" action command id */
	public static final String REPLACE_WITH_COMMIT_ACTION = "org.eclipse.egit.ui.team.ReplaceWithCommit"; //$NON-NLS-1$

	/** "Replace with Ref" action command id */
	public static final String REPLACE_WITH_REF_ACTION = "org.eclipse.egit.ui.team.ReplaceWithRef"; //$NON-NLS-1$

	/** "Replace with previous version id */
	public static final String REPLACE_WITH_PREVIOUS_ACTION = "org.eclipse.egit.ui.team.ReplaceWithPrevious"; //$NON-NLS-1$

	/** "Disconnect" action command id */
	public static final String DISCONNECT_ACTION = "org.eclipse.egit.ui.internal.actions.Disconnect"; //$NON-NLS-1$

	/** "Fetch" action command id */
	public static final String FETCH_ACTION = "org.eclipse.egit.ui.team.Fetch"; //$NON-NLS-1$

	/** "Ignore" action command id */
	public static final String IGNORE_ACTION = "org.eclipse.egit.ui.team.Ignore"; //$NON-NLS-1$

	/** "Merge" action command id */
	public static final String MERGE_ACTION = "org.eclipse.egit.ui.team.Merge"; //$NON-NLS-1$

	/** "Push" action command id */
	public static final String PUSH_ACTION = "org.eclipse.egit.ui.team.Push"; //$NON-NLS-1$

	/** "Push branch" action command id */
	public static final String PUSH_BRANCH_ACTION = "org.eclipse.egit.ui.team.PushBranch"; //$NON-NLS-1$

	/** "Simple Push" action command id */
	public static final String SIMPLE_PUSH_ACTION = "org.eclipse.egit.ui.team.SimplePush"; //$NON-NLS-1$

	/** "Simple Fetch" action command id */
	public static final String SIMPLE_FETCH_ACTION = "org.eclipse.egit.ui.team.SimpleFetch"; //$NON-NLS-1$

	/** "Reset" action command id */
	public static final String RESET_ACTION = "org.eclipse.egit.ui.team.Reset"; //$NON-NLS-1$

	/** "Rebase" action command id */
	public static final String REBASE_ACTION = "org.eclipse.egit.ui.team.Rebase"; //$NON-NLS-1$

	/** "Stash Changes" action command id */
	public static final String STASH_CREATE = "org.eclipse.egit.ui.team.stash.create"; //$NON-NLS-1$

	/** "Apply Stash" command id */
	public static final String STASH_APPLY = "org.eclipse.egit.ui.team.stash.apply"; //$NON-NLS-1$

	/** "Drop Stash" command id */
	public static final String STASH_DROP = "org.eclipse.egit.ui.team.stash.drop"; //$NON-NLS-1$

	/** "Synchronize with" action command id */
	public static final String SYNC_WITH_ACTION = "org.eclipse.egit.ui.team.SyncWith"; //$NON-NLS-1$

	/** "Synchronize workspace" action command id */
	public static final String SYNC_WORKSPACE_ACTION = "org.eclipse.egit.ui.team.SyncWorkspace"; //$NON-NLS-1$

	/** "Tag" action command id */
	public static final String TAG_ACTION = "org.eclipse.egit.ui.team.Tag"; //$NON-NLS-1$

	/** "Track" action command id */
	public static final String TRACK_ACTION = "org.eclipse.egit.ui.team.Track"; //$NON-NLS-1$

	/** "Pull from upstream configuration" action command id */
	public static final String PULL_FROM_UPSTREAM_CONFIG = "org.eclipse.egit.ui.team.PullFromUpstreamConfig"; //$NON-NLS-1$

	/** "Pull with a config dialog" action command id */
	public static final String PULL_WITH_OPTIONS = "org.eclipse.egit.ui.team.PullWithOptions"; //$NON-NLS-1$

	/** "Merge Tool" action command id */
	public static final String MERGE_TOOL_ACTION = "org.eclipse.egit.ui.team.MergeTool"; //$NON-NLS-1$

}
