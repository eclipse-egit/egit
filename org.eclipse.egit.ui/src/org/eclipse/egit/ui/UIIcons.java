/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

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

	/** Decoration for resource not being tracked by Git */
	public final static ImageDescriptor OVR_UNTRACKED;

	/** Decoration for tracked resource with a merge conflict.  */
	public final static ImageDescriptor OVR_CONFLICT;

	/** Decoration for tracked resources that we want to ignore changes in. */
	public final static ImageDescriptor OVR_ASSUMEVALID;

	/** Decoration for tracked resources that are dirty. */
	public final static ImageDescriptor OVR_DIRTY;

	/** Decoration for lightweight tags **/
	public final static ImageDescriptor OVR_LIGHTTAG;

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
	/** Trash icon */
	public final static ImageDescriptor ELCL16_TRASH;
	/** Clear icon */
	public final static ImageDescriptor ELCL16_CLEAR;
	/** Refresh icon */
	public final static ImageDescriptor ELCL16_REFRESH;
	/** Linked with icon */
	public final static ImageDescriptor ELCL16_SYNCED;

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

	/** New Repository button */
	public final static ImageDescriptor NEW_REPOSITORY;

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

	/** base URL */
	public final static URL base;

	static {
		base = init();
		OVR_STAGED = map("ovr/staged.gif"); //$NON-NLS-1$
		OVR_STAGED_ADD = map("ovr/staged_added.gif"); //$NON-NLS-1$
		OVR_STAGED_REMOVE = map("ovr/staged_removed.gif"); //$NON-NLS-1$
		OVR_UNTRACKED = map("ovr/untracked.gif"); //$NON-NLS-1$
		OVR_CONFLICT = map("ovr/conflict.gif"); //$NON-NLS-1$
		OVR_ASSUMEVALID = map("ovr/assume_valid.gif"); //$NON-NLS-1$
		OVR_DIRTY = map("ovr/dirty.gif"); //$NON-NLS-1$
		OVR_LIGHTTAG = map("ovr/lighttag.gif"); //$NON-NLS-1$
		ELCL16_FIND = map("elcl16/find.gif"); //$NON-NLS-1$
		ELCL16_COMPARE_VIEW = map("elcl16/compare_view.gif"); //$NON-NLS-1$
		ELCL16_NEXT = map("elcl16/next.gif"); //$NON-NLS-1$
		ELCL16_PREVIOUS = map("elcl16/previous.gif"); //$NON-NLS-1$
		WIZBAN_CREATE_PATCH = map("wizban/createpatch_wizban.png"); //$NON-NLS-1$
		WIZBAN_IMPORT_REPO = map("wizban/import_wiz.png"); //$NON-NLS-1$
		WIZBAN_CONNECT_REPO = map("wizban/newconnect_wizban.png"); //$NON-NLS-1$
		EDITCONFIG = map("obj16/editconfig.gif"); //$NON-NLS-1$
		ELCL16_COMMIT = map("elcl16/commit.gif"); //$NON-NLS-1$
		ELCL16_COMMENTS = map("elcl16/comment.gif"); //$NON-NLS-1$
		ELCL16_AUTHOR = map("elcl16/author.gif"); //$NON-NLS-1$
		ELCL16_COMMITTER = map("elcl16/committer.gif"); //$NON-NLS-1$
		ELCL16_DELETE = map("elcl16/delete.gif"); //$NON-NLS-1$
		ELCL16_ADD = map("elcl16/add.gif"); //$NON-NLS-1$
		ELCL16_TRASH = map("elcl16/trash.gif"); //$NON-NLS-1$
		ELCL16_CLEAR = map("elcl16/clear.gif"); //$NON-NLS-1$
		ELCL16_REFRESH = map("elcl16/refresh.gif"); //$NON-NLS-1$
		ELCL16_SYNCED = map("elcl16/synced.gif"); //$NON-NLS-1$
		ELCL16_ID = map("elcl16/sha1.gif"); //$NON-NLS-1$
		CHECKBOX_ENABLED_CHECKED = map("checkboxes/enabled_checked.gif"); //$NON-NLS-1$
		CHECKBOX_ENABLED_UNCHECKED = map("checkboxes/enabled_unchecked.gif"); //$NON-NLS-1$
		CHECKBOX_DISABLED_CHECKED = map("checkboxes/disabled_checked.gif"); //$NON-NLS-1$
		CHECKBOX_DISABLED_UNCHECKED = map("checkboxes/disabled_unchecked.gif"); //$NON-NLS-1$
		FILTERRESOURCE = map("elcl16/filterresource.gif"); //$NON-NLS-1$
		FILTERPROJECT = map("elcl16/filterproject.gif"); //$NON-NLS-1$
		FILTERFOLDER = map("elcl16/filterfolder.gif"); //$NON-NLS-1$
		FETCH = map("obj16/fetch.gif"); //$NON-NLS-1$
		PUSH = map("obj16/push.gif"); //$NON-NLS-1$
		PULL = map("obj16/pull.gif"); //$NON-NLS-1$
		REPOSITORY = map("obj16/repository_rep.gif"); //$NON-NLS-1$
		NEW_REPOSITORY = map("etool16/newlocation_wiz.gif"); //$NON-NLS-1$
		REMOTE_REPOSITORY = map("obj16/remote_entry_tbl.gif"); //$NON-NLS-1$
		REMOTE_SPEC = map("obj16/remotespec.gif"); //$NON-NLS-1$
		BRANCHES = map("obj16/branches_obj.gif"); //$NON-NLS-1$
		OVR_CHECKEDOUT = map("ovr/checkedout_ov.gif"); //$NON-NLS-1$
		TAGS = map("obj16/tags.gif"); //$NON-NLS-1$
		TAG = map("obj16/version_rep.gif"); //$NON-NLS-1$
		CREATE_TAG = map("obj16/new_tag_obj.gif"); //$NON-NLS-1$
		BRANCH = map("obj16/branch_obj.gif"); //$NON-NLS-1$
		CREATE_BRANCH = map("obj16/new_branch_obj.gif"); //$NON-NLS-1$
		COLLAPSEALL = map("elcl16/collapseall.gif"); //$NON-NLS-1$
		CLONEGIT = map("obj16/cloneGit.gif"); //$NON-NLS-1$
		RESET = map("obj16/reset.gif"); //$NON-NLS-1$
		CHANGESET = map("obj16/changelog_obj.gif"); //$NON-NLS-1$
		GERRIT = map("obj16/gerrit_obj.gif"); //$NON-NLS-1$
		EXPAND_ALL = map("wizban/expandall.gif"); //$NON-NLS-1$
		CHECKOUT = map("obj16/checkout.gif"); //$NON-NLS-1$
		SIGNED_OFF = map("obj16/signed-off.png"); //$NON-NLS-1$
		CHECK_ALL = map("obj16/check_all.gif"); //$NON-NLS-1$
		UNCHECK_ALL = map("obj16/uncheck_all.gif"); //$NON-NLS-1$
	}

	private static ImageDescriptor map(final String icon) {
		if (base != null) {
			try {
				return ImageDescriptor.createFromURL(new URL(base, icon));
			} catch (MalformedURLException mux) {
				Activator.logError(UIText.UIIcons_errorLoadingPluginImage, mux);
			}
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
}
