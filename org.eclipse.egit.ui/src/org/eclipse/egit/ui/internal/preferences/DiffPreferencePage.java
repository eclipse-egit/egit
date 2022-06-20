/*******************************************************************************
 * Copyright (C) 2020, Mykola Zakharchuk <mykola.zakharchuk@advantest.com>
 * Copyright (C) 2020, Andre Bossert <andre.bossert@siemens.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import static org.eclipse.egit.ui.UIPreferences.DIFF_TOOL_CUSTOM;
import static org.eclipse.egit.ui.UIPreferences.DIFF_TOOL_MODE;
import static org.eclipse.egit.ui.UIPreferences.MERGE_MODE;
import static org.eclipse.egit.ui.UIPreferences.MERGE_TOOL_CUSTOM;
import static org.eclipse.egit.ui.UIPreferences.MERGE_TOOL_MODE;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.diffmerge.DiffMergeMode;
import org.eclipse.egit.ui.internal.diffmerge.DiffMergeSettings;
import org.eclipse.egit.ui.internal.diffmerge.DiffToolMode;
import org.eclipse.egit.ui.internal.diffmerge.MergeMode;
import org.eclipse.egit.ui.internal.diffmerge.MergeToolMode;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.util.StringUtils;
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
public class DiffPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private DiffMergePreferencesManager prefsManager;


	@Override
	public void init(IWorkbench workbench) {
		prefsManager = new DiffMergePreferencesManager(getPreferenceStore());
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		final Composite container = getFieldEditorParent();
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createDiffToolSection(container);
		createMergeToolSection(container);
	}

	private GridLayout createGridWithLeftMergins() {
		GridLayout leftMargin = new GridLayout();
		leftMargin.marginLeft = 5;
		return leftMargin;
	}

	/**
	 * Generates diff tools section with specified margin for secondary
	 * elements.
	 *
	 * @param container
	 *            parent container
	 *
	 */
	private void createDiffToolSection(Composite container) {
		HashMap<Button, DiffMergeMode> diffControls = new HashMap<>();

		Group diffGroup = new Group(container, SWT.None);
		diffGroup.setLayout(new GridLayout());
		diffGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		diffGroup.setBackground(container.getBackground());
		diffGroup.setText(UIText.DiffPreferencePage_DiffToolSection);

		// Warning text

		String externalDiffToolPreference = DiffMergeSettings
				.getExternalDiffToolPreference();
		if (StringUtils.isEmptyOrNull(externalDiffToolPreference)) {
			Text warningText = new Text(diffGroup, SWT.READ_ONLY);
			warningText.setText(UIText.DiffPreferencePage_WarningMessage);
			warningText.setEditable(false);
		}

		Label diffToolToUseLabel = new Label(diffGroup, SWT.READ_ONLY);
		diffToolToUseLabel.setText(UIText.DiffPreferencePage_DiffToolToUse);

		// Tool selection radio buttons

		// First radio with checkbox (internal editor)
		Button useInternal = new Button(diffGroup, SWT.RADIO);
		useInternal.setText(UIText.DiffPreferencePage_UseBuiltInEditor);
		diffControls.put(useInternal, DiffToolMode.INTERNAL);
		if (prefsManager.isActiveMode(DiffToolMode.INTERNAL)
				|| prefsManager.isActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE)) {
					useInternal.setSelection(true);
		}

		// Checkbox for first radio
		Composite attributesCont = new Composite(diffGroup, SWT.None);
		attributesCont.setLayout(createGridWithLeftMergins());


		Button useExternalForType = new Button(attributesCont, SWT.CHECK);
		useExternalForType.setText(UIText.DiffPreferencePage_UseExternalForType);
		if (prefsManager.isActiveMode(DiffToolMode.GIT_CONFIG)
				|| prefsManager.isActiveMode(DiffToolMode.EXTERNAL)) {
			useExternalForType.setEnabled(false);
		}
		diffControls.put(useExternalForType, DiffToolMode.EXTERNAL_FOR_TYPE);
		useInternal.addListener(SWT.Selection, event -> {
			prefsManager.setActiveMode(DiffToolMode.INTERNAL);
			useExternalForType.setEnabled(true);
		});
		if (prefsManager.isActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE)) {
			useExternalForType.setSelection(true);
		}
		useExternalForType.addListener(SWT.Selection, event -> {
			if (useExternalForType.getSelection()) {
				prefsManager.setActiveMode(DiffToolMode.EXTERNAL_FOR_TYPE);
			} else {
				prefsManager.setActiveMode(DiffToolMode.INTERNAL);
			}
		});
		prefsManager.addControlWithCustomReset(useExternalForType,
				() -> useExternalForType.setEnabled(true));

		// Second radio

		Button useGitConfig = new Button(diffGroup, SWT.RADIO);
		useGitConfig.setText(UIText.DiffPreferencePage_UseGitConfig);
		if (prefsManager.isActiveMode(DiffToolMode.GIT_CONFIG)) {
			useGitConfig.setSelection(true);
		}
		diffControls.put(useGitConfig, DiffToolMode.GIT_CONFIG);
		useGitConfig.addListener(SWT.Selection, event -> {
			if (useGitConfig.getSelection()) {
				prefsManager.setActiveMode(DiffToolMode.GIT_CONFIG);
				useExternalForType.setEnabled(false);
			}
		});

		// Third radio + combo

		Button useExternal = new Button(diffGroup, SWT.RADIO);
		useExternal.setText(UIText.DiffPreferencePage_UseExternal);
		if (prefsManager.isActiveMode(DiffToolMode.EXTERNAL)) {
			useExternal.setSelection(true);
		}

		diffControls.put(useExternal, DiffToolMode.EXTERNAL);

		// Custom diff tool combo

		Composite diffToolCustomCont = new Composite(diffGroup, SWT.None);
		diffToolCustomCont.setLayout(createGridWithLeftMergins());

		Combo customDiffCombo = new Combo(diffToolCustomCont, SWT.READ_ONLY);
		Set<String> diffToolsList = DiffMergeSettings.getAvailableDiffTools();

		for (String tool : diffToolsList) {
			customDiffCombo.add(tool);
		}

		useExternal.addListener(SWT.Selection, event -> {
			if (useExternal.getSelection()) {
				prefsManager.setActiveMode(DiffToolMode.EXTERNAL);
				useExternalForType.setEnabled(false);
				prefsManager.setCustomTool(DIFF_TOOL_CUSTOM,
						customDiffCombo.getText());
			}
		});

		IPreferenceStore store = getPreferenceStore();
		String defaultCustomDiffTool = store.getString(DIFF_TOOL_CUSTOM);
		if (diffToolsList.contains(defaultCustomDiffTool)) {
			customDiffCombo.setText(defaultCustomDiffTool);
		} else {
			customDiffCombo
					.setText(diffToolsList.stream().findFirst().orElse("")); //$NON-NLS-1$
		}
		customDiffCombo
				.setEnabled(prefsManager.isActiveMode(DiffToolMode.EXTERNAL));

		customDiffCombo.addListener(SWT.Selection, event -> {
			prefsManager.setCustomTool(DIFF_TOOL_CUSTOM,
					customDiffCombo.getText());
		});

		useExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				customDiffCombo.setEnabled(useExternal.getSelection());
			}
		});
		prefsManager.addControlWithCustomReset(customDiffCombo, () -> {
			customDiffCombo.setEnabled(false);
			customDiffCombo.setText(store.getString(DIFF_TOOL_CUSTOM));
		});

		prefsManager.bindButtons(diffControls);

		updateMargins(diffGroup);
	}

	private void createMergeToolSection(Composite container) {
		HashMap<Button, DiffMergeMode> mergeControls = new HashMap<>();

		Group mergeGroup = new Group(container, SWT.None);
		mergeGroup.setLayout(new GridLayout());
		mergeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		mergeGroup.setBackground(container.getBackground());
		mergeGroup.setText(UIText.DiffPreferencePage_MergeToolSection);

		// Merge content radio buttons

		Composite toolContentSection = new Composite(mergeGroup, SWT.None);
		toolContentSection.setLayout(new GridLayout());

		Label mergeContentLabel = new Label(toolContentSection, SWT.READ_ONLY);
		mergeContentLabel.setText(UIText.DiffPreferencePage_MergeToolContent);

		Button promptForTool = new Button(toolContentSection, SWT.RADIO);
		promptForTool.setText(UIText.DiffPreferencePage_MergePromptMode);
		if (prefsManager.isActiveMode(MergeMode.PROMPT)) {
			promptForTool.setSelection(true);
		}
		promptForTool.addListener(SWT.Selection,
				event -> prefsManager.setActiveMode(MergeMode.PROMPT));
		mergeControls.put(promptForTool, MergeMode.PROMPT);

		Button oursPreMerged = new Button(toolContentSection, SWT.RADIO);
		oursPreMerged
				.setText(UIText.DiffPreferencePage_MergeModePremergedOursMode);
		if (prefsManager.isActiveMode(MergeMode.OURS)) {
			oursPreMerged.setSelection(true);
		}
		oursPreMerged.addListener(SWT.Selection,
				event -> prefsManager.setActiveMode(MergeMode.OURS));
		mergeControls.put(oursPreMerged, MergeMode.OURS);

		Button workspacePreMerged = new Button(toolContentSection, SWT.RADIO);
		workspacePreMerged.setText(UIText.DiffPreferencePage_MergeWorkspaceMode);
		if (prefsManager.isActiveMode(MergeMode.WORKSPACE)) {
			workspacePreMerged.setSelection(true);
		}
		workspacePreMerged.addListener(SWT.Selection,
				event -> prefsManager.setActiveMode(MergeMode.WORKSPACE));
		mergeControls.put(workspacePreMerged, MergeMode.WORKSPACE);

		Button lastHead = new Button(toolContentSection, SWT.RADIO);
		lastHead.setText(UIText.DiffPreferencePage_MergeLastHeadMode);
		if (prefsManager.isActiveMode(MergeMode.LAST_HEAD)) {
			lastHead.setSelection(true);
		}
		lastHead.addListener(SWT.Selection,
				event -> prefsManager.setActiveMode(MergeMode.LAST_HEAD));
		mergeControls.put(lastHead, MergeMode.LAST_HEAD);

		// Merge tool radio buttons

		Composite toolToUseSection = new Composite(mergeGroup, SWT.None);
		toolToUseSection.setLayout(new GridLayout());

		Label mergeToolToUseLabel = new Label(toolToUseSection, SWT.READ_ONLY);
		mergeToolToUseLabel.setText(UIText.DiffPreferencePage_MergeToolToUse);

		// First radio with checkbox (internal editor)

		Button mergeUseEclipseCompare = new Button(toolToUseSection, SWT.RADIO);
		mergeUseEclipseCompare.setText(UIText.DiffPreferencePage_UseBuiltInEditor);
		if (prefsManager.isActiveMode(MergeToolMode.INTERNAL)) {
			mergeUseEclipseCompare.setSelection(true);
		}
		mergeUseEclipseCompare.addListener(SWT.Selection,
				event -> prefsManager.setActiveMode(MergeToolMode.INTERNAL));
		mergeControls.put(mergeUseEclipseCompare, MergeToolMode.INTERNAL);

		Composite attributesCont = new Composite(toolToUseSection, SWT.None);
		attributesCont.setLayout(createGridWithLeftMergins());

		// Checkbox for first radio

		Button useExternalForType = new Button(attributesCont, SWT.CHECK);
		useExternalForType
				.setText(UIText.DiffPreferencePage_UseExternalForType);
		if (prefsManager.isActiveMode(MergeToolMode.GIT_CONFIG)
				|| prefsManager.isActiveMode(MergeToolMode.EXTERNAL)) {
			useExternalForType.setEnabled(false);
		}
		mergeControls.put(useExternalForType, MergeToolMode.EXTERNAL_FOR_TYPE);
		mergeUseEclipseCompare.addListener(SWT.Selection, event -> {
			prefsManager.setActiveMode(MergeToolMode.INTERNAL);
			useExternalForType.setEnabled(true);
		});
		if (prefsManager.isActiveMode(MergeToolMode.EXTERNAL_FOR_TYPE)) {
			useExternalForType.setSelection(true);
		}
		useExternalForType.addListener(SWT.Selection, event -> {
			if (useExternalForType.getSelection()) {
				prefsManager.setActiveMode(MergeToolMode.EXTERNAL_FOR_TYPE);
			} else {
				prefsManager.setActiveMode(MergeToolMode.INTERNAL);
			}
		});
		prefsManager.addControlWithCustomReset(useExternalForType,
				() -> useExternalForType.setEnabled(true));

		// Second radio

		Button useGitConfig = new Button(toolToUseSection, SWT.RADIO);
		useGitConfig.setText(UIText.DiffPreferencePage_UseGitConfig);
		if (prefsManager.isActiveMode(MergeToolMode.GIT_CONFIG)) {
			useGitConfig.setSelection(true);
		}
		mergeControls.put(useGitConfig, MergeToolMode.GIT_CONFIG);
		useGitConfig.addListener(SWT.Selection, event -> {
			if (useGitConfig.getSelection()) {
				prefsManager.setActiveMode(MergeToolMode.GIT_CONFIG);
				useExternalForType.setEnabled(false);
			}
		});

		// Third radio with combo selection

		Button mergeUseExternalTool = new Button(toolToUseSection, SWT.RADIO);
		mergeUseExternalTool.setText(UIText.DiffPreferencePage_UseExternal);
		if (prefsManager.isActiveMode(MergeToolMode.EXTERNAL)) {
			mergeUseExternalTool.setSelection(true);
		}
		mergeControls.put(mergeUseExternalTool, MergeToolMode.EXTERNAL);

		Composite mergeToolCustomCont = new Composite(toolToUseSection, SWT.None);
		mergeToolCustomCont.setLayout(new GridLayout());

		Combo customMergeCombo = new Combo(mergeToolCustomCont, SWT.READ_ONLY);
		Set<String> mergeTools = DiffMergeSettings.getAvailableMergeTools();
		for (String tool : mergeTools) {
			customMergeCombo.add(tool);
		}

		mergeUseExternalTool.addListener(SWT.Selection, event -> {
			prefsManager.setActiveMode(MergeToolMode.EXTERNAL);
			prefsManager.setCustomTool(MERGE_TOOL_CUSTOM,
					customMergeCombo.getText());
		});

		IPreferenceStore store = getPreferenceStore();
		String defaultCustomMergeTool = store.getString(MERGE_TOOL_CUSTOM);
		if (mergeTools.contains(defaultCustomMergeTool)) {
			customMergeCombo.setText(defaultCustomMergeTool);
		} else {
			customMergeCombo
					.setText(mergeTools.stream().findFirst().orElse("")); //$NON-NLS-1$
		}
		if (prefsManager.isActiveMode(MergeToolMode.INTERNAL)) {
			customMergeCombo.setEnabled(false);
		} else {
			customMergeCombo.setEnabled(true);
		}
		mergeUseExternalTool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				customMergeCombo.setEnabled(mergeUseExternalTool.getSelection());
				useExternalForType.setEnabled(false);
			}
		});
		customMergeCombo.addListener(SWT.Selection, event -> {
			prefsManager.setCustomTool(MERGE_TOOL_CUSTOM,
					customMergeCombo.getText());
		});

		prefsManager.addControlWithCustomReset(customMergeCombo, () -> {
			customMergeCombo.setEnabled(false);
			customMergeCombo.setText(store.getString(MERGE_TOOL_CUSTOM));
		});

		addField(new BooleanFieldEditor(
				UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX,
				UIText.DiffPreferencePage_MergeAddExternalMergedFile,
				toolToUseSection));

		prefsManager.bindButtons(mergeControls);

		updateMargins(mergeGroup);
	}


	/**
	 * Manager intended to provide helper methods for working with preferences
	 * in DiffMergePreferencePage.
	 */
	static class DiffMergePreferencesManager {
		private IPreferenceStore store;

		private Map<Button, DiffMergeMode> buttons;

		private Map<Control, Runnable> customResetControls;

		/** Key is the mode, value is selection */
		private Map<String, Integer> modeSelections;

		/** Key is the tool key, value is tool selection */
		private Map<String, String> toolSelections;

		public DiffMergePreferencesManager(IPreferenceStore store) {
			this.store = store;
			buttons = new LinkedHashMap<>();
			customResetControls = new LinkedHashMap<>();
			modeSelections = new LinkedHashMap<>();
			toolSelections = new LinkedHashMap<>();
		}

		/**
		 * Links buttons to the corresponding {@code DiffMergeMode}.
		 *
		 * @param bindings
		 *            bindings to save
		 *
		 */
		private void bindButtons(Map<Button, DiffMergeMode> bindings) {
			buttons.putAll(bindings);
		}

		/**
		 * Clears saved bindings. Should be triggered before {@code dispose()}.
		 */
		private void emptyControlsStore() {
			buttons.clear();
			customResetControls.clear();
		}

		/**
		 * Deletes selection for every button in the store.
		 */
		private void removeButtonsSelections() {
			for (Button button : buttons.keySet()) {
				button.setSelection(false);
			}
		}

		/**
		 * Sets selection for every button in the store depending on the default
		 * preferences.
		 */
		private void resetSelections() {
			removeButtonsSelections();
			setControlsSelections(DiffToolMode.class);
			setControlsSelections(MergeToolMode.class);
			setControlsSelections(MergeMode.class);
			resetControlsWithCustomReset();
		}

		/**
		 * Saves modified values in {@code DiffPreferencePage}.
		 */
		private void saveChanges() {
			for (Entry<String, Integer> entry : modeSelections.entrySet()) {
				store.setValue(entry.getKey(), entry.getValue().intValue());
			}
			for (Entry<String, String> entry : toolSelections.entrySet()) {
				store.setValue(entry.getKey(), entry.getValue());
			}
		}

		/**
		 * Sets default values in {@code DiffPreferencePage}.
		 */
		private void resetChanges() {
			store.setToDefault(DIFF_TOOL_MODE);
			store.setToDefault(MERGE_TOOL_MODE);
			store.setToDefault(MERGE_MODE);
			store.setToDefault(DIFF_TOOL_CUSTOM);
			store.setToDefault(MERGE_TOOL_CUSTOM);
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
			for (Entry<Button, DiffMergeMode> entry : buttons.entrySet()) {
				if (entry.getValue().getValue() == modeId) {
					entry.getKey().setSelection(true);
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
			int modeId = 0;
			if (mode == DiffToolMode.class) {
				modeId = store.getInt(DIFF_TOOL_MODE);
			} else if (mode == MergeToolMode.class) {
				modeId = store.getInt(MERGE_TOOL_MODE);
			} else if (mode == MergeMode.class) {
				modeId = store.getInt(MERGE_MODE);
			}
			setButtonsSelectionsForMode(modeId);
		}

		/**
		 * Sets selected custom external diff/merge tool.
		 *
		 * @param toolId
		 *            either {@link #DIFF_TOOL_CUSTOM} or
		 *            {@link #MERGE_TOOL_CUSTOM}
		 *
		 * @param selection
		 *            tool to be set
		 *
		 */
		private void setCustomTool(String toolId, String selection) {
			toolSelections.put(toolId, selection);
		}

		/**
		 * Sets corresponding working mode for external diff/merge
		 * functionality.
		 *
		 * @param mode
		 *            working mode to be set
		 *
		 */
		private void setActiveMode(DiffMergeMode mode) {
			String modeKey = null;
			if (mode instanceof DiffToolMode) {
				modeKey = DIFF_TOOL_MODE;
			} else if (mode instanceof MergeToolMode) {
				modeKey = MERGE_TOOL_MODE;
			} else if (mode instanceof MergeMode) {
				modeKey = MERGE_MODE;
			}
			if (modeKey != null) {
				modeSelections.put(modeKey, Integer.valueOf(mode.getValue()));
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
				return mode.getValue() == store.getInt(DIFF_TOOL_MODE);
			}
			if (mode instanceof MergeMode) {
				return mode.getValue() == store.getInt(MERGE_MODE);
			}
			if (mode instanceof MergeToolMode) {
				return mode.getValue() == store.getInt(MERGE_TOOL_MODE);
			}
			return false;
		}

		/**
		 * Stores a button with a custom reset behavior.
		 *
		 * @param c
		 *            {@code Control} to store
		 * @param reset
		 *            corresponding custom {@code Runnable} to run on reset
		 *
		 */
		private void addControlWithCustomReset(Control c, Runnable reset) {
			customResetControls.put(c, reset);
		}

		/**
		 * Triggers corresponding custom reset behavior for every combo in the
		 * store.
		 */
		private void resetControlsWithCustomReset() {
			for (Runnable r : customResetControls.values()) {
				r.run();
			}
		}
	}

	private void updateMargins(Group group) {
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}

	@Override
	public boolean performOk() {
		prefsManager.saveChanges();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		prefsManager.resetChanges();
		prefsManager.resetSelections();
		super.performDefaults();
	}

	@Override
	public void dispose() {
		prefsManager.emptyControlsStore();
		super.dispose();
	}
}
