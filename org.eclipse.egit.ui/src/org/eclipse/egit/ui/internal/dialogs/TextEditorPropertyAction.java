/*******************************************************************************
 * Copyright (c) 2012, 2019 Tomasz Zarna (IBM) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Factored out of SpellcheckableMessageArea.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * An {@link Action} that operates on an {@link ITextViewer} that represents a
 * boolean preference in the text editor preferences. When toggled, it toggles
 * the persisted state and invokes {@link #toggleState(boolean)}, which is to be
 * implemented by sub-classes to update the text viewer in response to the
 * preference change.
 */
public abstract class TextEditorPropertyAction extends Action
		implements IPropertyChangeListener, IWorkbenchAction {

	// Possibly originally copied from
	// org.eclipse.compare.internal.TextEditorPropertyAction and adapted?

	private ITextViewer viewer;

	private String preferenceKey;

	private IPreferenceStore store;

	/**
	 * Creates a new {@link TextEditorPropertyAction}.
	 *
	 * @param label
	 *            for the action
	 * @param viewer
	 *            to operate on
	 * @param preferenceKey
	 *            in the {@link EditorsUI#getPreferenceStore()} for this action
	 * @param initiallyOff
	 *            if {@code true}, the action initially is not checked and does
	 *            not reflect the preference state
	 */
	public TextEditorPropertyAction(String label, ITextViewer viewer,
			String preferenceKey, boolean initiallyOff) {
		super(label, IAction.AS_CHECK_BOX);
		this.viewer = viewer;
		this.preferenceKey = preferenceKey;
		this.store = EditorsUI.getPreferenceStore();
		if (store != null) {
			store.addPropertyChangeListener(this);
		}
		if (!initiallyOff) {
			synchronizeWithPreference();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(getPreferenceKey())) {
			synchronizeWithPreference();
		}
	}

	/**
	 * Reads the preference and calls {@link #toggleState(boolean)} as
	 * appropriate.
	 */
	protected void synchronizeWithPreference() {
		boolean checked = false;
		if (store != null) {
			checked = store.getBoolean(getPreferenceKey());
		}
		if (checked != isChecked()) {
			setChecked(checked);
			toggleState(checked);
		} else if (checked) {
			toggleState(false);
			toggleState(true);
		}
	}

	/**
	 * Retrieves the preference key of this action.
	 *
	 * @return the preference key.
	 */
	protected String getPreferenceKey() {
		return preferenceKey;
	}

	@Override
	public void run() {
		toggleState(isChecked());
		if (store != null) {
			store.setValue(getPreferenceKey(), isChecked());
		}
	}

	@Override
	public void dispose() {
		if (store != null) {
			store.removePropertyChangeListener(this);
		}
	}

	/**
	 * Updates the {@link ITextViewer} of this action as appropriate.
	 *
	 * @param checked
	 *            new state
	 */
	abstract protected void toggleState(boolean checked);

	/**
	 * Retrieves the {@link ITextViewer} this action operates on.
	 *
	 * @return the {@link ITextViewer}
	 */
	protected ITextViewer getTextViewer() {
		return viewer;
	}

	/**
	 * Retrieves the {@link IPreferenceStore} this action operates on.
	 *
	 * @return the {@link IPreferenceStore}, or {@code null} if there is no
	 *         preference store for text editors
	 */
	protected IPreferenceStore getStore() {
		return store;
	}
}

