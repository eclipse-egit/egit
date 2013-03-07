/*******************************************************************************
 * Copyright (C) 2010, Robin Stocker <robin@nibor.org>
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
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences for commit dialog. */
public class CommitDialogPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/** */
	public CommitDialogPreferencePage() {
		super(GRID);
		setTitle(UIText.CommitDialogPreferencePage_title);
	}

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

		Group formattingGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		formattingGroup.setText(UIText.CommitDialogPreferencePage_formatting);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(formattingGroup);

		BooleanFieldEditor hardWrap = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_HARD_WRAP_MESSAGE,
				UIText.CommitDialogPreferencePage_hardWrapMessage, formattingGroup);
		hardWrap.getDescriptionControl(formattingGroup).setToolTipText(
				UIText.CommitDialogPreferencePage_hardWrapMessageTooltip);
		addField(hardWrap);
		updateMargins(formattingGroup);

		Group footersGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		footersGroup.setText(UIText.CommitDialogPreferencePage_footers);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(footersGroup);

		BooleanFieldEditor signedOffBy = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY,
				UIText.CommitDialogPreferencePage_signedOffBy,
				footersGroup);
		signedOffBy
				.getDescriptionControl(footersGroup)
				.setToolTipText(
						UIText.CommitDialogPreferencePage_signedOffByTooltip);
		addField(signedOffBy);
		updateMargins(footersGroup);

		IntegerFieldEditor historySize = new IntegerFieldEditor(
				UIPreferences.COMMIT_DIALOG_HISTORY_SIZE,
				UIText.CommitDialogPreferencePage_commitMessageHistory, main);
		addField(historySize);

		BooleanFieldEditor includeUntracked = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
				UIText.CommitDialogPreferencePage_includeUntrackedFiles, main);
		includeUntracked.getDescriptionControl(main).setToolTipText(
				UIText.CommitDialogPreferencePage_includeUntrackedFilesTooltip);
		addField(includeUntracked);
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
