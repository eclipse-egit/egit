/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2013, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2014, Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2015, Denis Zygann <d.zygann@web.de>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

/**
 * Icons for the the Eclipse plugin. Mostly decorations.
 */
public class UIIcons {

	/** Decoration for resource in the index but not yet committed. */
	public final static ImageDescriptor OVR_STAGED;

	/** Decoration for resource added to index but not yet committed. */
	public final static ImageDescriptor OVR_STAGED_ADD;

	/** Decoration for resource removed from the index but not commit. */
	public final static ImageDescriptor OVR_STAGED_REMOVE;

	/** Decoration for resource that was removed and added with another name */
	public static final ImageDescriptor OVR_STAGED_RENAME;

	/** Decoration for resource not being tracked by Git */
	public final static ImageDescriptor OVR_UNTRACKED;

	/** Decoration for tracked resource with a merge conflict.  */
	public final static ImageDescriptor OVR_CONFLICT;

	/** Decoration for tracked resources that we want to ignore changes in. */
	public final static ImageDescriptor OVR_ASSUMEUNCHANGED;

	/** Decoration for tracked resources that are dirty. */
	public final static ImageDescriptor OVR_DIRTY;

	/** Decoration for warning **/
	public final static ImageDescriptor OVR_ERROR;

	/** Decoration for symlink **/
	public final static ImageDescriptor OVR_SYMLINK;

	/** Find icon */
	public final static ImageDescriptor ELCL16_FIND;
	/** Compare / View icon */
	public final static ImageDescriptor ELCL16_COMPARE_VIEW;
	/** Next arrow icon */
	public final static ImageDescriptor ELCL16_NEXT;
	/** Previous arrow icon */
	public final static ImageDescriptor ELCL16_PREVIOUS;
	/** Commit icon */
	public final static ImageDescriptor ELCL16_COMMIT;
	/** Comments icon */
	public final static ImageDescriptor ELCL16_COMMENTS;
	/** Author icon */
	public final static ImageDescriptor ELCL16_AUTHOR;
	/** Committer icon */
	public final static ImageDescriptor ELCL16_COMMITTER;
	/** Id icon */
	public final static ImageDescriptor ELCL16_ID;
	/** Delete icon */
	public final static ImageDescriptor ELCL16_DELETE;
	/** Add icon */
	public final static ImageDescriptor ELCL16_ADD;
	/** "Add all" icon */
	public final static ImageDescriptor ELCL16_ADD_ALL;
	/** Trash icon */
	public final static ImageDescriptor ELCL16_TRASH;
	/** Clear icon */
	public final static ImageDescriptor ELCL16_CLEAR;
	/** Refresh icon */
	public final static ImageDescriptor ELCL16_REFRESH;
	/** Linked with icon */
	public final static ImageDescriptor ELCL16_SYNCED;
	/** Filter icon */
	public final static ImageDescriptor ELCL16_FILTER;

	/** Enabled, checked, checkbox image */
	public final static ImageDescriptor CHECKBOX_ENABLED_CHECKED;
	/** Enabled, unchecked, checkbox image */
	public final static ImageDescriptor CHECKBOX_ENABLED_UNCHECKED;
	/** Disabled, checked, checkbox image */
	public final static ImageDescriptor CHECKBOX_DISABLED_CHECKED;
	/** Disabled, unchecked, checkbox image */
	public final static ImageDescriptor CHECKBOX_DISABLED_UNCHECKED;
	/** Edit configuration */
	public final static ImageDescriptor EDITCONFIG;
	/** Create Patch Wizard banner */
	public final static ImageDescriptor WIZBAN_CREATE_PATCH;

	/** Import Wizard banner */
	public final static ImageDescriptor WIZBAN_IMPORT_REPO;

	/** Connect Wizard banner */
	public final static ImageDescriptor WIZBAN_CONNECT_REPO;

	/**
	 * Commit Wizard banner
	 * @TODO make use of this wizban
	 */
	public final static ImageDescriptor WIZBAN_COMMIT;

	/** Fetch from Gerrit Wizard banner */
	public final static ImageDescriptor WIZBAN_FETCH_GERRIT;

	/** Fetch Wizard banner */
	public final static ImageDescriptor WIZBAN_FETCH;

	/**
	 * Pull Wizard banner
	 * @TODO make use of this wizban
	 */
	public final static ImageDescriptor WIZBAN_PULL;

