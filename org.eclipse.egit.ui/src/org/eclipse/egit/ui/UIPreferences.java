/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;


/**
 * Preferences used by the EGit UI plug-in.
 * <p>
 * All plug-in preferences shall be referenced by a constant in this class.
 */
public class UIPreferences {
	/** */
	public final static String RESOURCEHISTORY_SHOW_COMMENT_WRAP = "resourcehistory_show_comment_wrap"; //$NON-NLS-1$
	/** */
	public static final String RESOURCEHISTORY_SHOW_COMMENT_FILL = "resourcehistory_fill_comment_paragraph"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_SHOW_REV_DETAIL = "resourcehistory_show_rev_detail"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_SHOW_REV_COMMENT = "resourcehistory_show_rev_comment"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_GRAPH_SPLIT = "resourcehistory_graph_split"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_REV_SPLIT = "resourcehistory_rev_split"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_SHOW_TOOLTIPS = "resourcehistory_show_tooltips"; //$NON-NLS-1$
	/** */
	public final static String RESOURCEHISTORY_SHOW_FINDTOOLBAR = "resourcehistory_show_findtoolbar"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_IGNORE_CASE = "findtoolbar_ignore_case"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_COMMIT_ID = "findtoolbar_commit_id"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_COMMENTS = "findtoolbar_comments"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_AUTHOR = "findtoolbar_author"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_COMMITTER = "findtoolbar_committer"; //$NON-NLS-1$
	/** */
	public final static String FINDTOOLBAR_FIND_IN = "findtoolbar_find_in"; //$NON-NLS-1$

	/** */
	public final static String THEME_CommitGraphNormalFont = "org.eclipse.egit.ui.CommitGraphNormalFont"; //$NON-NLS-1$
	/** */
	public final static String THEME_CommitGraphHighlightFont = "org.eclipse.egit.ui.CommitGraphHighlightFont"; //$NON-NLS-1$
	/** */
	public final static String THEME_CommitMessageFont = "org.eclipse.egit.ui.CommitMessageFont"; //$NON-NLS-1$
	/** */
	public final static String THEME_UncommittedChangeForegroundColor = "org.eclipse.egit.ui.UncommittedChangeForegroundColor"; //$NON-NLS-1$
	/** */
	public final static String THEME_UncommittedChangeBackgroundColor = "org.eclipse.egit.ui.UncommittedChangeBackgroundColor"; //$NON-NLS-1$
	/** */
	public final static String THEME_UncommittedChangeFont = "org.eclipse.egit.ui.UncommittedChangeFont"; //$NON-NLS-1$

	/** */
	public final static String DECORATOR_RECOMPUTE_ANCESTORS = "decorator_recompute_ancestors"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_RECURSIVE_LIMIT = "decorator_recursive_limit"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_FILETEXT_DECORATION = "decorator_filetext_decoration"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_FOLDERTEXT_DECORATION = "decorator_foldertext_decoration"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_PROJECTTEXT_DECORATION = "decorator_projecttext_decoration"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_TRACKED_ICON = "decorator_show_tracked_icon"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_UNTRACKED_ICON = "decorator_show_untracked_icon"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_STAGED_ICON = "decorator_show_staged_icon"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_CONFLICTS_ICON = "decorator_show_conflicts_icon"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_ASSUME_VALID_ICON = "decorator_show_assume_valid_icon"; //$NON-NLS-1$
	/** */
	public final static String DECORATOR_SHOW_DIRTY_ICON = "decorator_show_dirty_icon"; //$NON-NLS-1$
	/** */
	public static final String REFESH_ON_INDEX_CHANGE = "refesh_on_index_change"; //$NON-NLS-1$
	/** */
	public static final String REFESH_ONLY_WHEN_ACTIVE = "refesh_only_when_active"; //$NON-NLS-1$
	/** */
	public static final String REMOTE_CONNECTION_TIMEOUT = "remote_connection_timeout"; //$NON-NLS-1$
	/** */
	public static final String DEFAULT_REPO_DIR = "default_repository_dir"; //$NON-NLS-1$

	/**
	 * Converts a persisted String separated with commas to an integer array
	 *
	 * @param value
	 *            the String value
	 * @param cnt
	 *            number of entries in the returned array
	 * @return the preference values for the array.
	 */
	public static int[] stringToIntArray(final String value, final int cnt) {
		final int[] r = new int[cnt];
		if (value != null) {
			final String[] e = value.split(","); //$NON-NLS-1$
			for (int i = 0; i < Math.min(e.length, r.length); i++)
				r[i] = Integer.parseInt(e[i].trim());
		}
		return r;
	}

	/**
	 * Converts an integer array into a String separated by commas
	 *
	 * @param data
	 *            integers to store
	 * @return the String
	 */
	public static String intArrayToString(final int[] data) {
		final StringBuilder s = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0)
				s.append(',');
			s.append(data[i]);
		}
		return s.toString();
	}
}
