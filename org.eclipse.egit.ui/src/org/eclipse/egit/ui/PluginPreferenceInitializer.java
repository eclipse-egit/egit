/*******************************************************************************
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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Plugin extension point to initialize the plugin runtime preferences.
 */
public class PluginPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * Calls super constructor.
	 */
	public PluginPreferenceInitializer() {
		super();
	}

	/**
	 * This method initializes the plugin preferences with default values.
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int[] w;

		store.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP, true);
		store.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL, true);
		store.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT, true);
		store.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_TOOLTIPS, false);

		store.setDefault(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS, true);
		store.setDefault(UIPreferences.DECORATOR_RECURSIVE_LIMIT,
				Integer.MAX_VALUE);
		store.setDefault(UIPreferences.DECORATOR_FILETEXT_DECORATION,
				GitLightweightDecorator.DecorationHelper.FILE_FORMAT_DEFAULT);
		store.setDefault(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION,
				GitLightweightDecorator.DecorationHelper.FOLDER_FORMAT_DEFAULT);
		store.setDefault(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION,
				GitLightweightDecorator.DecorationHelper.PROJECT_FORMAT_DEFAULT);
		store.setDefault(UIPreferences.DECORATOR_SHOW_TRACKED_ICON, true);
		store.setDefault(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON, true);
		store.setDefault(UIPreferences.DECORATOR_SHOW_STAGED_ICON, true);
		store.setDefault(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON, true);
		store.setDefault(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON, true);

		w = new int[] { 500, 500 };
		store.setDefault(UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT, UIPreferences.intArrayToString(w));
		w = new int[] { 700, 300 };
		store.setDefault(UIPreferences.RESOURCEHISTORY_REV_SPLIT, UIPreferences.intArrayToString(w));

		store.setDefault(UIPreferences.FINDTOOLBAR_IGNORE_CASE, true);
		store.setDefault(UIPreferences.FINDTOOLBAR_FIND_IN, 2);

		store.setDefault(UIPreferences.REFESH_ON_INDEX_CHANGE, true);
		store.setDefault(UIPreferences.REFESH_ONLY_WHEN_ACTIVE, true);
	}

}
