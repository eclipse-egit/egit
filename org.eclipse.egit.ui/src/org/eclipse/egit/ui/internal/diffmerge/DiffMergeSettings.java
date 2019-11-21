/*******************************************************************************
 * Copyright (C) 2020, Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.diffmerge;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.diffmergetool.DiffTools;
import org.eclipse.jgit.internal.diffmergetool.MergeTools;
import org.eclipse.jgit.internal.diffmergetool.ToolException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

/**
 * Common tooling for diff / merge settings
 */
public final class DiffMergeSettings {

	/**
	 * @return one of {@link DiffToolMode} values
	 */
	public static DiffToolMode getDiffToolMode() {
		return DiffToolMode
				.fromInt(getStore().getInt(UIPreferences.DIFF_TOOL_MODE));
	}

	/**
	 * @return one of {@link MergeToolMode} values
	 */
	public static MergeToolMode getMergeToolMode() {
		return MergeToolMode
				.fromInt(getStore().getInt(UIPreferences.MERGE_TOOL_MODE));
	}

	/**
	 * @return true if external diff tool and false if internal compare should
	 *         be used
	 */
	public static boolean useInternalDiffTool() {
		return getStore()
				.getInt(UIPreferences.DIFF_TOOL_MODE) == DiffToolMode.INTERNAL
						.getValue();
	}

	/**
	 * @return true if external merge tool and false if internal merge should be
	 *         used
	 */
	public static boolean useInternalMergeTool() {
		return getStore()
				.getInt(UIPreferences.MERGE_TOOL_MODE) == MergeToolMode.INTERNAL
						.getValue();
	}

	/**
	 * @return user selected merge tool name
	 */
	public static String getMergeToolName() {
		return getStore().getString(UIPreferences.MERGE_TOOL_CUSTOM);
	}

	/**
	 * @return user selected diff tool name
	 */
	private static String getDiffToolName() {
		return getStore().getString(UIPreferences.DIFF_TOOL_CUSTOM);
	}

	/**
	 * @return set of diff tools available in the system
	 */
	public static Set<String> getAvailableDiffTools() {
		DiffTools diffToolManager = new DiffTools(loadUserConfig());
		return diffToolManager.getPredefinedAvailableTools();
	}

	/**
	 * @return set of predefined merge tools
	 */
	public static Set<String> getAvailableMergeTools() {
		MergeTools mergeToolManager = new MergeTools(loadUserConfig());
		return mergeToolManager.getPredefinedAvailableTools();
	}

	/**
	 * Provides access to configured diff tool names (configuration via git
	 * attributes, git config or Eclipse preferences).
	 *
	 * @param repository
	 * @param relativeFilePath
	 *            path in repository
	 * @return the selected diff tool name, or empty value if no tool configured
	 */
	public static Optional<String> getDiffToolName(Repository repository,
			String relativeFilePath) {
		DiffToolMode diffToolMode = getDiffToolMode();
		Optional<String> toolName = Optional.empty();
		if (diffToolMode == DiffToolMode.INTERNAL) {
			return toolName;
		}
		if (diffToolMode == DiffToolMode.EXTERNAL_FOR_TYPE
				|| diffToolMode == DiffToolMode.GIT_CONFIG) {
			try {
				// try to read from git attributes
				toolName = new DiffTools(repository)
						.getExternalToolFromAttributes(relativeFilePath);
			} catch (ToolException e) {
				Activator.handleError(
						UIText.CompareUtils_GitConfigurationErrorText, e, true);
			}
		}

		if (!toolName.isPresent() && diffToolMode == DiffToolMode.GIT_CONFIG) {
			// try to read from git config
			toolName = DiffMergeSettings.readDiffToolFromGitConfig();
		}

		if (diffToolMode == DiffToolMode.EXTERNAL) {
			// check Eclipse preferences
			toolName = Optional.of(getDiffToolName());
		}

		return toolName;
	}

	/**
	 * Provides access to configured merge tool names (configuration via git
	 * attributes, git config or Eclipse preferences).
	 *
	 * @param repository
	 * @param relativeFilePath
	 *            path in repository
	 * @return the selected merge tool name, or empty value if no tool
	 *         configured
	 */
	public static Optional<String> getMergeToolName(Repository repository,
			String relativeFilePath) {

		MergeToolMode mergeToolMode = getMergeToolMode();
		Optional<String> toolName = Optional.empty();
		if (mergeToolMode == MergeToolMode.INTERNAL) {
			return toolName;
		}
		if (mergeToolMode == MergeToolMode.EXTERNAL_FOR_TYPE
				|| mergeToolMode == MergeToolMode.GIT_CONFIG) {
			try {
				// try to read from git attributes
				toolName = new MergeTools(repository)
						.getExternalToolFromAttributes(relativeFilePath);
			} catch (ToolException e) {
				Activator.handleError(
						UIText.CompareUtils_GitConfigurationErrorText, e, true);
			}
		}

		if (!toolName.isPresent()
				&& mergeToolMode == MergeToolMode.GIT_CONFIG) {
			// try to read from git config
			toolName = DiffMergeSettings.readDiffToolFromGitConfig();
		}

		if (mergeToolMode == MergeToolMode.EXTERNAL) {
			// check Eclipse preferences
			toolName = Optional.of(getDiffToolName());
		}

		return toolName;
	}

	/**
	 * @return the selected diff tool name. If the value is not present,
	 *         internal eclipse diff should be used
	 */
	private static Optional<String> readDiffToolFromGitConfig() {
		FileBasedConfig config = loadUserConfig();
		updateDefaultDiffToolFromGitConfig(config);
		return Optional.ofNullable(
				getStore().getString(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG));
	}

	/**
	 * Updates preferences with the current diff tool.
	 *
	 * @param config
	 */
	private static void updateDefaultDiffToolFromGitConfig(
			FileBasedConfig config) {
		String diffTool = getCustomDiffToolFromGitConfig(config);
		if (diffTool != null) {
			getStore().setValue(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG,
					diffTool);
		} else {
			getStore().setValue(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG, ""); //$NON-NLS-1$
		}
	}

	/**
	 * @param config
	 *            to read from
	 * @return provides a name of a custom diff tool defined tin git
	 *         configuration
	 */
	private static String getCustomDiffToolFromGitConfig(
			FileBasedConfig config) {
		DiffTools diffToolManager = new DiffTools(config);
		return diffToolManager.getDefaultToolName(false);
	}

	private static FileBasedConfig loadUserConfig() {
		FileBasedConfig config = SystemReader.getInstance().openUserConfig(null,
				FS.DETECTED);
		try {
			config.load();
		} catch (IOException | ConfigInvalidException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		return config;
	}

	private static IPreferenceStore getStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
