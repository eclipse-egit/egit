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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.diffmergetool.DiffTools;
import org.eclipse.jgit.internal.diffmergetool.MergeTools;
import org.eclipse.jgit.internal.diffmergetool.ToolException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
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
			toolName = DiffMergeSettings.readExternalToolFromGitConfig(
					c -> getDiffToolFromGitConfig(c), repository,
					UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG);
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
			toolName = DiffMergeSettings
					.readExternalToolFromGitConfig(
							c -> getMergeToolFromGitConfig(c), repository,
							UIPreferences.MERGE_TOOL_FROM_GIT_CONFIG);
		}

		if (mergeToolMode == MergeToolMode.EXTERNAL) {
			// check Eclipse preferences
			toolName = Optional.of(getMergeToolName());
		}

		return toolName;
	}

	/**
	 * Checks the configuration of the specified repository, for a configured
	 * external tool. Stores a non-empty preference value, if a configuration is
	 * found.
	 *
	 * @param readConfiguredExternalTool
	 *            retrieves a configured external tool from a git config
	 * @param repository
	 *            the config of this repository will be read
	 * @param preferenceName
	 *            the preference to update after reading the git config value
	 * @return the selected external tool name. If the value is not present,
	 *         internal eclipse tool should be used
	 */
	private static Optional<String> readExternalToolFromGitConfig(
			Function<StoredConfig, String> readConfiguredExternalTool,
			Repository repository, String preferenceName) {
		StoredConfig repoConfig = repository.getConfig();
		String externalTool = readConfiguredExternalTool.apply(repoConfig);
		if (externalTool != null) {
			updateDefaultExternalToolPreference(externalTool, preferenceName);
		}
		return Optional.ofNullable(getStore().getString(preferenceName));
	}

	private static void updateDefaultExternalToolPreference(String externalTool,
			String preferenceName) {
		if (externalTool != null) {
			getStore().setValue(preferenceName, externalTool);
		} else {
			getStore().setValue(preferenceName, ""); //$NON-NLS-1$
		}
	}

	/**
	 * @param config
	 *            to read from
	 * @return provides a name of a custom diff tool defined in the git
	 *         configuration
	 */
	private static String getDiffToolFromGitConfig(StoredConfig config) {
		DiffTools diffToolManager = new DiffTools(config);
		return diffToolManager.getDefaultToolName(false);
	}

	/**
	 * @param config
	 *            to read from
	 * @return provides a name of a custom merge tool defined in the git
	 *         configuration
	 */
	private static String getMergeToolFromGitConfig(StoredConfig config) {
		MergeTools mergeToolManager = new MergeTools(config);
		return mergeToolManager.getDefaultToolName(false);
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

	/**
	 * @param filePath
	 *            The file for which to retrieve the external diff tool command.
	 * @return The external diff tool command, as specified by product
	 *         customization, for the file extension of the specified file path.
	 */
	@Nullable
	public static String getDiffToolCommandFromPreferences(String filePath) {
		DiffToolMode diffToolMode = getDiffToolMode();
		if (diffToolMode == DiffToolMode.EXTERNAL_FOR_TYPE) {
			String fileExtension = getFileExtension(filePath);
			if (!StringUtils.isEmptyOrNull(fileExtension)) {
				String preference = getExternalDiffToolPreference();
				String[] tools = preference.split(","); //$NON-NLS-1$
				for (int i = 0; i < tools.length; i += 2) {
					String extension = tools[i].trim();
					String command = tools[i + 1].trim();
					if (Objects.equals(extension, fileExtension)) {
						return command;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return The external diff tools configured by product customization. An
	 *         empty string when the preference is not specified.
	 *
	 * @see UIPreferences#EXTERNAL_DIFF_TOOL_FOR_EXTENSION
	 */
	public static String getExternalDiffToolPreference() {
		String preference = Platform.getPreferencesService().getString(
				Activator.PLUGIN_ID,
				UIPreferences.EXTERNAL_DIFF_TOOL_FOR_EXTENSION,
				"", //$NON-NLS-1$
				null);
		return preference;
	}

	/**
	 *
	 * @param path
	 *            The file path.
	 * @return Returns the file extension of the specified file path. Empty
	 *         string if the path has no extension.
	 */
	public static String getFileExtension(String path) {
		int index = path.lastIndexOf('.');
		if (index == -1) {
			return ""; //$NON-NLS-1$
		}
		if (index == (path.length() - 1)) {
			return ""; //$NON-NLS-1$
		}
		return path.substring(index + 1);
	}
}