	/** Push to Gerrit Wizard banner */
	public final static ImageDescriptor WIZBAN_PUSH_GERRIT;

	/** Push Wizard banner */
	public final static ImageDescriptor WIZBAN_PUSH;

	/** Synchronize Wizard banner */
	public final static ImageDescriptor WIZBAN_SYNCHRONIZE;

	/** History view, select all version in same project */
	public final static ImageDescriptor FILTERPROJECT;

	/** History view, select all version in same folder */
	public final static ImageDescriptor FILTERFOLDER;

	/** History view, select all version of resource */
	public final static ImageDescriptor FILTERRESOURCE;

	/** Import button */
	public final static ImageDescriptor FETCH;

	/** Import button */
	public final static ImageDescriptor PULL;

	/** Export button */
	public final static ImageDescriptor PUSH;

	/** Collapse all button */
	public final static ImageDescriptor COLLAPSEALL;

	/** Repository tree node */
	public final static ImageDescriptor REPOSITORY;

	/** Gerrit Repository tree node */
	public final static ImageDescriptor REPOSITORY_GERRIT;

	/** New Repository button */
	public final static ImageDescriptor NEW_REPOSITORY;

	/** Create Repository button */
	public final static ImageDescriptor CREATE_REPOSITORY;

	/** Remote Repository tree node */
	public final static ImageDescriptor REMOTE_REPOSITORY;

	/** Reset */
	public final static ImageDescriptor RESET;

	/** Remote Repository tree node */
	public final static ImageDescriptor REMOTE_SPEC;

	/** Branches tree node */
	public final static ImageDescriptor BRANCHES;

	/** Checked-out decorator for branch */
	public final static ImageDescriptor OVR_CHECKEDOUT;

	/** Tags icon */
	public final static ImageDescriptor TAGS;

	/** Tag icon */
	public final static ImageDescriptor TAG;

	/** Create Tag icon */
	public final static ImageDescriptor CREATE_TAG;

	/** Branch icon */
	public final static ImageDescriptor BRANCH;

	/** Create Branch icon */
	public final static ImageDescriptor CREATE_BRANCH;

	/** Clone Icon */
	public final static ImageDescriptor CLONEGIT;

	/** Changeset Icon */
	public final static ImageDescriptor CHANGESET;

	/** Gerrit Icon */
	public final static ImageDescriptor GERRIT;

	/** Expand all icon */
	public final static ImageDescriptor EXPAND_ALL;

	/** Checkout icon */
	public final static ImageDescriptor CHECKOUT;

	/** Signed Off By icon */
	public final static ImageDescriptor SIGNED_OFF;

	/** Check all icon */
	public final static ImageDescriptor CHECK_ALL;

	/** Uncheck all icon */
	public final static ImageDescriptor UNCHECK_ALL;

	/** Amend commit icon */
	public final static ImageDescriptor AMEND_COMMIT;

	/** Untracked file icon */
	public final static ImageDescriptor UNTRACKED_FILE;

	/** Commit note icon */
	public final static ImageDescriptor NOTE;

	/** Show Annotation icon */
	public final static ImageDescriptor ANNOTATE;

	/** Commit icon */
	public final static ImageDescriptor COMMIT;

	/** Cherry-pick icon */
	public final static ImageDescriptor CHERRY_PICK;

	/** Rebase icon */
	public final static ImageDescriptor REBASE;

	/** Rebase continue icon */
	public final static ImageDescriptor REBASE_CONTINUE;

	/** Rebase skip icon */
	public final static ImageDescriptor REBASE_SKIP;

	/** Rebase abort icon */
	public final static ImageDescriptor REBASE_ABORT;

	/** Rebase process steps icon */
	public final static ImageDescriptor REBASE_PROCESS_STEPS;

	/** Merge icon */
	public final static ImageDescriptor MERGE;

	/** Annotated tag icon */
	public final static ImageDescriptor TAG_ANNOTATED;

	/** Submodules icon */
	public final static ImageDescriptor SUBMODULES;

	/** Clean icon */
	public final static ImageDescriptor CLEAN;

	/** Stash icon */
	public final static ImageDescriptor STASH;

	/** Stash apply icon */
	public final static ImageDescriptor STASH_APPLY;

	/** History view icon */
	public final static ImageDescriptor HISTORY;

