/*******************************************************************************
 * Copyright (C) 2010, 2013 Robin Stocker <robin@nibor.org> and others.
 * Copyright (C) 2015 SAP SE (Christian Georgi <christian.georgi@sap.com>)
 * Copyright (C) 2016, 2022 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.PluginPreferenceInitializer;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preferences for committing with commit dialog/staging view. */
public class CommittingPreferencePage extends DoublePreferencesPreferencePage
		implements IWorkbenchPreferencePage {

	private final static String[][] GPG_SIGNER_NAMES_AND_VALUES = new String[2][2];

	static {
		GPG_SIGNER_NAMES_AND_VALUES[0][0] = UIText.CommittingPreferencePage_gpgSignerBouncyCastleLabel;
		GPG_SIGNER_NAMES_AND_VALUES[0][1] = "bc"; //$NON-NLS-1$
		GPG_SIGNER_NAMES_AND_VALUES[1][0] = UIText.CommittingPreferencePage_gpgSignerGpgLabel;
		GPG_SIGNER_NAMES_AND_VALUES[1][1] = "gpg"; //$NON-NLS-1$
	}

	private BooleanFieldEditor useStagingView;

	private BooleanFieldEditor autoStage;

	private Button warnCheckbox;

	private Group buildProblemsGroup;

	private ComboFieldEditor warnCombo;

	private Button blockCheckbox;

	private ComboFieldEditor blockCombo;

	private Group generalGroup;

	private ComboFieldEditor gpgSigner;

	private FullWidthFileFieldEditor gpgExecutable;

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
	protected IPreferenceStore doGetSecondaryPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE,
				org.eclipse.egit.core.Activator.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		Composite main = getFieldEditorParent();

		generalGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		generalGroup.setText(UIText.CommittingPreferencePage_general);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(generalGroup);

		useStagingView = new BooleanFieldEditor(
				UIPreferences.ALWAYS_USE_STAGING_VIEW,
				UIText.CommittingPreferencePage_AlwaysUseStagingView,
				generalGroup);
		addField(useStagingView);

		autoStage = new BooleanFieldEditor(UIPreferences.AUTO_STAGE_ON_COMMIT,
				UIText.CommittingPreferencePage_AutoStageOnCommit,
				generalGroup);
		GridDataFactory.fillDefaults().indent(UIUtils.getControlIndent(), 0)
				.applyTo(autoStage.getDescriptionControl(generalGroup));
		addField(autoStage);
		autoStage.setEnabled(getPreferenceStore()
				.getBoolean(UIPreferences.ALWAYS_USE_STAGING_VIEW),
				generalGroup);

		BooleanFieldEditor includeUntracked = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
				UIText.CommittingPreferencePage_includeUntrackedFiles,
				generalGroup);
		includeUntracked.getDescriptionControl(generalGroup).setToolTipText(
				UIText.CommittingPreferencePage_includeUntrackedFilesTooltip);
		addField(includeUntracked);

		BooleanFieldEditor autoStageDeletion = new BooleanFieldEditor(
				GitCorePreferences.core_autoStageDeletion,
				UIText.CommittingPreferencePage_autoStageDeletion,
				generalGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		addField(autoStageDeletion);

		BooleanFieldEditor autoStageMoves = new BooleanFieldEditor(
				GitCorePreferences.core_autoStageMoves,
				UIText.CommittingPreferencePage_autoStageMoves, generalGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		addField(autoStageMoves);

		IntegerFieldEditor historySize = new IntegerFieldEditor(
				UIPreferences.COMMIT_DIALOG_HISTORY_SIZE,
				UIText.CommittingPreferencePage_commitMessageHistory,
				generalGroup);
		addField(historySize);

		gpgSigner = new ComboFieldEditor(GitCorePreferences.core_gpgSigner,
				UIText.CommittingPreferencePage_gpgSignerLabel,
				GPG_SIGNER_NAMES_AND_VALUES, generalGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		addField(gpgSigner);
		gpgExecutable = new FullWidthFileFieldEditor(
				GitCorePreferences.core_gpgExecutable,
				UIText.CommittingPreferencePage_gpgExecutableLabel, true,
				generalGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}

			@Override
			protected boolean doCheckState() {
				Text text = getTextControl();
				if (text != null) {
					String value = text.getText().trim();
					if (!value.isEmpty()) {
						try {
							// Super class resolves symlinks.
							if (!Files.isExecutable(Paths.get(value))) {
								setErrorMessage(
										UIText.CommittingPreferencePage_gpgExecutableNotExecutable);
								return false;
							}
						} catch (InvalidPathException e) {
							setErrorMessage(
									UIText.CommittingPreferencePage_gpgExecutableInvalid);
							return false;
						}
					}
				}
				return super.doCheckState();
			}
		};
		gpgExecutable.indent(UIUtils.getControlIndent(), 0);
		addField(gpgExecutable);
		gpgExecutable.getLabelControl(generalGroup).setToolTipText(
				UIText.CommittingPreferencePage_gpgExecutableTooltip);

		updateMargins(generalGroup);

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
		gpgExecutable.setEnabled("gpg".equals(getSecondaryPreferenceStore() //$NON-NLS-1$
				.getString(GitCorePreferences.core_gpgSigner)), generalGroup);
	}

	@Override
	protected void initialize() {
		super.initialize();
		useStagingView.setPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (FieldEditor.VALUE.equals(event.getProperty())) {
					autoStage.setEnabled(
							((Boolean) event.getNewValue()).booleanValue(),
							generalGroup);
				}
			}
		});
		gpgSigner.setPropertyChangeListener(event -> {
			if (FieldEditor.VALUE.equals(event.getProperty())) {
				gpgExecutable.setEnabled("gpg".equals(event.getNewValue()), //$NON-NLS-1$
						generalGroup);
			}
		});
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		// We don't get property changed events when the default values are
		// restored...
		autoStage.setEnabled(
				getPreferenceStore().getDefaultBoolean(
						UIPreferences.ALWAYS_USE_STAGING_VIEW),
				generalGroup);
		gpgExecutable.setEnabled(
				"gpg".equals(getSecondaryPreferenceStore() //$NON-NLS-1$
						.getDefaultString(GitCorePreferences.core_gpgSigner)),
				generalGroup);
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
		if (selection) {
			warnCheckbox.setSelection(true);
			warnCheckbox.setEnabled(false);
		} else {
			warnCheckbox.setEnabled(true);
		}
	}

	private void handleWarnCheckboxSelection(boolean selection) {
		warnCombo.setEnabled(selection, buildProblemsGroup);
		blockCheckbox.setEnabled(selection);
		blockCombo.setEnabled(selection, buildProblemsGroup);
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(UIPreferences.WARN_BEFORE_COMMITTING,
				warnCheckbox.getSelection());
		getPreferenceStore().setValue(UIPreferences.BLOCK_COMMIT,
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
