/*******************************************************************************
 * Copyright (C) 2010, 2013 Robin Stocker <robin@nibor.org> and others.
 * Copyright (C) 2015 SAP SE (Christian Georgi <christian.georgi@sap.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.PluginPreferenceInitializer;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences for committing with commit dialog/staging view. */
public class CommittingPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private Button warnCheckbox;

	private Group buildProblemsGroup;

	private ComboFieldEditor warnCombo;

	private Button blockCheckbox;

	private ComboFieldEditor blockCombo;

	/** */
	public CommittingPreferencePage() {
		super(GRID);
		setTitle(UIText.CommittingPreferencePage_title);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		Composite main = getFieldEditorParent();

		BooleanFieldEditor useStagingView = new BooleanFieldEditor(
				UIPreferences.ALWAYS_USE_STAGING_VIEW,
				UIText.CommittingPreferencePage_AlwaysUseStagingView, main);
		addField(useStagingView);

		Group formattingGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		formattingGroup.setText(UIText.CommittingPreferencePage_formatting);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(formattingGroup);

		BooleanFieldEditor hardWrap = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_HARD_WRAP_MESSAGE,
				UIText.CommittingPreferencePage_hardWrapMessage, formattingGroup);
		hardWrap.getDescriptionControl(formattingGroup).setToolTipText(
				UIText.CommittingPreferencePage_hardWrapMessageTooltip);
		addField(hardWrap);

		BooleanFieldEditor secondLineCheck = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_WARN_ABOUT_MESSAGE_SECOND_LINE,
				UIText.CommittingPreferencePage_warnAboutCommitMessageSecondLine,
				formattingGroup);
		addField(secondLineCheck);

		updateMargins(formattingGroup);

		Group footersGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		footersGroup.setText(UIText.CommittingPreferencePage_footers);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(footersGroup);

		BooleanFieldEditor signedOffBy = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY,
				UIText.CommittingPreferencePage_signedOffBy,
				footersGroup);
		signedOffBy
				.getDescriptionControl(footersGroup)
				.setToolTipText(
						UIText.CommittingPreferencePage_signedOffByTooltip);
		addField(signedOffBy);
		updateMargins(footersGroup);

		buildProblemsGroup = createGroup(main, 1);
		buildProblemsGroup.setText(
				UIText.CommittingPreferencePage_WarnBeforeCommittingTitle);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(buildProblemsGroup);

		warnCheckbox = createCheckBox(buildProblemsGroup,
				UIText.CommittingPreferencePage_CheckBeforeCommitting);
		((GridData) warnCheckbox.getLayoutData()).horizontalSpan = 3;
		warnCheckbox.setSelection(doGetPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING));
		warnCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWarnCheckboxSelection(warnCheckbox.getSelection());
			}
		});

		warnCombo = new ComboFieldEditor(UIPreferences.WARN_BEFORE_COMMITTING_LEVEL,
				UIText.CommittingPreferencePage_WarnBeforeCommitting,
				new String[][] {
						{ UIText.CommittingPreferencePage_WarnBlock_Errors,
								PluginPreferenceInitializer.COMMITTING_PREFERENCE_PAGE_WARN_BLOCK_ERRORS },
						{ UIText.CommittingPreferencePage_WarnBlock_WarningsAndErrors,
								PluginPreferenceInitializer.COMMITTING_PREFERENCE_PAGE_WARN_BLOCK_WARNINGS_AND_ERRORS } },
				buildProblemsGroup);
		addField(warnCombo);

		blockCheckbox = createCheckBox(buildProblemsGroup,
				UIText.CommittingPreferencePage_BlockCommit);
		((GridData) blockCheckbox.getLayoutData()).horizontalSpan = 3;
		blockCheckbox.setSelection(
				doGetPreferenceStore().getBoolean(UIPreferences.BLOCK_COMMIT));
		blockCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBlockCheckboxSelection(blockCheckbox.getSelection());
			}
		});

		blockCombo = new ComboFieldEditor(UIPreferences.BLOCK_COMMIT_LEVEL,
				UIText.CommittingPreferencePage_BlockCommitCombo,
				new String[][] {
						{ UIText.CommittingPreferencePage_WarnBlock_Errors,
								PluginPreferenceInitializer.COMMITTING_PREFERENCE_PAGE_WARN_BLOCK_ERRORS },
						{ UIText.CommittingPreferencePage_WarnBlock_WarningsAndErrors,
								PluginPreferenceInitializer.COMMITTING_PREFERENCE_PAGE_WARN_BLOCK_WARNINGS_AND_ERRORS } },
				buildProblemsGroup);
		addField(blockCombo);

		handleWarnCheckboxSelection(warnCheckbox.getSelection());
		handleBlockCheckboxSelection(blockCheckbox.getSelection());
		updateMargins(buildProblemsGroup);

		BooleanFieldEditor includeUntracked = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
				UIText.CommittingPreferencePage_includeUntrackedFiles, main);
		includeUntracked.getDescriptionControl(main).setToolTipText(
				UIText.CommittingPreferencePage_includeUntrackedFilesTooltip);
		addField(includeUntracked);

		IntegerFieldEditor historySize = new IntegerFieldEditor(
				UIPreferences.COMMIT_DIALOG_HISTORY_SIZE,
				UIText.CommittingPreferencePage_commitMessageHistory, main);
		addField(historySize);
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}

	private Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData(GridData.FILL);
		data.verticalAlignment = GridData.CENTER;
		data.horizontalAlignment = GridData.FILL;
		button.setLayoutData(data);
		return button;
	}

	private void handleBlockCheckboxSelection(boolean selection) {
		blockCombo.setEnabled(selection, buildProblemsGroup);
	}

	private void handleWarnCheckboxSelection(boolean selection) {
		warnCombo.setEnabled(selection, buildProblemsGroup);
		blockCheckbox.setEnabled(selection);
		blockCombo.setEnabled(selection, buildProblemsGroup);
	}

	@Override
	public boolean performOk() {
		doGetPreferenceStore().setValue(UIPreferences.WARN_BEFORE_COMMITTING,
				warnCheckbox.getSelection());
		doGetPreferenceStore().setValue(UIPreferences.BLOCK_COMMIT,
				blockCheckbox.getSelection());
		return super.performOk();
	}

	private Group createGroup(Composite parent, int numColumns) {
		Group group = new Group(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		group.setLayout(layout);
		GridData data = new GridData(SWT.FILL);
		data.horizontalIndent = 0;
		data.verticalAlignment = SWT.FILL;
		data.horizontalAlignment = SWT.END;
		data.grabExcessHorizontalSpace = true;
		group.setLayoutData(data);
		return group;
	}
}