	/** Search commit icon */
	public final static ImageDescriptor SEARCH_COMMIT;

	/** Hierarchy layout icon */
	public final static ImageDescriptor HIERARCHY;

	/** Flat presentation icon */
	public final static ImageDescriptor FLAT;

	/** Compact tree presentation icon */
	public final static ImageDescriptor COMPACT;

	/** Squash icon arrow up */
	public final static ImageDescriptor SQUASH_UP;

	/** Squash icon arrow down */
	public final static ImageDescriptor SQUASH_DOWN;

	/** Fixup icon arrow up */
	public final static ImageDescriptor FIXUP_UP;

	/** Fixup icon arrow down */
	public final static ImageDescriptor FIXUP_DOWN;

	/** Revert icon */
	public final static ImageDescriptor REVERT;

	/** Reword icon */
	public final static ImageDescriptor REWORD;

	/** Icon for done rebase step */
	public final static ImageDescriptor DONE_STEP;

	/** Reword for current rebase step */
	public final static ImageDescriptor CURRENT_STEP;

	/** Sort alphabetically icon */
	public final static ImageDescriptor ALPHABETICALLY_SORT;

	/** Sort by state icon */
	public final static ImageDescriptor STATE_SORT;

	/** Unstage icon */
	public final static ImageDescriptor UNSTAGE;

	/** "Unstage all" icon */
	public final static ImageDescriptor UNSTAGE_ALL;

	/** {@link #BRANCH} overlaid with {@link #OVR_CHECKEDOUT}. */
	public final static ImageDescriptor CHECKED_OUT_BRANCH;

	/** base URL */
	public final static URL base;

