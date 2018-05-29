/*******************************************************************************
 * Copyright (C) 2015 Andrey Loskutov <loskutov@gmx.de>
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
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences for committing with commit dialog/staging view. */
public class StagingViewPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/** */
	public StagingViewPreferencePage() {
		super(GRID);
		setTitle(UIText.StagingViewPreferencePage_title);
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

		IntegerFieldEditor historySize = new IntegerFieldEditor(
				UIPreferences.STAGING_VIEW_MAX_LIMIT_LIST_MODE,
				UIText.StagingViewPreferencePage_maxLimitListMode, main);
		addField(historySize);
	}


}
