/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preference page for views preferences */
public class MergePreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

	private final MergeStrategyHelper helper;

	/**
	 * The default constructor
	 */
	public MergePreferencePage() {
		helper = new MergeStrategyHelper(true);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public void init(final IWorkbench workbench) {
		// Do nothing
	}

	@Override
	protected Control createContents(Composite parent) {
		Control c = helper.createContents(parent);
		helper.load();
		return c;
	}

	@Override
	public boolean performOk() {
		// Need to save the core preference store because the
		// PreferenceDialog will only save the store provided
		// by doGetPreferenceStore()
		helper.store();
		helper.save();
		return true;
	}
}
