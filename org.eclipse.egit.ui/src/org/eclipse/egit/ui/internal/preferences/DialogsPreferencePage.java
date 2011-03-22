/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
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

/** Root preference page for the all of our workspace preferences. */
public class DialogsPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	private static String[][] TOGGLE_OPTIONS = new String[][] {
			{ UIText.DialogsPreferencePage_ShowDialog,
					MessageDialogWithToggle.PROMPT },
			{ UIText.DialogsPreferencePage_DontShowDialog,
					MessageDialogWithToggle.ALWAYS } };

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

		Group initialGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		initialGroup.setText(UIText.DialogsPreferencePage_InitialGroupHeader);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(initialGroup);
		addField(new BooleanFieldEditor(
				UIPreferences.SHOW_INITIAL_CONFIG_DIALOG,
				UIText.DialogsPreferencePage_ShowInitialConfigCheckbox,
				initialGroup));
		updateMargins(initialGroup);

		Group confirmGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(confirmGroup);
		confirmGroup
				.setText(UIText.DialogsPreferencePage_HideConfirmationGroupHeader);
		addField(new BooleanFieldEditor(UIPreferences.REBASE_HIDE_CONFIRM,
				UIText.DialogsPreferencePage_RebaseCheckbox, confirmGroup));
		addField(new ComboFieldEditor(UIPreferences.SHOW_DETACHED_HEAD_WARNING,
				UIText.DialogsPreferencePage_DetachedHeadCombo, TOGGLE_OPTIONS,
				confirmGroup));
		addField(new ComboFieldEditor(UIPreferences.SHOW_HOME_DIR_WARNING,
				UIText.DialogsPreferencePage_HomeDirWarning, TOGGLE_OPTIONS,
				confirmGroup));
		updateMargins(confirmGroup);
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
