/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2017, 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
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

	/**
	 * The default constructor
	 */
	public DialogsPreferencePage() {
		super(GRID);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
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

		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_INITIAL_CONFIG_DIALOG,
				UIText.DialogsPreferencePage_ShowInitialConfigCheckbox,
				confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_CHECKOUT_CONFIRMATION,
				UIText.DialogsPreferencePage_ShowCheckoutConfirmation, confirmDialogsGroup));

		addField(new BooleanFieldEditor(UIPreferences.SHOW_REBASE_CONFIRM,
				UIText.DialogsPreferencePage_RebaseCheckbox, confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_DETACHED_HEAD_WARNING,
				UIText.DialogsPreferencePage_DetachedHeadCombo, confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_DELETE_REPO_GROUP_WARNING,
				UIText.DialogsPreferencePage_ShowDeleteRepoGroup,
				confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_RUNNING_LAUNCH_ON_CHECKOUT_WARNING,
				UIText.DialogsPreferencePage_RunningLaunchOnCheckout,
				confirmDialogsGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.CLONE_WIZARD_SHOW_DETAILED_FAILURE_DIALOG,
				UIText.DialogsPreferencePage_ShowCloneFailedDialog,
				confirmDialogsGroup));
		addField(new BooleanFieldEditor(UIPreferences.LFS_AUTO_CONFIGURATION,
				UIText.DialogsPreferencePage_autoConfigureLfs,
				confirmDialogsGroup));
		BooleanFieldEditor editor = new BooleanFieldEditor(
				UIPreferences.ALWAYS_SHOW_PUSH_WIZARD_ON_COMMIT,
				UIText.DialogsPreferencePage_alwaysShowPushWizard,
				confirmDialogsGroup);
		addField(editor);
		editor.getDescriptionControl(confirmDialogsGroup).setToolTipText(
				UIText.DialogsPreferencePage_alwaysShowPushWizardTooltip);

		updateMargins(confirmDialogsGroup);

		Group infoGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(infoGroup);
		infoGroup.setText(UIText.DialogsPreferencePage_ShowInfoGroupHeader);

		editor = new BooleanFieldEditor(
				UIPreferences.SHOW_FETCH_POPUP_SUCCESS,
				UIText.DialogsPreferencePage_ShowFetchInfoDialog, infoGroup);
		addField(editor);
		editor.getDescriptionControl(infoGroup)
				.setToolTipText(UIText.DialogsPreferencePage_ShowTooltip);
		editor = new BooleanFieldEditor(UIPreferences.SHOW_PUSH_POPUP_SUCCESS,
				UIText.DialogsPreferencePage_ShowPushInfoDialog, infoGroup);
		addField(editor);
		editor.getDescriptionControl(infoGroup)
				.setToolTipText(UIText.DialogsPreferencePage_ShowTooltip);

		updateMargins(infoGroup);

		Group warningsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(warningsGroup);
		warningsGroup.setText(UIText.DialogsPreferencePage_HideWarningGroupHeader);

		addField(new BooleanFieldEditor(UIPreferences.SHOW_HOME_DIR_WARNING,
				UIText.DialogsPreferencePage_HomeDirWarning, warningsGroup));
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
