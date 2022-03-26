/*******************************************************************************
 * Copyright (C) 2022, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.DropDownMenuAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

/**
 * A general component for managing some settings related to pushing commits.
 * Settings are persisted in the preferences per repository; the component
 * updates on preference changes. The UI is a ToolItem with drop-down menu.
 */
public class PushSettings {

	private static final String PER_REPO_SETTINGS_PREFIX = "repository_push_settings_"; //$NON-NLS-1$

	private static final String SEPARATOR = ";"; //$NON-NLS-1$

	private static final String NONE = "-"; //$NON-NLS-1$

	// State

	private final IPreferenceStore preferences;

	private IPropertyChangeListener listener;

	private boolean forceState;

	private boolean dialogState;

	private String key;

	private String[] values;

	// UI

	private boolean enabled;

	private boolean visible;

	private ToolBar toolbar;

	private PushSettingsAction action;

	/**
	 * Create a new {@link PushSettings} instance with default values.
	 */
	public PushSettings() {
		preferences = Activator.getDefault().getPreferenceStore();
		listener = event -> {
			if (key != null && key.equals(event.getProperty())) {
				runInUiThread(() -> load(key));
			}
		};
		preferences.addPropertyChangeListener(listener);
	}

	/**
	 * Creates a new {@link PushSettings} and initializes the state for the
	 * given {@link Repository}.
	 *
	 * @param repository
	 *            {@link Repository} to initialize the state for
	 */
	public PushSettings(Repository repository) {
		this();
		load(repository);
	}

	/**
	 * Sets the state of the settings for the given {@link Repository}. If no
	 * previous persisted values are available, default values are used,
	 * otherwise the persisted settings are loaded.
	 *
	 * @param repository
	 *            {@link Repository} to load the values for
	 */
	public void load(Repository repository) {
		if (repository == null) {
			key = null;
		} else {
			key = PER_REPO_SETTINGS_PREFIX
					+ repository.getDirectory().getAbsolutePath();
		}
		load(key);
	}

	private void load(String prefKey) {
		if (prefKey == null) {
			forceState = false;
			dialogState = false;
			setEnabled(false);
		} else {
			String prefs = preferences.getString(prefKey);
			if (StringUtils.isEmptyOrNull(prefs)) {
				values = null;
			} else {
				values = prefs.split(SEPARATOR);
			}
			if (values == null || values.length < 2) {
				values = new String[] { Boolean.FALSE.toString(), NONE };
			}
			forceState = Boolean.parseBoolean(values[0]);
			if (StringUtils.isEmptyOrNull(values[1])
					|| NONE.equals(values[1])) {
				dialogState = preferences.getBoolean(
						UIPreferences.ALWAYS_SHOW_PUSH_WIZARD_ON_COMMIT);
			} else {
				dialogState = Boolean.parseBoolean(values[1]);
			}
			setEnabled(true);
		}
		updateImage();
	}

	private void runInUiThread(Runnable r) {
		if (Display.getCurrent() != null) {
			r.run();
		} else {
			Display.getDefault().asyncExec(r);
		}
	}

	private void save() {
		if (key != null) {
			preferences.setValue(key, String.join(SEPARATOR, values));
		}
	}

	/**
	 * Disposes this {@link PushSettings} component.
	 */
	public void dispose() {
		if (listener != null) {
			preferences.removePropertyChangeListener(listener);
			listener = null;
		}
	}

	/**
	 * Tells whether a push should be a force push.
	 *
	 * @return {@code true} if the "force push" flag is set, {@code false}
	 *         otherwise
	 */
	public boolean isForce() {
		return forceState;
	}

	/**
	 * Tells whether a push should show a push dialog.
	 *
	 * @return {@code true} if a dialog should be shown, {@code false} if the
	 *         push may be performed without dialog, if possible
	 */
	public boolean alwaysShowDialog() {
		return dialogState;
	}

	/**
	 * Creates the UI for this {@link PushSettings} component.
	 *
	 * @param parent
	 *            {@link Composite} to create the UI in
	 * @return the {@link Control}
	 */
	public Control createControl(Composite parent) {
		if (toolbar == null) {
			ToolBarManager settingsManager = new ToolBarManager(
					SWT.FLAT | SWT.HORIZONTAL);
			action = new PushSettingsAction();
			settingsManager.add(action);
			action.setEnabled(isEnabled());
			toolbar = settingsManager.createControl(parent);
			toolbar.setEnabled(isEnabled());
			updateImage();
		}
		return toolbar;
	}

	/**
	 * Retrieves the {@link Control} of this {@link PushSettings} component.
	 *
	 * @return the {@link Control}, or {@code null} if none was created yet
	 */
	public Control getControl() {
		return toolbar;
	}

	private void updateImage() {
		if (action != null && toolbar != null && !toolbar.isDisposed()) {
			action.setImageDescriptor(
					isForce() ? UIIcons.SETTINGS_FORCE : UIIcons.SETTINGS);
		}
	}

	/**
	 * Tells whether the {@link PushSettings} component is enabled.
	 *
	 * @return {@code true} if the component is enabled, {@code false} otherwise
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the {@link PushSettings} component.
	 *
	 * @param enabled
	 *            whether the component is enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (toolbar != null && !toolbar.isDisposed()) {
			toolbar.setEnabled(enabled);
		}
		if (action != null) {
			action.setEnabled(enabled);
		}
	}

	/**
	 * Tells whether the {@link PushSettings} component is visible.
	 *
	 * @return {@code true} if the component is visible, {@code false} otherwise
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Shows or hides the {@link PushSettings} component.
	 *
	 * @param visible
	 *            whether the component is visible
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (toolbar != null && !toolbar.isDisposed()) {
			toolbar.setVisible(visible);
		}
	}

	private class PushSettingsAction extends DropDownMenuAction {

		public PushSettingsAction() {
			super(UIText.PushSettings_Title);
			setImageDescriptor(UIIcons.SETTINGS);
		}

		@Override
		protected Collection<IContributionItem> getActions() {
			if (!isEnabled()) {
				return Collections.emptyList();
			}
			List<IContributionItem> items = new ArrayList<>(2);
			Action forceAction = new Action(UIText.PushSettings_Force,
					IAction.AS_CHECK_BOX) {

				@Override
				public void run() {
					forceState = isChecked();
					values[0] = Boolean.toString(forceState);
					save();
					updateImage();
				}
			};
			forceAction.setChecked(forceState);
			items.add(new ActionContributionItem(forceAction));
			Action showDialogAction = new Action(
					UIText.PushSettings_DialogAlways, IAction.AS_CHECK_BOX) {

				@Override
				public void run() {
					dialogState = isChecked();
					values[1] = Boolean.toString(dialogState);
					save();
				}
			};
			showDialogAction.setChecked(dialogState);
			items.add(new ActionContributionItem(showDialogAction));
			return items;
		}
	}
}
