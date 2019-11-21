/*
 * Copyright (C) 2019, Mykola Zakharchuk <mykola.zakharchuk@advantest.com>
 * Copyright (C) 2019, Andre Bossert <andre.bossert@siemens.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jgit.diffmergetool.DiffToolManager;
import org.eclipse.jgit.diffmergetool.MergeToolManager;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for interacting with external diff/merge tools.
 *
 * Every section of the page provides controls to interrelate with corresponding
 * {@code DiffMergeMode}. Every mode interprets separate instruction pipeline
 * for the execution of the external tool.
 *
 */
public class DiffPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

	private final static IPreferenceStore PREFERENCE_STORE;

	private final static DiffMergePreferencesManager PREF_MANAGER;

	static FileBasedConfig userScopedConfig = null;

	static Set<String> diffToolsList = null;

	static Set<String> mergeToolsList = null;

	@Override
	public void init(IWorkbench workbench) {
		//
	}

	static {
		loadUserScopedConfig();
		PREFERENCE_STORE = Activator.getDefault().getPreferenceStore();
		PREF_MANAGER = new DiffMergePreferencesManager(PREFERENCE_STORE);
	}


	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setFont(parent.getFont());

		GridLayout leftMargin = new GridLayout();
		leftMargin.marginLeft = 5;

		createDiffToolSection(container, leftMargin);
		createMergeToolSection(container, leftMargin);

		return container;
	}

	/**
	 * Generates diff tools section with specified margin for secondary
	 * elements.
	 *
	 * @param container
	 *            parent container
	 * @param leftMargin
	 *            margin for secondary elements
	 *
	 */
	private void createDiffToolSection(Composite container, GridLayout leftMargin) {
		Group diffGroup = new Group(container, SWT.None);
		HashMap<Button, DiffMergeMode> diffControls = new HashMap<>();

		diffGroup.setLayout(new GridLayout());
		diffGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		diffGroup.setBackground(container.getBackground());
		diffGroup.setText(UIText.DiffPreferencePage_DiffToolToUse);

		Text warningText = new Text(diffGroup, SWT.READ_ONLY);
		warningText.setText(UIText.DiffPreferencePage_WarningMessage);
		warningText.setEditable(false);
		Button useInternal = new Button(diffGroup, SWT.RADIO);
		useInternal.setText(UIText.DiffPreferencePage_UseBuiltInEditor);
		diffControls.put(useInternal, DiffToolMode.INTERNAL);
		if (PREF_MANAGER.isActiveMode(DiffToolMode.INTERNAL)
				|| PREF_MANAGER.isActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE)) {
			useInternal.setSelection(true);
		}

		Composite attributesCont = new Composite(diffGroup, SWT.None);
		attributesCont.setLayout(leftMargin);

		Button useExternalForType = new Button(attributesCont, SWT.CHECK);
		useExternalForType.setText(UIText.DiffPreferencePage_UseExternalForType);
		if (PREF_MANAGER.isActiveMode(DiffToolMode.GIT_CONFIG)
				|| PREF_MANAGER.isActiveMode(DiffToolMode.EXTERNAL)) {
			useExternalForType.setEnabled(false);
		}
		diffControls.put(useExternalForType, DiffToolMode.EXTERNAL_FOR_TYPE);
		useInternal.addListener(SWT.Selection, event -> {
			PREF_MANAGER.setActiveMode(DiffToolMode.INTERNAL);
			useExternalForType.setEnabled(true);
		});
		if (PREF_MANAGER.isActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE)) {
			useExternalForType.setSelection(true);
		}
		useExternalForType.addListener(SWT.Selection, event -> {
			if (useExternalForType.getSelection()) {
				PREF_MANAGER.setActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE);
			} else {
				PREF_MANAGER.setActiveMode(DiffToolMode.INTERNAL);
			}
		});
		PREF_MANAGER.addButtonWithCustomReset(useExternalForType, () -> {
			useExternalForType.setEnabled(true);
		});

		Button useGitConfig = new Button(diffGroup, SWT.RADIO);
		useGitConfig.setText(UIText.DiffPreferencePage_UseGitConfig);
		if (PREF_MANAGER.isActiveMode(DiffToolMode.GIT_CONFIG)) {
			useGitConfig.setSelection(true);
		}
		diffControls.put(useGitConfig, DiffToolMode.GIT_CONFIG);
		useGitConfig.addListener(SWT.Selection, event -> {
			if (useGitConfig.getSelection()) {
				PREF_MANAGER.setActiveMode(DiffToolMode.GIT_CONFIG);
				PREF_MANAGER.updateDefaultDiffToolFromGitConfig();
				useExternalForType.setEnabled(false);
			}
		});

		Button useExternal = new Button(diffGroup, SWT.RADIO);
		useExternal.setText(UIText.DiffPreferencePage_UseExternal);
		if (PREF_MANAGER.isActiveMode(DiffToolMode.EXTERNAL)) {
			useExternal.setSelection(true);
		}

		useExternal.addListener(SWT.Selection, event -> {
			if (useExternal.getSelection()) {
				PREF_MANAGER.setActiveMode(DiffToolMode.EXTERNAL);
				useExternalForType.setEnabled(false);
			}
		});
		diffControls.put(useExternal, DiffToolMode.EXTERNAL);

		Composite diffToolCustomCont = new Composite(diffGroup, SWT.None);
		diffToolCustomCont.setLayout(leftMargin);

		Combo diffToolCustom = new Combo(diffToolCustomCont, SWT.READ_ONLY);
		for (String tool : diffToolsList) {
			diffToolCustom.add(tool);
		}
		String defaultCustomDiffTool = PREFERENCE_STORE.getString(UIPreferences.DIFF_TOOL_CUSTOM);
		if (diffToolsList.contains(defaultCustomDiffTool)) {
			diffToolCustom.setText(defaultCustomDiffTool);
		} else {
			diffToolCustom
					.setText(diffToolsList.stream().findFirst().toString());
		}
		if (PREF_MANAGER.isActiveMode(DiffToolMode.EXTERNAL)) {
			diffToolCustom.setEnabled(true);
		} else {
			diffToolCustom.setEnabled(false);
		}
		useExternal.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (useExternal.getSelection()) {
					diffToolCustom.setEnabled(true);
				} else {
					diffToolCustom.setEnabled(false);
				}
			}
		});
		diffToolCustom.addListener(SWT.Selection, event -> {
			PREFERENCE_STORE.setValue(UIPreferences.DIFF_TOOL_CUSTOM, diffToolCustom.getText());
			diffToolCustom.setText(PREFERENCE_STORE.getString(UIPreferences.DIFF_TOOL_CUSTOM));
		});
		PREF_MANAGER.addComboWithCustomReset(diffToolCustom, () -> {
			diffToolCustom.setEnabled(false);
		});

		PREF_MANAGER.bindButtons(diffControls);

		updateMargins(diffGroup);
	}

	/**
	 * * Generates merge tools section with specified margin for secondary
	 * elements.
	 *
	 * @param container
	 *            parent container
	 * @param leftMargin
	 *            margin for secondary elements
	 *
	 */
	private void createMergeToolSection(Composite container, GridLayout leftMargin) {
		Group mergeGroup = new Group(container, SWT.None);
		HashMap<Button, DiffMergeMode> mergeControls = new HashMap<>();
		mergeGroup.setLayout(new GridLayout());
		mergeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		mergeGroup.setBackground(container.getBackground());
		mergeGroup.setText(UIText.DiffPreferencePage_MergeToolSection);

		Composite toolContentSection = new Composite(mergeGroup, SWT.None);
		toolContentSection.setLayout(new GridLayout());

		Label mergeContentLabel = new Label(toolContentSection, SWT.READ_ONLY);
		mergeContentLabel.setText(UIText.DiffPreferencePage_MergeToolContent);

		Button lastHead = new Button(toolContentSection, SWT.RADIO);
		lastHead.setText(UIText.DiffPreferencePage_MergeLastHeadMode);
		if (PREF_MANAGER.isActiveMode(MergeMode.LAST_HEAD)) {
			lastHead.setSelection(true);
		}
		lastHead.addListener(SWT.Selection, event -> PREF_MANAGER.setActiveMode(MergeMode.LAST_HEAD));
		mergeControls.put(lastHead, MergeMode.LAST_HEAD);

		Button promptForTool = new Button(toolContentSection, SWT.RADIO);
		promptForTool.setText(UIText.DiffPreferencePage_MergePromptMode);
		if (PREF_MANAGER.isActiveMode(MergeMode.PROMPT)) {
			promptForTool.setSelection(true);
		}
		promptForTool.addListener(SWT.Selection, event -> PREF_MANAGER.setActiveMode(MergeMode.PROMPT));
		mergeControls.put(promptForTool, MergeMode.PROMPT);

		Button workspacePreMerged = new Button(toolContentSection, SWT.RADIO);
		workspacePreMerged.setText(UIText.DiffPreferencePage_MergeWorkspaceMode);
		if (PREF_MANAGER.isActiveMode(MergeMode.WORKSPACE)) {
			workspacePreMerged.setSelection(true);
		}
		workspacePreMerged.addListener(SWT.Selection, event -> PREF_MANAGER.setActiveMode(MergeMode.WORKSPACE));
		mergeControls.put(workspacePreMerged, MergeMode.WORKSPACE);

		Composite toolToUseSection = new Composite(mergeGroup, SWT.None);
		toolToUseSection.setLayout(new GridLayout());

		Label mergeToolToUseLabel = new Label(toolToUseSection, SWT.READ_ONLY);
		mergeToolToUseLabel.setText(UIText.DiffPreferencePage_MergeToolToUse);

		Button mergeUseEclipseCompare = new Button(toolToUseSection, SWT.RADIO);
		mergeUseEclipseCompare.setText(UIText.DiffPreferencePage_UseBuiltInEditor);
		if (PREF_MANAGER.isActiveMode(MergeToolMode.INTERNAL)) {
			mergeUseEclipseCompare.setSelection(true);
		}
		mergeUseEclipseCompare.addListener(SWT.Selection, event -> PREF_MANAGER.setActiveMode(MergeToolMode.INTERNAL));
		mergeControls.put(mergeUseEclipseCompare, MergeToolMode.INTERNAL);

		Button mergeUseExternalTool = new Button(toolToUseSection, SWT.RADIO);
		mergeUseExternalTool.setText(UIText.DiffPreferencePage_UseExternal);
		if (PREF_MANAGER.isActiveMode(MergeToolMode.EXTERNAL)) {
			mergeUseExternalTool.setSelection(true);
		}
		mergeUseExternalTool.addListener(SWT.Selection, event -> PREF_MANAGER.setActiveMode(MergeToolMode.EXTERNAL));
		mergeControls.put(mergeUseExternalTool, MergeToolMode.EXTERNAL);

		Composite mergeToolCustomCont = new Composite(toolToUseSection, SWT.None);
		mergeToolCustomCont.setLayout(leftMargin);

		Combo mergeToolCustom = new Combo(mergeToolCustomCont, SWT.READ_ONLY);
		Set<String> customMergeTools = getCustomMergeToolsSet();
		for (String tool : customMergeTools) {
			mergeToolCustom.add(tool);
		}
		String defaultCustomMergeTool = PREFERENCE_STORE.getString(UIPreferences.MERGE_TOOL_CUSTOM);
		if (customMergeTools.contains(defaultCustomMergeTool)) {
			mergeToolCustom.setText(defaultCustomMergeTool);
		} else {
			mergeToolCustom.setText(customMergeTools.stream().findFirst().toString());
		}
		if (PREF_MANAGER.isActiveMode(MergeToolMode.INTERNAL)) {
			mergeToolCustom.setEnabled(false);
		} else {
			mergeToolCustom.setEnabled(true);
		}
		mergeUseExternalTool.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (mergeUseExternalTool.getSelection()) {
					mergeToolCustom.setEnabled(true);
				} else {
					mergeToolCustom.setEnabled(false);
				}
			}
		});
		mergeToolCustom.addListener(SWT.Selection, event -> {
			PREFERENCE_STORE.setValue(UIPreferences.MERGE_TOOL_CUSTOM, mergeToolCustom.getText());
			mergeToolCustom.setText(PREFERENCE_STORE.getString(UIPreferences.MERGE_TOOL_CUSTOM));
		});

		Button addExternalMergedFile = new Button(mergeToolCustomCont, SWT.CHECK);
		addExternalMergedFile.setText(UIText.DiffPreferencePage_MergeAddExternalMergedFile);
		if (PREFERENCE_STORE.getBoolean(UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX) == true) {
			addExternalMergedFile.setSelection(true);
		}
		addExternalMergedFile.addListener(SWT.Selection,
				event -> PREFERENCE_STORE.setValue(UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX, true));
		PREF_MANAGER.addComboWithCustomReset(mergeToolCustom, () -> {
			mergeToolCustom.setEnabled(false);
		});

		PREF_MANAGER.bindButtons(mergeControls);

		updateMargins(mergeGroup);
	}

	/**
	 * Loads user-specific git configuration.
	 */
	private static void loadUserScopedConfig() {
		if (getUserScopedConfig() == null || getUserScopedConfig().isOutdated()) {
			userScopedConfig = SystemReader.getInstance().openUserConfig(null, FS.DETECTED);
			try {
				getUserScopedConfig().load();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			} catch (ConfigInvalidException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			diffToolsList = getAvailableDiffToolsSet();
			mergeToolsList = getCustomMergeToolsSet();
		}
	}

	/**
	 * @return provides a set of diff tools available in the system
	 */
	private static Set<String> getAvailableDiffToolsSet() {
		DiffToolManager diffToolManager = new DiffToolManager(FS.DETECTED, null,
				null, DiffPreferencePage.getUserScopedConfig());
		return diffToolManager.getPredefinedAvailableTools();
	}

	/**
	 * @return provides a name of a custom diff tool defined tin git
	 *         configuration
	 */
	private static String getCustomDiffToolFromGitConfig() {
		DiffToolManager diffToolManager = new DiffToolManager(FS.DETECTED, null,
				null, DiffPreferencePage.getUserScopedConfig());
		return diffToolManager.getDefaultToolName(false);
	}

	/**
	 * @return provides a set of predefined merge tools
	 */
	private static Set<String> getCustomMergeToolsSet() {
		MergeToolManager mergeToolManager = new MergeToolManager(FS.DETECTED,
				null, null, DiffPreferencePage.getUserScopedConfig());
		return mergeToolManager.getAllToolNames();
	}

	/**
	 *
	 * @return true if external merge tool and false if internal Eclipse compare
	 *         editor should be used
	 */
	public static boolean useExternalMergeTool() {
		return PREF_MANAGER.useExternalMergeTool();
	}

	/**
	 * @return the selected diff tool name
	 */
	public static Optional<String> getDiffToolName() {
		return PREF_MANAGER.getDiffToolName();
	}

	/**
	 * @return true if external diff tool and false if internal compare should be
	 *         used
	 */
	public static DiffToolMode getDiffToolMode() {
		return PREF_MANAGER.getDiffToolMode();
	}

	/**
	 * @return the selected merge tool name
	 */
	public static String getMergeToolName() {
		return PREF_MANAGER.getMergeToolName();
	}

	/**
	 * @return the selected diff tool name or null (=default)
	 */
	public static Optional<String> getDiffToolFromGitCongig() {
		return PREF_MANAGER.getDiffToolFromGitCongig();
	}

	/**
	 * @return user configuration from the root pref. page
	 */
	private static FileBasedConfig getUserScopedConfig() {
		return userScopedConfig;
	}

	/**
	 * Manager intended to provide helper methods for working with preferences
	 * in DiffMergePreferencePage.
	 */
	static class DiffMergePreferencesManager {
		private IPreferenceStore prefStore;

		private Map<Button, DiffMergeMode> buttonsStore = new LinkedHashMap<>();

		private Map<Button, Runnable> customResetButtons = new LinkedHashMap<>();

		private Map<Combo, Runnable> customResetCombos = new LinkedHashMap<>();

		public DiffMergePreferencesManager(IPreferenceStore store) {
			this.prefStore = store;

		}

		/**
		 * Links buttons to the corresponding {@code DiffMergeMode}.
		 *
		 * @param bindings
		 *            bindings to save
		 *
		 */
		private void bindButtons(Map<Button, DiffMergeMode> bindings) {
			buttonsStore.putAll(bindings);
		}

		/**
		 * Clears saved bindings. Should be triggered before {@code dispose()}.
		 */
		private void emptyControlsStore() {
			buttonsStore.clear();
			customResetButtons.clear();
			customResetCombos.clear();
		}

		/**
		 * Deletes selection for every button in the store.
		 */
		private void removeButtonsSelections() {
			for (Entry<Button, DiffMergeMode> control : buttonsStore
					.entrySet()) {
				control.getKey().setSelection(false);
			}
		}

		/**
		 * Sets selection for every button in the store depending on the default
		 * preferences.
		 */
		private void resetButtonsSelections() {
			setControlsSelections(DiffToolMode.class);
			setControlsSelections(MergeToolMode.class);
			setControlsSelections(MergeMode.class);
			resetButtonsWithCustomReset();
			resetCombosWithCustomReset();
		}

		/**
		 * Sets default values for every mode in {@code DiffPreferencePage}.
		 */
		private void resetModes() {
			PREFERENCE_STORE.setToDefault(UIPreferences.DIFF_TOOL_MODE);
			PREFERENCE_STORE.setToDefault(UIPreferences.MERGE_TOOL_MODE);
			PREFERENCE_STORE.setToDefault(UIPreferences.MERGE_MODE);
		}

		/**
		 * Sets selection to the button depending on the corresponding mode id
		 * binded to the button.
		 *
		 * @param modeId
		 *            mode id to set selection to
		 *
		 */
		private void setButtonsSelectionsForMode(int modeId) {
			for (Entry<Button, DiffMergeMode> control : buttonsStore
					.entrySet()) {
				if (control.getValue().getValue() == modeId) {
					control.getKey().setSelection(true);
				}
			}
		}

		/**
		 * Sets control selection depending on the {@code DiffMergeMode} saved
		 * in the {@code UIPreferences}.
		 *
		 * @param mode
		 *            mode to set corresponding value saved in preferences for
		 *
		 */
		public void setControlsSelections(Class<? extends DiffMergeMode> mode) {
			removeButtonsSelections();
			if (mode.isAssignableFrom(DiffToolMode.class)) {
				setButtonsSelectionsForMode(
						prefStore.getInt(UIPreferences.DIFF_TOOL_MODE));
			} else if (mode.isAssignableFrom(MergeToolMode.class)) {
				setButtonsSelectionsForMode(
						prefStore.getInt(UIPreferences.MERGE_TOOL_MODE));
			} else if (mode.isAssignableFrom(MergeMode.class)) {
				setButtonsSelectionsForMode(
						prefStore.getInt(UIPreferences.MERGE_MODE));
			}
		}

		/**
		 * Sets corresponding working mode for external diff/merge functionality.
		 *
		 * @param mode working mode to be set
		 *
		 */
		private void setActiveMode(DiffMergeMode mode) {
			if (mode instanceof DiffToolMode) {
				prefStore.setValue(UIPreferences.DIFF_TOOL_MODE, mode.getValue());
			} else if (mode instanceof MergeToolMode) {
				prefStore.setValue(UIPreferences.MERGE_TOOL_MODE, mode.getValue());
			} else if (mode instanceof MergeMode) {
				prefStore.setValue(UIPreferences.MERGE_MODE, mode.getValue());
			}
		}

		/**
		 * Checks if specified mode is now the active mode.
		 *
		 * @param mode working mode to check if it is active now
		 * @return true if the mode is active
		 */
		private boolean isActiveMode(DiffMergeMode mode) {
			if (mode instanceof DiffToolMode) {
				return mode.getValue() == prefStore.getInt(UIPreferences.DIFF_TOOL_MODE);
			}
			if (mode instanceof MergeMode) {
				return mode.getValue() == prefStore.getInt(UIPreferences.MERGE_MODE);
			}
			if (mode instanceof MergeToolMode) {
				return mode.getValue() == prefStore.getInt(UIPreferences.MERGE_TOOL_MODE);
			}
			return false;
		}

		/**
		 * @return true if external merge tool and false if internal compare should be
		 *         used
		 */
		private boolean useExternalMergeTool() {
			return prefStore.getInt(UIPreferences.MERGE_TOOL_MODE) == MergeToolMode.EXTERNAL.getValue();
		}

		/**
		 * @return the selected merge tool name
		 */
		private String getMergeToolName() {
			return prefStore.getString(UIPreferences.MERGE_TOOL_CUSTOM);
		}

		/**
		 * @return the selected diff tool name
		 */
		private Optional<String> getDiffToolName() {
			return Optional.ofNullable(prefStore.getString(UIPreferences.DIFF_TOOL_CUSTOM));
		}

		/**
		 * @return the selected diff tool name or null (=default)
		 */
		private Optional<String> getDiffToolFromGitCongig() {
			loadUserScopedConfig();
			updateDefaultDiffToolFromGitConfig();
			return Optional.ofNullable(prefStore.getString(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG));
		}

		/**
		 * @return true if external diff tool and false if internal compare should be
		 *         used
		 */
		private DiffToolMode getDiffToolMode() {
			return DiffToolMode.fromInt(prefStore.getInt(UIPreferences.DIFF_TOOL_MODE));
		}

		/**
		 * Updates preferences with the current diff tool.
		 */
		private void updateDefaultDiffToolFromGitConfig() {
			String diffTool = getCustomDiffToolFromGitConfig();
			if (diffTool != null) {
				prefStore.setValue(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG,
						getCustomDiffToolFromGitConfig());
			} else {
				prefStore.setValue(UIPreferences.DIFF_TOOL_FROM_GIT_CONFIG, ""); //$NON-NLS-1$
			}
		}

		/**
		 * Stores a button with a custom reset behavior.
		 *
		 * @param button
		 *            {@code Button} to store
		 * @param resetFunc
		 *            corresponding custom {@code Runnable} to run on reset
		 *
		 */
		private void addButtonWithCustomReset(Button button,
				Runnable resetFunc) {
			customResetButtons.put(button, resetFunc);
		}

		/**
		 * Stores a combo with a custom reset behavior.
		 *
		 * @param combo
		 *            {@code Combo} to store
		 * @param resetFunc
		 *            corresponding custom {@code Runnable} to run on reset
		 *
		 */
		private void addComboWithCustomReset(Combo combo, Runnable resetFunc) {
			customResetCombos.put(combo, resetFunc);
		}

		/**
		 * Triggers corresponding custom reset behavior for every button in the
		 * store.
		 */
		private void resetButtonsWithCustomReset() {
			for (Entry<Button, Runnable> button : customResetButtons
					.entrySet()) {
				button.getValue().run();
			}
		}

		/**
		 * Triggers corresponding custom reset behavior for every combo in the
		 * store.
		 */
		private void resetCombosWithCustomReset() {
			for (Entry<Combo, Runnable> combo : customResetCombos
					.entrySet()) {
				combo.getValue().run();
			}
		}
	}

	private void updateMargins(Group group) {
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}

	@Override
	protected void performDefaults() {
		PREF_MANAGER.resetModes();
		PREF_MANAGER.resetButtonsSelections();
	}

	@Override
	public void dispose() {
		PREF_MANAGER.emptyControlsStore();
		super.dispose();
	}
}
