/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * An Action linked to a boolean preference value.
 */
public abstract class BooleanPrefAction extends Action implements
		IPropertyChangeListener, IWorkbenchAction {
	private final String prefName;

	private final IPersistentPreferenceStore store;

	/**
	 * @param store the preference store
	 * @param pn the preference name
	 * @param text the text for the action
	 */
	protected BooleanPrefAction(final IPersistentPreferenceStore store,
			final String pn, final String text) {
		this.store = store;
		setText(text);
		prefName = pn;
		store.addPropertyChangeListener(this);
		setChecked(store.getBoolean(prefName));
	}

	@Override
	public void run() {
		store.setValue(prefName, isChecked());
		if (store.needsSaving())
			try {
				store.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
	}

	/**
	 * Update to the new value
	 * @param value the current value
	 */
	protected abstract void apply(boolean value);

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (prefName.equals(event.getProperty())) {
			setChecked(store.getBoolean(prefName));
			apply(isChecked());
		}
	}

	@Override
	public void dispose() {
		// stop listening
		store.removePropertyChangeListener(this);
	}
}
