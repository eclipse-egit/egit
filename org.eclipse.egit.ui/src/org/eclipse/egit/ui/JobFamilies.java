/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Job families of EGit jobs. May be used in tests to join job execution.
 *
 */
public class JobFamilies {

	/**
	 * Job family with icon for progress reporting
	 */
	private static final class JobFamily {

		/**
		 * job family with custom progress icon
		 *
		 * @param imageDescriptor
		 *            icon for this job family
		 */
		protected JobFamily(final ImageDescriptor imageDescriptor) {
			IProgressService service = PlatformUI.getWorkbench()
					.getProgressService();
			service.registerIconForFamily(imageDescriptor, this);
		}

		/**
		 * job family with standard repository icon
		 */
		protected JobFamily() {
			this(UIIcons.REPOSITORY);
		}
	}

	/**
	 * GenerateHistoryJob
	 */
	public final static Object GENERATE_HISTORY = new JobFamily(
			UIIcons.HISTORY);

	/**
	 * History diff jobs
	 */
	public final static Object HISTORY_DIFF = new JobFamily();

	/**
	 * History file diff generation jobs
	 */
	public final static Object HISTORY_FILE_DIFF = new JobFamily();

	/**
	 * Commit job
	 */
	public final static Object COMMIT = new JobFamily(UIIcons.COMMIT);

	/**
	 * Checkout job
	 */
	public final static Object CHECKOUT = new JobFamily(UIIcons.CHECKOUT);

	/**
	 * Merge job
	 */
	public final static Object MERGE = new JobFamily(UIIcons.MERGE);

	/**
	 * Push job
	 */
	public final static Object PUSH = new JobFamily(UIIcons.PUSH);

	/**
	 * Fetch job
	 */
	public final static Object FETCH = new JobFamily(UIIcons.FETCH);

	/**
	 * Repositories View refresh
	 */
	public final static Object REPO_VIEW_REFRESH = new JobFamily();

	/**
	 * Delete repository job
	 */
	public final static Object REPOSITORY_DELETE = new JobFamily(
			UIIcons.ELCL16_DELETE);

	/**
	 * Tag job
	 */
	public final static Object TAG = new JobFamily(UIIcons.TAG);

	/**
	 * Reset job
	 */
	public static final Object RESET = new JobFamily(UIIcons.RESET);

	/**
	 * Rebase job
	 */
	public static final Object REBASE = new JobFamily(UIIcons.REBASE);

	/**
	 * Pull job
	 */
	public final static Object PULL = new JobFamily(UIIcons.PULL);

	/**
	 * Format job
	 */
	public final static Object FORMAT_COMMIT_INFO = new JobFamily();

	/**
	 * Commit editor job
	 */
	public final static Object COMMIT_EDITOR = new JobFamily();

	/**
	 * Fill tag list
	 */
	public final static Object FILL_TAG_LIST = new JobFamily(UIIcons.TAGS);

	/**
	 * AssumeUnchanged/NoAssumeUnchanged
	 */
	public final static Object ASSUME_NOASSUME_UNCHANGED = new JobFamily();

	/**
	 * Untrack
	 */
	public final static Object UNTRACK = new JobFamily();

	/**
	 * Disconnect
	 */
	public final static Object DISCONNECT = new JobFamily();

	/**
	 * Discard Changes
	 */
	public final static Object DISCARD_CHANGES = new JobFamily();


	/**
	 * Add to index job
	 */
	public static final Object ADD_TO_INDEX = new JobFamily(UIIcons.ELCL16_ADD);

	/**
	 * Remove from index job
	 */
	public static final Object REMOVE_FROM_INDEX = new JobFamily();

	/**
	 * Updates staging view repository on selection change
	 */
	public static final Object UPDATE_SELECTION = new JobFamily();

	/**
	 * Cherry pick commit job
	 */
	public static final Object CHERRY_PICK = new JobFamily(UIIcons.CHERRY_PICK);

	/**
	 * Squash commits job
	 */
	public static final Object SQUASH = new JobFamily(UIIcons.SQUASH_DOWN);

	/**
	 * Reword commit job
	 */
	public static final Object REWORD = new JobFamily(UIIcons.REWORD);

	/**
	 * Edit commit job
	 */
	public static final Object EDIT = new JobFamily();

	/**
	 * Revert commit job
	 */
	public static final Object REVERT_COMMIT = new JobFamily();

	/**
	 * Clone repository job
	 */
	public static final Object CLONE = new JobFamily(UIIcons.CLONEGIT);

	/**
	 * Fetch data from git job
	 */
	public static final Object SYNCHRONIZE_READ_DATA = new JobFamily();

	/**
	 * Show annotations git job
	 */
	public static final Object BLAME = new JobFamily();

	/**
	 * Submodule add git job
	 */
	public static final Object SUBMODULE_ADD = new JobFamily();

	/**
	 * Submodule sync git job
	 */
	public static final Object SUBMODULE_SYNC = new JobFamily();

	/**
	 * Submodule update git job
	 */
	public static final Object SUBMODULE_UPDATE = new JobFamily();

	/**
	 * Stash git job
	 */
	public static final Object STASH = new JobFamily(UIIcons.STASH);

	/**
	 * Staging view reload
	 */
	public static final Object STAGING_VIEW_RELOAD = new JobFamily();

}
