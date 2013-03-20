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

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIPreferences;
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
