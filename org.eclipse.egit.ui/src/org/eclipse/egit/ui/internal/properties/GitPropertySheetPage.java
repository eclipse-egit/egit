/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.properties;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * A {@link PropertySheetPage} that listens to git date format preference
 * changes and refreshes itself when they change.
 */
public class GitPropertySheetPage extends PropertySheetPage {

	private final IPropertyChangeListener dateFormatListener = event -> {
		String property = event.getProperty();
		if (property == null) {
			return;
		}
		switch (property) {
		case UIPreferences.DATE_FORMAT:
		case UIPreferences.DATE_FORMAT_CHOICE:
			refreshInUiThread();
			break;
		default:
			break;
		}
	};

	private final IPreferenceStore store;

	private volatile boolean disposed;

	/**
	 * Creates a new instance.
	 */
	public GitPropertySheetPage() {
		super();
		store = Activator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(dateFormatListener);
	}

	/**
	 * Refreshes the page in the UI thread.
	 */
	public void refreshInUiThread() {
		getSite().getShell().getDisplay().asyncExec(() -> {
			if (!isDisposed()) {
				refresh();
			}
		});
	}

	/**
	 * Tells whether the page is disposed.
	 *
	 * @return {@code true} is {@link #dispose()} has been called,
	 *         {@code false} otherwise
	 */
	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void dispose() {
		disposed = true;
		store.removePropertyChangeListener(dateFormatListener);
		super.dispose();
	}
}
