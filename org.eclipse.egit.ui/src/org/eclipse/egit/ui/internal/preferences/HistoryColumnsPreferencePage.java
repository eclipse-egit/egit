/*******************************************************************************
 * Copyright (C) 2018 Thomas Wolf <thomas.wolf@paranor.ch>
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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preferences defining which columns to show by default in the history view.
 */
public class HistoryColumnsPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/** Creates a new instance. */
	public HistoryColumnsPreferencePage() {
		super(GRID);
		setTitle(UIText.HistoryColumnsPreferencePage_title);
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
		final Composite pane = getFieldEditorParent();
		Label explanation = new Label(pane, SWT.LEFT | SWT.WRAP);
		explanation.setText(UIText.HistoryColumnsPreferencePage_explanation);
		GridData layout = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		layout.widthHint = 0; // Needed to get wrapping to work
		explanation.setLayoutData(layout);
		addField(new BooleanFieldEditor(UIPreferences.HISTORY_COLUMN_ID,
				UIText.CommitGraphTable_CommitId, pane));
		addField(new BooleanFieldEditor(UIPreferences.HISTORY_COLUMN_AUTHOR,
				UIText.HistoryPage_authorColumn, pane));
		addField(
				new BooleanFieldEditor(UIPreferences.HISTORY_COLUMN_AUTHOR_DATE,
						UIText.HistoryPage_authorDateColumn, pane));
		addField(new BooleanFieldEditor(UIPreferences.HISTORY_COLUMN_COMMITTER,
				UIText.CommitGraphTable_Committer, pane));
		addField(new BooleanFieldEditor(
				UIPreferences.HISTORY_COLUMN_COMMITTER_DATE,
				UIText.CommitGraphTable_committerDateColumn, pane));
	}

}
