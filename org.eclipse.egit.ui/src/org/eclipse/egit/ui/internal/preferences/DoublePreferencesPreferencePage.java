/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;

/**
 * A {@link FieldEditorPreferencePage} that provides access to a secondary
 * {@link IPreferenceStore} and that takes care of storing that secondary store,
 * if necessary.
 */
public abstract class DoublePreferencesPreferencePage
		extends FieldEditorPreferencePage {

	private IPreferenceStore secondaryStore;

	/**
	 * Creates a new {@link DoublePreferencesPreferencePage} with
	 * {@link FieldEditorPreferencePage#FLAT FLAT} style and neither title nor
	 * image.
	 */
	public DoublePreferencesPreferencePage() {
		super(FLAT);
	}

	/**
	 * Creates a new {@link DoublePreferencesPreferencePage} with the given
	 * style and neither title nor image.
	 *
	 * @param style
	 *            to use
	 */
	protected DoublePreferencesPreferencePage(int style) {
		super(style);
	}

	/**
	 * Creates a new {@link DoublePreferencesPreferencePage} with the given
	 * style and title but without image.
	 *
	 * @param title
	 *            for the page
	 * @param style
	 *            to use
	 */
	protected DoublePreferencesPreferencePage(String title, int style) {
		super(title, style);
	}

	/**
	 * Creates a new {@link DoublePreferencesPreferencePage} with the given
	 * style, title, and image.
	 *
	 * @param title
	 *            for the page
	 * @param imageDescriptor
	 *            for the image for the page
	 * @param style
	 *            to use
	 */
	protected DoublePreferencesPreferencePage(String title,
			ImageDescriptor imageDescriptor, int style) {
		super(title, imageDescriptor, style);
	}

	/**
	 * Returns the secondary preference store of this preference page.
	 * <p>
	 * This is a framework hook method for subclasses to return a page-specific
	 * preference store. The default implementation returns {@code null}.
	 * </p>
	 *
	 * @return the preference store, or {@code null} if none
	 */
	protected IPreferenceStore doGetSecondaryPreferenceStore() {
		return null;
	}

	/**
	 * Returns the secondary preference store of this preference page.
	 *
	 * @return the preference store, or {@code null} if none
	 */
	public IPreferenceStore getSecondaryPreferenceStore() {
		if (secondaryStore == null) {
			secondaryStore = doGetSecondaryPreferenceStore();
		}
		return secondaryStore;
	}

	@Override
	public boolean performOk() {
		boolean isOk = super.performOk();
		if (isOk) {
			saveSecondaryPreferenceStore();
		}
		return isOk;
	}

	@Override
	public void dispose() {
		super.dispose();
		secondaryStore = null;
	}

	private void saveSecondaryPreferenceStore() {
		IPreferenceStore store = getSecondaryPreferenceStore();
		if (store != null && store.needsSaving()
				&& (store instanceof IPersistentPreferenceStore)) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				String message = JFaceResources.format(
						"PreferenceDialog.saveErrorMessage", //$NON-NLS-1$
						new Object[] { getTitle(), e.getMessage() });
				Policy.getStatusHandler().show(
						new Status(IStatus.ERROR, Activator.getPluginId(),
								message, e),
						JFaceResources
								.getString("PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$
			}
		}
	}
}
