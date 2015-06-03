/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preference page for views preferences */
public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private BooleanFieldEditor useLogicalModelEditor;

	private RadioGroupFieldEditor modelStrategyEditor;

	private ScopedPreferenceStore corePreferenceStore;

	/**
	 * The default constructor
	 */
	public SynchronizePreferencePage() {
		super(FLAT);
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
		addField(new BooleanFieldEditor(
				UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH,
				UIText.GitPreferenceRoot_fetchBeforeSynchronization,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.SYNC_VIEW_ALWAYS_SHOW_CHANGESET_MODEL,
				UIText.GitPreferenceRoot_automaticallyEnableChangesetModel,
				getFieldEditorParent()));
		useLogicalModelEditor = new BooleanFieldEditor(
				GitCorePreferences.core_enableLogicalModel,
				UIText.GitPreferenceRoot_useLogicalModel,
				getFieldEditorParent()) {
			@Override
			public IPreferenceStore getPreferenceStore() {
				return getCorePreferenceStore();
			}
		};
		addField(useLogicalModelEditor);
		modelStrategyEditor = new RadioGroupFieldEditor(
				GitCorePreferences.core_preferredModelMergeStrategy,
				UIText.GitPreferenceRoot_preferreModelMergeStrategy, 1,
				getAvailableMergeStrategies(), getFieldEditorParent(), true) {
			@Override
			public IPreferenceStore getPreferenceStore() {
				return getCorePreferenceStore();
			}
		};
		addField(modelStrategyEditor);
	}

	private String[][] getAvailableMergeStrategies() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						"org.eclipse.egit.core.modelMergeStrategy"); //$NON-NLS-1$
		List<String[]> strategies = new ArrayList<>();
		// Always add the default merge strategy first
		strategies.add(new String[] {
				UIText.GitPreferenceRoot_defaultMergeStrategyLabel,
				MergeStrategy.RECURSIVE.getName() });
		for (IConfigurationElement element : elements) {
			try {
				Object ext = element.createExecutableExtension("class"); //$NON-NLS-1$
				if (ext instanceof MergeStrategy) {
					strategies.add(new String[] {
							element.getAttribute("label"), //$NON-NLS-1$
							((MergeStrategy) ext).getName() });
				}
			} catch (CoreException e) {
				Activator
						.logError(
								UIText.GitPreferenceRoot_modelMergeStrategyLoadError,
								e);
			}
		}
		return strategies.toArray(new String[strategies.size()][2]);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == useLogicalModelEditor) {
			modelStrategyEditor.setEnabled(
					((Boolean) event.getNewValue()).booleanValue(),
					getFieldEditorParent());
		}
	}

	private ScopedPreferenceStore getCorePreferenceStore() {
		if (corePreferenceStore == null) {
			corePreferenceStore = new ScopedPreferenceStore(
					InstanceScope.INSTANCE, Activator.getPluginId());
		}
		return corePreferenceStore;
	}
}
