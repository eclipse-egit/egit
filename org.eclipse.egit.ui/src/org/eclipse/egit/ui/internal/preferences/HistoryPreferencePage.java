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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for the History view
 */
public class HistoryPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * The default constructor
	 */
	public HistoryPreferencePage() {
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
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE,
				UIText.ResourceHistory_toggleRelativeDate,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES,
				UIText.HistoryPreferencePage_toggleAllBranches, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS,
				UIText.HistoryPreferencePage_toggleAdditionalRefs, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_NOTES,
				UIText.ResourceHistory_toggleShowNotes, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES,
				UIText.GitHistoryPage_FollowRenames, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
				UIText.ResourceHistory_toggleCommentWrap,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT,
				UIText.ResourceHistory_toggleRevComment, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL,
				UIText.ResourceHistory_toggleRevDetail, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.HISTORY_SHOW_TAG_SEQUENCE,
				UIText.ResourceHistory_ShowTagSequence, getFieldEditorParent()));
		addField(new IntegerFieldEditor(UIPreferences.HISTORY_MAX_NUM_COMMITS,
				UIText.ResourceHistory_MaxNumCommitsInList,
				getFieldEditorParent()));
		addField(new IntegerFieldEditor(UIPreferences.HISTORY_MAX_TAG_LENGTH,
				UIText.HistoryPreferencePage_MaxTagLength,
				getFieldEditorParent()));
		addField(new IntegerFieldEditor(
				UIPreferences.HISTORY_MAX_BRANCH_LENGTH,
				UIText.HistoryPreferencePage_MaxBranchLength,
				getFieldEditorParent()));
	}
}
