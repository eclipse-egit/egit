/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preference page for confirmation dialog preferences */
public class DialogsPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	private final static int GROUP_SPAN = 3;

	private static final String[][] COMMIT_WITH_WARNINGS_SCOPE = new String[3][2];

	private static final String[][] COMMIT_WITH_WARNINGS_ACTION = new String[3][2];

	private static final String[][] COMMIT_WITH_ERRORS_SCOPE = new String[3][2];

	private static final String[][] COMMIT_WITH_ERRORS_ACTION = new String[3][2];

	static {
		COMMIT_WITH_WARNINGS_SCOPE[0][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Scope1_Label;
		COMMIT_WITH_WARNINGS_SCOPE[0][1] = "0";//$NON-NLS-1$
		COMMIT_WITH_WARNINGS_SCOPE[1][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Scope2_Label;
		COMMIT_WITH_WARNINGS_SCOPE[1][1] = "1";//$NON-NLS-1$
		COMMIT_WITH_WARNINGS_SCOPE[2][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Scope3_Label;
		COMMIT_WITH_WARNINGS_SCOPE[2][1] = "2"; //$NON-NLS-1$

		COMMIT_WITH_WARNINGS_ACTION[0][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Action1_Label;
		COMMIT_WITH_WARNINGS_ACTION[0][1] = "0";//$NON-NLS-1$
		COMMIT_WITH_WARNINGS_ACTION[1][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Action2_Label;
		COMMIT_WITH_WARNINGS_ACTION[1][1] = "1";//$NON-NLS-1$
		COMMIT_WITH_WARNINGS_ACTION[2][0] = UIText.DialogsPreferencePage_WhenCommittingWarnings_Action3_Label;
		COMMIT_WITH_WARNINGS_ACTION[2][1] = "2";//$NON-NLS-1$

		COMMIT_WITH_ERRORS_SCOPE[0][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Scope1_Label;
		COMMIT_WITH_ERRORS_SCOPE[0][1] = "0";//$NON-NLS-1$
		COMMIT_WITH_ERRORS_SCOPE[1][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Scope2_Label;
		COMMIT_WITH_ERRORS_SCOPE[1][1] = "1";//$NON-NLS-1$
		COMMIT_WITH_ERRORS_SCOPE[2][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Scope3_Label;
		COMMIT_WITH_ERRORS_SCOPE[2][1] = "2"; //$NON-NLS-1$

		COMMIT_WITH_ERRORS_ACTION[0][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Action1_Label;
		COMMIT_WITH_ERRORS_ACTION[0][1] = "0";//$NON-NLS-1$
		COMMIT_WITH_ERRORS_ACTION[1][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Action2_Label;
		COMMIT_WITH_ERRORS_ACTION[1][1] = "1";//$NON-NLS-1$
		COMMIT_WITH_ERRORS_ACTION[2][0] = UIText.DialogsPreferencePage_WhenCommittingErrors_Action3_Label;
		COMMIT_WITH_ERRORS_ACTION[2][1] = "2";//$NON-NLS-1$
	}

	/**
	 * The default constructor
	 */
	public DialogsPreferencePage() {
		super(GRID);
	}

	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		Composite main = getFieldEditorParent();

		Group confirmDialogsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(confirmDialogsGroup);
		confirmDialogsGroup
				.setText(UIText.DialogsPreferencePage_HideConfirmationGroupHeader);

		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(confirmDialogsGroup);
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_INITIAL_CONFIG_DIALOG,
				UIText.DialogsPreferencePage_ShowInitialConfigCheckbox,
				confirmDialogsGroup));

		addField(new BooleanFieldEditor(UIPreferences.SHOW_REBASE_CONFIRM,
				UIText.DialogsPreferencePage_RebaseCheckbox, confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_DETACHED_HEAD_WARNING,
				UIText.DialogsPreferencePage_DetachedHeadCombo, confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.CLONE_WIZARD_SHOW_DETAILED_FAILURE_DIALOG,
				UIText.DialogsPreferencePage_ShowCloneFailedDialog,
				confirmDialogsGroup));
		updateMargins(confirmDialogsGroup);

		Group commitWarningsErrorsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(commitWarningsErrorsGroup);
		commitWarningsErrorsGroup
				.setText(UIText.DialogsPreferencePage_WarningsErrorsWhileCommitting);

		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(commitWarningsErrorsGroup);
		addField(new ComboFieldEditor(UIPreferences.COMMIT_WITH_WARNINGS_SCOPE,
				UIText.DialogsPreferencePage_WhenCommittingWarnings_Scope,
				COMMIT_WITH_WARNINGS_SCOPE, commitWarningsErrorsGroup));
		addField(new ComboFieldEditor(
				UIPreferences.COMMIT_WITH_WARNINGS_ACTION,
				UIText.DialogsPreferencePage_WhenCommittingWarnings_Action,
				COMMIT_WITH_WARNINGS_ACTION, commitWarningsErrorsGroup));
		addField(new ComboFieldEditor(UIPreferences.COMMIT_WITH_ERRORS_SCOPE,
				UIText.DialogsPreferencePage_WhenCommittingErrors_Scope,
				COMMIT_WITH_ERRORS_SCOPE, commitWarningsErrorsGroup));
		addField(new ComboFieldEditor(UIPreferences.COMMIT_WITH_ERRORS_ACTION,
				UIText.DialogsPreferencePage_WhenCommittingErrors_Action,
				COMMIT_WITH_ERRORS_ACTION, commitWarningsErrorsGroup));
		updateMargins(commitWarningsErrorsGroup);

		Group warningsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(warningsGroup);
		warningsGroup
				.setText(UIText.DialogsPreferencePage_HideWarningGroupHeader);

		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(warningsGroup);
		addField(new BooleanFieldEditor(UIPreferences.SHOW_HOME_DIR_WARNING,
				UIText.DialogsPreferencePage_HomeDirWarning, warningsGroup));
		addField(new BooleanFieldEditor(UIPreferences.SHOW_GIT_PREFIX_WARNING,
				UIText.DialogsPreferencePage_GitPrefixWarning, warningsGroup));
		updateMargins(warningsGroup);
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