	static {
		base = init();
		OVR_CHECKEDOUT = map("ovr/checkedout_ov.png"); //$NON-NLS-1$
		OVR_STAGED = map("ovr/staged.png"); //$NON-NLS-1$
		OVR_STAGED_ADD = map("ovr/staged_added.png"); //$NON-NLS-1$
		OVR_STAGED_REMOVE = map("ovr/staged_removed.png"); //$NON-NLS-1$
		OVR_STAGED_RENAME = map("ovr/staged_renamed.png"); //$NON-NLS-1$
		OVR_UNTRACKED = map("ovr/untracked.png"); //$NON-NLS-1$
		OVR_CONFLICT = map("ovr/conflict.png"); //$NON-NLS-1$
		OVR_ASSUMEUNCHANGED = map("ovr/assume_unchanged.png"); //$NON-NLS-1$
		OVR_DIRTY = map("ovr/dirty.png"); //$NON-NLS-1$
		OVR_SYMLINK = map("ovr/symlink_ovr.png"); //$NON-NLS-1$
		ELCL16_FIND = map("elcl16/find.png"); //$NON-NLS-1$
		ELCL16_COMPARE_VIEW = map("elcl16/compare_view.png"); //$NON-NLS-1$
		ELCL16_NEXT = map("elcl16/next_nav.png"); //$NON-NLS-1$
		ELCL16_PREVIOUS = map("elcl16/prev_nav.png"); //$NON-NLS-1$
		WIZBAN_CREATE_PATCH = map("wizban/createpatch_wizban.png"); //$NON-NLS-1$
		WIZBAN_IMPORT_REPO = map("wizban/import_wiz.png"); //$NON-NLS-1$
		WIZBAN_CONNECT_REPO = map("wizban/newconnect_wizban.png"); //$NON-NLS-1$
		WIZBAN_COMMIT = map("wizban/commit_wizban.png"); //$NON-NLS-1$
		WIZBAN_FETCH_GERRIT = map("wizban/fetch_gerrit_wizban.png"); //$NON-NLS-1$
		WIZBAN_FETCH = map("wizban/fetch_wizban.png"); //$NON-NLS-1$
		WIZBAN_PULL = map("wizban/pull_wizban.png"); //$NON-NLS-1$
		WIZBAN_PUSH_GERRIT = map("wizban/push_gerrit_wizban.png"); //$NON-NLS-1$
		WIZBAN_PUSH = map("wizban/push_wizban.png"); //$NON-NLS-1$
		WIZBAN_SYNCHRONIZE = map("wizban/synchronize_wizban.png"); //$NON-NLS-1$
		EDITCONFIG = map("obj16/editconfig.png"); //$NON-NLS-1$
		ELCL16_COMMIT = map("elcl16/commit.png"); //$NON-NLS-1$
		ELCL16_COMMENTS = map("elcl16/comment.png"); //$NON-NLS-1$
		ELCL16_AUTHOR = map("elcl16/author.png"); //$NON-NLS-1$
		ELCL16_COMMITTER = map("elcl16/committer.png"); //$NON-NLS-1$
		ELCL16_DELETE = map("elcl16/delete.png"); //$NON-NLS-1$
		ELCL16_ADD = map("elcl16/add.png"); //$NON-NLS-1$
		ELCL16_ADD_ALL = map("elcl16/add_all.png"); //$NON-NLS-1$
		ELCL16_TRASH = map("elcl16/trash.png"); //$NON-NLS-1$
		ELCL16_CLEAR = map("elcl16/clear_co.png"); //$NON-NLS-1$
		ELCL16_REFRESH = map("elcl16/refresh.png"); //$NON-NLS-1$
		ELCL16_SYNCED = map("elcl16/synced.png"); //$NON-NLS-1$
		ELCL16_FILTER = map("elcl16/filter_ps.png"); //$NON-NLS-1$
		ELCL16_ID = map("elcl16/sha1.png"); //$NON-NLS-1$
		CHECKBOX_ENABLED_CHECKED = map("checkboxes/enabled_checked.png"); //$NON-NLS-1$
		CHECKBOX_ENABLED_UNCHECKED = map("checkboxes/enabled_unchecked.png"); //$NON-NLS-1$
		CHECKBOX_DISABLED_CHECKED = map("checkboxes/disabled_checked.png"); //$NON-NLS-1$
		CHECKBOX_DISABLED_UNCHECKED = map("checkboxes/disabled_unchecked.png"); //$NON-NLS-1$
		FILTERRESOURCE = map("elcl16/filterresource.png"); //$NON-NLS-1$
		FILTERPROJECT = map("elcl16/filterproject.png"); //$NON-NLS-1$
		FILTERFOLDER = map("elcl16/filterfolder.png"); //$NON-NLS-1$
		FETCH = map("obj16/fetch.png"); //$NON-NLS-1$
		PUSH = map("obj16/push.png"); //$NON-NLS-1$
		PULL = map("obj16/pull.png"); //$NON-NLS-1$
		REPOSITORY = map("obj16/repository_rep.png"); //$NON-NLS-1$
		REPOSITORY_GERRIT = map("obj16/repository_gerrit.png"); //$NON-NLS-1$
		NEW_REPOSITORY = map("etool16/newlocation_wiz.png"); //$NON-NLS-1$
		REMOTE_REPOSITORY = map("obj16/remote_entry_tbl.png"); //$NON-NLS-1$
		REMOTE_SPEC = map("obj16/synchronize.png"); //$NON-NLS-1$
		BRANCHES = map("obj16/branches_obj.png"); //$NON-NLS-1$
		TAGS = map("obj16/tags.png"); //$NON-NLS-1$
		TAG = map("obj16/version_rep.png"); //$NON-NLS-1$
		CREATE_TAG = map("obj16/new_tag_obj.png"); //$NON-NLS-1$
		BRANCH = map("obj16/branch_obj.png"); //$NON-NLS-1$
		CREATE_BRANCH = map("obj16/new_branch_obj.png"); //$NON-NLS-1$
		COLLAPSEALL = map("elcl16/collapseall.png"); //$NON-NLS-1$
		CLONEGIT = map("obj16/cloneGit.png"); //$NON-NLS-1$
		RESET = map("obj16/reset.png"); //$NON-NLS-1$
		CHANGESET = map("obj16/changelog_obj.png"); //$NON-NLS-1$
		GERRIT = map("obj16/gerrit_obj.png"); //$NON-NLS-1$
		EXPAND_ALL = map("elcl16/expandall.png"); //$NON-NLS-1$
		CHECKOUT = map("obj16/checkout.png"); //$NON-NLS-1$
		SIGNED_OFF = map("obj16/signed-off.png"); //$NON-NLS-1$
		CHECK_ALL = map("obj16/check_all.png"); //$NON-NLS-1$
		UNCHECK_ALL = map("obj16/uncheck_all.png"); //$NON-NLS-1$
		AMEND_COMMIT = map("obj16/commit_amend.png"); //$NON-NLS-1$
		UNTRACKED_FILE = map("obj16/untracked_file.png"); //$NON-NLS-1$
		NOTE = map("obj16/note.png"); //$NON-NLS-1$
		ANNOTATE = map("etool16/annotate.png"); //$NON-NLS-1$
		COMMIT = map("obj16/commit.png"); //$NON-NLS-1$
		CHERRY_PICK = map("obj16/cherry-pick.png"); //$NON-NLS-1$
		REBASE = map("obj16/rebase.png"); //$NON-NLS-1$
		REBASE_CONTINUE = map("elcl16/continue.png"); //$NON-NLS-1$
		REBASE_SKIP = map("elcl16/skip.png"); //$NON-NLS-1$
		REBASE_ABORT = map("elcl16/progress_stop.png"); //$NON-NLS-1$
		REBASE_PROCESS_STEPS = map("elcl16/start.png"); //$NON-NLS-1$
		OVR_ERROR = map("ovr/error.png"); //$NON-NLS-1$
		MERGE = map("obj16/merge.png"); //$NON-NLS-1$
		TAG_ANNOTATED = map("obj16/annotated-tag.png"); //$NON-NLS-1$
		CREATE_REPOSITORY = map("etool16/createRepository.png"); //$NON-NLS-1$
		SUBMODULES = map("obj16/submodules.png"); //$NON-NLS-1$
		CLEAN = map("obj16/clean_obj.png"); //$NON-NLS-1$
		STASH = map("obj16/stash.png"); //$NON-NLS-1$
		STASH_APPLY = map("obj16/stash-apply.png"); //$NON-NLS-1$
		HISTORY = map("obj16/history.png"); //$NON-NLS-1$
		SEARCH_COMMIT = map("obj16/search-commit.png"); //$NON-NLS-1$
		HIERARCHY = map("elcl16/hierarchicalLayout.png"); //$NON-NLS-1$
		FLAT = map("elcl16/flatLayout.png"); //$NON-NLS-1$
		COMPACT = map("elcl16/compactLayout.png"); //$NON-NLS-1$
		SQUASH_UP = map("obj16/squash-up.png"); //$NON-NLS-1$
		SQUASH_DOWN = map("obj16/squash-down.png"); //$NON-NLS-1$
		FIXUP_UP = map("obj16/fixup-up.png"); //$NON-NLS-1$
		FIXUP_DOWN = map("obj16/fixup-down.png"); //$NON-NLS-1$
		REVERT = map("obj16/revert.png"); //$NON-NLS-1$
		REWORD = map("obj16/reword.png"); //$NON-NLS-1$
		DONE_STEP = map("obj16/done_step.png"); //$NON-NLS-1$
		CURRENT_STEP = map("obj16/current_step.png"); //$NON-NLS-1$
		ALPHABETICALLY_SORT = map("obj16/alphab_sort_co.png"); //$NON-NLS-1$
		STATE_SORT = map("obj16/state_sort_co.png"); //$NON-NLS-1$
		UNSTAGE = map("obj16/unstage.png"); //$NON-NLS-1$
		UNSTAGE_ALL = map("elcl16/unstage_all.png"); //$NON-NLS-1$
		CHECKED_OUT_BRANCH = new DecorationOverlayDescriptor(BRANCH,
				OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
	}

	private static ImageDescriptor map(final String icon) {
		if (base != null)
			try {
				return ImageDescriptor.createFromURL(new URL(base, icon));
			} catch (MalformedURLException mux) {
				Activator.logError(UIText.UIIcons_errorLoadingPluginImage, mux);
			}
		return ImageDescriptor.getMissingImageDescriptor();
	}

	private static URL init() {
		try {
			return new URL(Activator.getDefault().getBundle().getEntry("/"), //$NON-NLS-1$
					"icons/"); //$NON-NLS-1$
		} catch (MalformedURLException mux) {
			Activator.logError(UIText.UIIcons_errorDeterminingIconBase, mux);
			return null;
		}
	}

	/**
	 * Get the image for the given descriptor from the resource manager which
	 * handles disposal of the image when the resource manager itself is
	 * disposed.
	 *
	 * @param resourceManager
	 *            {code ResourceManager} managing the image resources
	 * @param descriptor
	 *            object describing an image
	 * @return the image for the given descriptor
	 */
	public static Image getImage(ResourceManager resourceManager,
			ImageDescriptor descriptor) {
		return (Image) resourceManager.get(descriptor);
	}
}
