/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

/**
 * Job families of EGit jobs. May be used in tests to join job execution.
 *
 */
public class JobFamilies {
	/**
	 * GenerateHistoryJob
	 */
	public final static Object GENERATE_HISTORY = new Object();

	/**
	 * Commit job
	 */
	public final static Object COMMIT = new Object();

	/**
	 * Checkout job
	 */
	public final static Object CHECKOUT = new Object();

	/**
	 * Push job
	 */
	public final static Object PUSH = new Object();

	/**
	 * Fetch job
	 */
	public final static Object FETCH = new Object();

	/**
	 * Repositories View refresh
	 */
	public final static Object REPO_VIEW_REFRESH = new Object();

	/**
	 * Delete repository job
	 */
	public final static Object REPOSITORY_DELETE = new Object();

	/**
	 * Tag job
	 */
	public final static Object TAG = new Object();

	/**
	 * Reset job
	 */
	public static final Object RESET = new Object();

	/**
	 * Rebase job
	 */
	public static final Object REBASE = new Object();

	/**
	 * Pull job
	 */
	public final static Object PULL = new Object();

	/**
	 * Format job
	 */
	public final static Object FORMAT_COMMIT_INFO = new Object();

	/**
	 * Fill tag list
	 */
	public final static Object FILL_TAG_LIST = new Object();

	/**
	 * AssumeUnchanged/NoAssumeUnchanged
	 */
	public final static Object ASSUME_NOASSUME_UNCHANGED = new Object();

	/**
	 * Untrack
	 */
	public final static Object UNTRACK = new Object();

	/**
	 * Disconnect
	 */
	public final static Object DISCONNECT = new Object();

	/**
	 * Discard Changes
	 */
	public final static Object DISCARD_CHANGES = new Object();


	/**
	 * Add to index job
	 */
	public static final Object ADD_TO_INDEX = new Object();

	/**
	 * Remove from index job
	 */
	public static final Object REMOVE_FROM_INDEX = new Object();

	/**
	 * Cherry pick commit job
	 */
	public static final Object CHERRY_PICK = new Object();

	/**
	 * Revert commit job
	 */
	public static final Object REVERT_COMMIT = new Object();

	/**
	 * Clone repository job
	 */
	public static final Object CLONE = new Object();

	/**
	 * Fetch data from git job
	 */
	public static final Object SYNCHRONIZE_READ_DATA = new Object();

	/**
	 * Show annotations git job
	 */
	public static final Object BLAME = new Object();

	/**
	 * Submodule add git job
	 */
	public static final Object SUBMODULE_ADD = new Object();

	/**
	 * Submodule sync git job
	 */
	public static final Object SUBMODULE_SYNC = new Object();

	/**
	 * Submodule update git job
	 */
	public static final Object SUBMODULE_UPDATE = new Object();

	/**
	 * Stash git job
	 */
	public static final Object STASH = new Object();
}
