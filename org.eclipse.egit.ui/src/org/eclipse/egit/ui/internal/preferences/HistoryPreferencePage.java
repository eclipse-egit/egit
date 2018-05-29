/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
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
import org.eclipse.jface.layout.GridLayoutFactory;
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
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(main);
		Group showGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		showGroup.setText(UIText.HistoryPreferencePage_ShowGroupLabel);
		// we need a span of 2 to accommodate the field editors
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(showGroup);
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES,
				UIText.HistoryPreferencePage_toggleAllBranches, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS,
				UIText.HistoryPreferencePage_toggleAdditionalRefs, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_NOTES,
				UIText.ResourceHistory_toggleShowNotes, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES,
				UIText.GitHistoryPage_FollowRenames, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT,
				UIText.ResourceHistory_toggleRevComment, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL,
				UIText.ResourceHistory_toggleRevDetail, showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE,
				UIText.ResourceHistory_toggleRelativeDate,
				showGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES,
				UIText.HistoryPreferencePage_toggleEmailAddresses,
				showGroup));
		addField(new IntegerFieldEditor(UIPreferences.HISTORY_MAX_NUM_COMMITS,
				UIText.ResourceHistory_MaxNumCommitsInList,
				showGroup));
		addField(new IntegerFieldEditor(UIPreferences.HISTORY_MAX_TAG_LENGTH,
				UIText.HistoryPreferencePage_MaxTagLength,
				showGroup));
		addField(new IntegerFieldEditor(
				UIPreferences.HISTORY_MAX_BRANCH_LENGTH,
				UIText.HistoryPreferencePage_MaxBranchLength,
				showGroup));
		addField(new IntegerFieldEditor(UIPreferences.HISTORY_MAX_DIFF_LINES,
				UIText.HistoryPreferencePage_MaxDiffLines, showGroup));

		addField(new BooleanFieldEditor(UIPreferences.HISTORY_CUT_AT_START,
				UIText.HistoryPreferencePage_toggleShortenAtStart, showGroup));
		updateMargins(showGroup);
		Group commentGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		// we need a span of 2 to accommodate the field editors
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(commentGroup);
		commentGroup.setText(UIText.HistoryPreferencePage_ShowInRevCommentGroupLabel);
		addField(new BooleanFieldEditor(
				UIPreferences.HISTORY_SHOW_BRANCH_SEQUENCE,
				UIText.ResourceHistory_ShowBranchSequence, commentGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.HISTORY_SHOW_TAG_SEQUENCE,
				UIText.ResourceHistory_ShowTagSequence, commentGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
				UIText.ResourceHistory_toggleCommentWrap,
				commentGroup));
		addField(new BooleanFieldEditor(
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL,
				UIText.ResourceHistory_toggleCommentFill,
				commentGroup));
		updateMargins(commentGroup);
		adjustGridLayout();
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
