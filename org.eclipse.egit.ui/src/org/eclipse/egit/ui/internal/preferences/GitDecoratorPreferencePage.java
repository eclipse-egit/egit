/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2015, 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.DecoratableResource;
import org.eclipse.egit.ui.internal.decorators.DecorationResult;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator.DecorationHelper;
import org.eclipse.egit.ui.internal.resources.IResourceState.StagingState;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitChangeSetLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Preference page for customizing Git label decorations
 */
public class GitDecoratorPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private GeneralTab generalTab;

	private TextDecorationTab textDecorationTab;

	private IconDecorationTab iconDecorationTab;

	private OtherDecorationTab otherDecorationTab;

	private Preview navigatorPreview;

	private Preview changeSetPreview;

	private boolean tabsInitialized;

	private static final int UNTRACKED = (1 << 0);

	private static final int IGNORED = (1 << 1);

	private static final int DIRTY = (1 << 2);

	private static final int CONFLICTS = (1 << 3);

	private static final int ASSUME_UNCHANGED = (1 << 4);

	private static final int SUBMODULE = (1 << 5);

	private static final String SAMPLE_COMMIT_MESSAGE = "Commit message text"; //$NON-NLS-1$

	private static final Collection PREVIEW_FILESYSTEM_ROOT;

	private static final Map<String, String> FILE_AND_FOLDER_BINDINGS;

	private static final Map<String, String> PROJECT_BINDINGS;

	private static final Map<String, String> CHANGESET_LABEL_BINDINGS;

	private IPropertyChangeListener themeListener;

	/**
	 * Listens to changes in the date preferences and updates the
	 * changeSetPreview if the preferences change.
	 */
	private IPropertyChangeListener uiPrefsListener;

	static {
		final PreviewResource project = new PreviewResource(
				"Project", IResource.PROJECT, //$NON-NLS-1$
				"repository" + '|' + RepositoryState.MERGING.getDescription(), //$NON-NLS-1$
				"master", "↑2 ↓1", DIRTY, StagingState.NOT_STAGED); //$NON-NLS-1$ //$NON-NLS-2$
		final ArrayList<PreviewResource> children = new ArrayList<>();

		children
				.add(new PreviewResource(
						"folder", IResource.FOLDER, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						DIRTY, StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"submodule", IResource.FOLDER, "submodule", //$NON-NLS-1$ //$NON-NLS-2$
						"master 5bef90d", null, DIRTY | SUBMODULE, //$NON-NLS-1$
						StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"tracked.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						0, StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"untracked.txt", IResource.FILE, "repository", null, //$NON-NLS-1$ //$NON-NLS-2$
						null, UNTRACKED, StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"ignored.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						IGNORED, StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"dirty.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						DIRTY, StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"staged.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						0, StagingState.MODIFIED));
		children
				.add(new PreviewResource(
						"partially-staged.txt", IResource.FILE, "repository", //$NON-NLS-1$ //$NON-NLS-2$
						null, null, DIRTY, StagingState.MODIFIED));
		children
				.add(new PreviewResource(
						"added.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						0, StagingState.ADDED));
		children
				.add(new PreviewResource(
						"removed.txt", IResource.FILE, "repository", null, null, //$NON-NLS-1$ //$NON-NLS-2$
						0, StagingState.REMOVED));
		children
				.add(new PreviewResource(
						"conflict.txt", IResource.FILE, "repository", null, //$NON-NLS-1$ //$NON-NLS-2$
						null, DIRTY | CONFLICTS,
						StagingState.NOT_STAGED));
		children
				.add(new PreviewResource(
						"assume-unchanged.txt", IResource.FILE, "repository", //$NON-NLS-1$ //$NON-NLS-2$
						null, null, ASSUME_UNCHANGED,
						StagingState.NOT_STAGED));
		project.children = children;
		PREVIEW_FILESYSTEM_ROOT = Collections.singleton(project);

		FILE_AND_FOLDER_BINDINGS = new TreeMap<>();
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);

		PROJECT_BINDINGS = new TreeMap<>();
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_REPOSITORY_NAME,
				UIText.GitDecoratorPreferencePage_bindingRepositoryNameFlag);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_BRANCH_NAME,
				UIText.DecoratorPreferencesPage_bindingBranchName);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_BRANCH_STATUS,
				UIText.DecoratorPreferencesPage_bindingBranchStatus);
		PROJECT_BINDINGS.put(DecorationHelper.BINDING_SHORT_MESSAGE,
				UIText.DecoratorPreferencesPage_bindingCommitMessage);


		CHANGESET_LABEL_BINDINGS = new TreeMap<>();
		CHANGESET_LABEL_BINDINGS.put(removeBraces(GitChangeSetLabelProvider.BINDING_CHANGESET_AUTHOR),
				UIText.DecoratorPreferencesPage_bindingChangeSetAuthor);
		CHANGESET_LABEL_BINDINGS.put(removeBraces(GitChangeSetLabelProvider.BINDING_CHANGESET_DATE),
				UIText.DecoratorPreferencesPage_bindingChangeSetDate);
		CHANGESET_LABEL_BINDINGS.put(removeBraces(GitChangeSetLabelProvider.BINDING_CHANGESET_COMMITTER),
				UIText.DecoratorPreferencesPage_bindingChangeSetCommitter);
		CHANGESET_LABEL_BINDINGS.put(removeBraces(GitChangeSetLabelProvider.BINDING_CHANGESET_SHORT_MESSAGE),
				UIText.DecoratorPreferencesPage_bindingChangeSetShortMessage);
	}

	private static String removeBraces(String string) {
		return string.replaceAll("[}{]", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}


	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_NONE);

		SWTUtils.createLabel(composite, UIText.DecoratorPreferencesPage_description);

		Composite folderComposite = SWTUtils.createHFillComposite(composite,
				SWTUtils.MARGINS_NONE);

		TabFolder tabFolder = new TabFolder(folderComposite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHVFillGridData());

		tabFolder.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (navigatorPreview != null && changeSetPreview != null) {
					if (UIText.DecoratorPreferencesPage_otherDecorations.equals(e.item.getData())) {
						navigatorPreview.hide();
						changeSetPreview.show();
					} else {
						changeSetPreview.hide();
						navigatorPreview.show();
					}
				}
			}

		});

		changeSetPreview = new ChangeSetPreview(composite);
		navigatorPreview = new NavigatorPreview(composite);

		generalTab = new GeneralTab(tabFolder);
		textDecorationTab = new TextDecorationTab(tabFolder);
		iconDecorationTab = new IconDecorationTab(tabFolder);
		otherDecorationTab = new OtherDecorationTab(tabFolder);

		initializeValues();

		changeSetPreview.hide();

		changeSetPreview.refresh();
		navigatorPreview.refresh();

		generalTab.addObserver(navigatorPreview);
		textDecorationTab.addObserver(navigatorPreview);
		iconDecorationTab.addObserver(navigatorPreview);

		otherDecorationTab.addObserver(changeSetPreview);

		// TODO: Add help text for this preference page

		themeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				navigatorPreview.refresh();
				changeSetPreview.refresh();
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(
				themeListener);

		uiPrefsListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (UIPreferences.DATE_FORMAT.equals(property)
						|| UIPreferences.DATE_FORMAT_CHOICE.equals(property)) {
					changeSetPreview.refresh();
				}
			}
		};
		getPreferenceStore().addPropertyChangeListener(uiPrefsListener);

		Dialog.applyDialogFont(parent);

		return tabFolder;
	}

	/**
	 * Wrapper class for TabItems used in the preference page TabFolder
	 *
	 * The reason for this wrapper is mainly that the class TabItem is not
	 * supposed to be subclassed.
	 *
	 * When controls in the tab change it will emit update() to any registered
	 * observers. This is currently used for updating the decoration preview.
	 */
	private abstract class Tab extends Observable {
		public abstract void initializeValues(IPreferenceStore store);

		public abstract void performDefaults(IPreferenceStore store);

		public abstract void performOk(IPreferenceStore store);
	}

	/**
	 * Tab page for general preferences related to decoration
	 */
	private class GeneralTab extends Tab implements SelectionListener {

		private Button recomputeAncestorDecorations;

		public GeneralTab(TabFolder parent) {
			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT, 1);

			recomputeAncestorDecorations = SWTUtils
					.createCheckBox(
							composite,
							UIText.DecoratorPreferencesPage_recomputeAncestorDecorations);
			recomputeAncestorDecorations
					.setToolTipText(UIText.DecoratorPreferencesPage_recomputeAncestorDecorationsTooltip);

			SWTUtils.createPreferenceLink(
					(IWorkbenchPreferenceContainer) getContainer(), composite,
					"org.eclipse.ui.preferencePages.Decorators", //$NON-NLS-1$
					UIText.DecoratorPreferencesPage_labelDecorationsLink);

			SWTUtils.createPreferenceLink(
					(IWorkbenchPreferenceContainer) getContainer(), composite,
					"org.eclipse.ui.preferencePages.ColorsAndFonts", //$NON-NLS-1$
					UIText.DecoratorPreferencesPage_colorsAndFontsLink);

			recomputeAncestorDecorations.addSelectionListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_generalTabFolder);
			tabItem.setControl(composite);
		}

		@Override
		public void initializeValues(IPreferenceStore store) {
			recomputeAncestorDecorations.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
		}

		@Override
		public void performDefaults(IPreferenceStore store) {
			recomputeAncestorDecorations
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
		}

		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS,
					recomputeAncestorDecorations.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// Not interesting for us
		}

	}

	/**
	 * The tab page for text-decoration preferences
	 */
	private class TextDecorationTab extends Tab implements ModifyListener {

		private final FormatEditor fileTextFormat;

		private final FormatEditor folderTextFormat;

		private final FormatEditor projectTextFormat;

		private final FormatEditor submoduleTextFormat;

		public TextDecorationTab(TabFolder parent) {
			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT, 3);

			fileTextFormat = new FormatEditor(composite,
					UIText.DecoratorPreferencesPage_fileFormatLabel,
					UIText.DecoratorPreferencesPage_addVariablesAction,
					FILE_AND_FOLDER_BINDINGS,
					UIPreferences.DECORATOR_FILETEXT_DECORATION);
			folderTextFormat = new FormatEditor(composite,
					UIText.DecoratorPreferencesPage_folderFormatLabel,
					UIText.DecoratorPreferencesPage_addVariablesAction2,
					FILE_AND_FOLDER_BINDINGS,
					UIPreferences.DECORATOR_FOLDERTEXT_DECORATION);
			projectTextFormat = new FormatEditor(composite,
					UIText.DecoratorPreferencesPage_projectFormatLabel,
					UIText.DecoratorPreferencesPage_addVariablesAction3,
					PROJECT_BINDINGS,
					UIPreferences.DECORATOR_PROJECTTEXT_DECORATION);
			submoduleTextFormat = new FormatEditor(composite,
					UIText.DecoratorPreferencesPage_submoduleFormatLabel,
					UIText.DecoratorPreferencesPage_addVariablesAction3,
					PROJECT_BINDINGS,
					UIPreferences.DECORATOR_SUBMODULETEXT_DECORATION);

			fileTextFormat.addModifyListener(this);
			folderTextFormat.addModifyListener(this);
			projectTextFormat.addModifyListener(this);
			submoduleTextFormat.addModifyListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_textLabel);
			tabItem.setControl(composite);
		}

		@Override
		public void initializeValues(IPreferenceStore store) {
			fileTextFormat.initializeValue(store);
			folderTextFormat.initializeValue(store);
			projectTextFormat.initializeValue(store);
			submoduleTextFormat.initializeValue(store);
		}

		@Override
		public void performDefaults(IPreferenceStore store) {
			fileTextFormat.performDefaults(store);
			folderTextFormat.performDefaults(store);
			projectTextFormat.performDefaults(store);
			submoduleTextFormat.performDefaults(store);
		}

		@Override
		public void performOk(IPreferenceStore store) {
			fileTextFormat.performOk(store);
			folderTextFormat.performOk(store);
			projectTextFormat.performOk(store);
			submoduleTextFormat.performOk(store);
		}

		@Override
		public void modifyText(ModifyEvent e) {
			setChanged();
			notifyObservers();
		}

	}

	private class OtherDecorationTab extends Tab implements ModifyListener {

		private final FormatEditor changeSetLabelFormat;

		public OtherDecorationTab(TabFolder parent) {
			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT, 3);

			changeSetLabelFormat = new FormatEditor(composite,
					UIText.DecoratorPreferencesPage_changeSetLabelFormat,
					UIText.DecoratorPreferencesPage_addVariablesAction3,
					CHANGESET_LABEL_BINDINGS,
					UIPreferences.SYNC_VIEW_CHANGESET_LABEL_FORMAT);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);

			tabItem.setText(UIText.DecoratorPreferencesPage_otherDecorations);
			tabItem.setControl(composite);
			tabItem.setData(UIText.DecoratorPreferencesPage_otherDecorations);

			changeSetLabelFormat.addModifyListener(this);
		}

		@Override
		public void initializeValues(IPreferenceStore store) {
			changeSetLabelFormat.initializeValue(store);
		}

		@Override
		public void performDefaults(IPreferenceStore store) {
			changeSetLabelFormat.performDefaults(store);
		}

		@Override
		public void performOk(IPreferenceStore store) {
			changeSetLabelFormat.performOk(store);
		}

		@Override
		public void modifyText(ModifyEvent e) {
			setChanged();
			notifyObservers();
		}
	}

	private static final class FormatEditor extends SelectionAdapter {
		private final Text text;

		private final Map bindings;

		private final String key;

		public FormatEditor(Composite composite, String title,
				String buttonText, Map bindings, String key) {

			this.key = key;
			this.bindings = bindings;

			final Label label = SWTUtils.createLabel(composite, title);
			label.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT,
					SWT.DEFAULT, false, false));

			text = SWTUtils.createText(composite);

			final Button button = new Button(composite, SWT.NONE);
			button.setText(buttonText);
			button.setLayoutData(new GridData());

			button.addSelectionListener(this);
		}

		public void addModifyListener(ModifyListener listener) {
			text.addModifyListener(listener);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			final ILabelProvider labelProvider = new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Map.Entry) element).getKey()
					+ " - " + ((Map.Entry) element).getValue(); //$NON-NLS-1$
				}
			};

			final IStructuredContentProvider contentsProvider = ArrayContentProvider.getInstance();

			final ListSelectionDialog dialog = new ListSelectionDialog(text
					.getShell(), bindings.entrySet(), contentsProvider,
					labelProvider,
					UIText.DecoratorPreferencesPage_selectVariablesToAdd);
			dialog.setHelpAvailable(false);
			dialog
			.setTitle(UIText.DecoratorPreferencesPage_addVariablesTitle);
			if (dialog.open() != Window.OK)
				return;

			Object[] result = dialog.getResult();

			for (int i = 0; i < result.length; i++) {
				text.insert("{" + ((Map.Entry) result[i]).getKey() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		public void performOk(IPreferenceStore store) {
			store.setValue(key, text.getText());
		}

		public void performDefaults(IPreferenceStore store) {
			store.setToDefault(key);
			text.setText(store.getDefaultString(key));
		}

		public void initializeValue(IPreferenceStore store) {
			text.setText(store.getString(key));
		}
	}

	/**
	 * Tab page for icon-related preferences
	 */
	private class IconDecorationTab extends Tab implements SelectionListener {

		private Button showTracked;

		private Button showUntracked;

		private Button showStaged;

		private Button showConflicts;

		private Button showAssumeUnchanged;

		private Button showDirty;

		public IconDecorationTab(TabFolder parent) {
			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT, 2);

			showTracked = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowTracked);
			showUntracked = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowUntracked);
			showStaged = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowStaged);
			showConflicts = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowConflicts);
			showAssumeUnchanged = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowAssumeUnchanged);
			showDirty = SWTUtils.createCheckBox(composite,
					UIText.GitDecoratorPreferencePage_iconsShowDirty);

			showTracked.addSelectionListener(this);
			showUntracked.addSelectionListener(this);
			showStaged.addSelectionListener(this);
			showConflicts.addSelectionListener(this);
			showAssumeUnchanged.addSelectionListener(this);
			showDirty.addSelectionListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_iconLabel);
			tabItem.setControl(composite);
		}

		@Override
		public void initializeValues(IPreferenceStore store) {
			showTracked.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON));
			showUntracked.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON));
			showStaged.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON));
			showConflicts.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON));
			showAssumeUnchanged
					.setSelection(store
							.getBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_UNCHANGED_ICON));
			showDirty.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_DIRTY_ICON));
		}

		@Override
		public void performDefaults(IPreferenceStore store) {
			showTracked
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON));
			showUntracked
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON));
			showStaged
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON));
			showConflicts
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON));
			showAssumeUnchanged
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_UNCHANGED_ICON));
			showDirty
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_DIRTY_ICON));
		}

		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(UIPreferences.DECORATOR_SHOW_TRACKED_ICON,
					showTracked.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON,
					showUntracked.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_STAGED_ICON, showStaged
					.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON,
					showConflicts.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_ASSUME_UNCHANGED_ICON,
					showAssumeUnchanged.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_DIRTY_ICON,
					showDirty.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// Not interesting for us
		}
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		generalTab.initializeValues(store);
		textDecorationTab.initializeValues(store);
		iconDecorationTab.initializeValues(store);
		otherDecorationTab.initializeValues(store);
		setValid(true);
		tabsInitialized = true;
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		// No-op
	}

	/**
	 * OK was clicked. Store the preferences to the plugin store
	 *
	 * @return whether it is okay to close the preference page
	 */
	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		final boolean okToClose = performOk(store);
		if (store.needsSaving()) {
			try {
				((IPersistentPreferenceStore)store).save();
				Activator.broadcastPropertyChange(new PropertyChangeEvent(this,
						Activator.DECORATORS_CHANGED, null, null));
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}
		return okToClose;
	}

	/**
	 * Store the preferences to the given preference store
	 *
	 * @param store
	 *            the preference store to store the preferences to
	 *
	 * @return whether it operation succeeded
	 */
	private boolean performOk(IPreferenceStore store) {
		generalTab.performOk(store);
		textDecorationTab.performOk(store);
		iconDecorationTab.performOk(store);
		otherDecorationTab.performOk(store);
		return true;
	}

	/**
	 * Defaults was clicked. Restore the Git decoration preferences to their
	 * default values
	 */
	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		generalTab.performDefaults(store);
		textDecorationTab.performDefaults(store);
		iconDecorationTab.performDefaults(store);
		otherDecorationTab.performDefaults(store);
		super.performDefaults();
		navigatorPreview.refresh();
		changeSetPreview.refresh();
	}

	/**
	 * Returns the preference store that belongs to our plugin.
	 *
	 * This is important because we want to store our preferences separately
	 * from the desktop.
	 *
	 * @return the preference store for this plugin
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		if (themeListener != null) {
			PlatformUI.getWorkbench().getThemeManager()
					.removePropertyChangeListener(themeListener);
		}
		if (uiPrefsListener != null) {
			getPreferenceStore().removePropertyChangeListener(uiPrefsListener);
		}
		super.dispose();
	}

	private abstract class Preview
			implements Observer {

		protected PreferenceStore store = new PreferenceStore();
		protected final TreeViewer fViewer;
		private Composite composite;
		private Composite parent;

		protected final ResourceManager fImageCache = new LocalResourceManager(
				JFaceResources.getResources());

		public Preview(Composite parent) {
			this.parent = parent;
			composite = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE);

			SWTUtils.createLabel(composite, UIText.DecoratorPreferencesPage_preview);

			fViewer = new TreeViewer(composite);
			fViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
		}

		@Override
		public void update(Observable o, Object arg) {
			refresh();
		}

		public abstract void refresh();

		public void dispose() {
			fImageCache.dispose();
		}

		public void hide() {
			((GridData)composite.getLayoutData()).exclude = true;	// ignore by layout
			composite.setVisible(false);
			composite.layout();
			parent.layout();
		}

		public void show() {
			((GridData)composite.getLayoutData()).exclude = false;	// ignore by layout
			composite.setVisible(true);
			composite.layout();
			parent.layout();
		}
	}

	private class ChangeSetPreview extends Preview implements
			ITreeContentProvider {

		public ChangeSetPreview(Composite composite) {
			super(composite);
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(new GitLabelProvider() {

				@Override
				public Image getImage(Object element) {
					if (element instanceof GitModelCommitMockup)
						return getChangesetIcon();

					return super.getImage(element);
				}

				@Override
				public String getText(Object element) {
					if (element instanceof GitModelCommitMockup) {
						String format = store.getString(UIPreferences.SYNC_VIEW_CHANGESET_LABEL_FORMAT);
						return ((GitModelCommitMockup) element)
								.getMokeupText(format);
					}
					return super.getText(element);
				}
			});
			fViewer.setContentProvider(this);
			fViewer.setInput(new GitModelCommitMockup());
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[] { inputElement };
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// No-op
		}

		@Override
		public void refresh() {
			store = new PreferenceStore();
			performOk(store);
			fViewer.refresh(true);
		}
	}


	/**
	 * NavigatorPreview control for showing how changes in the dialog will affect
	 * decoration
	 */
	private class NavigatorPreview extends Preview
			implements ITreeContentProvider {

		private DecorationHelper fHelper;

		public NavigatorPreview(Composite composite) {
			super(composite);
			// Has to happen before the tree control is constructed
			reloadDecorationHelper();

			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
					new ResLabelProvider()));
			fViewer.setInput(PREVIEW_FILESYSTEM_ROOT);
			fViewer.expandAll();
			fHelper = new DecorationHelper(new PreferenceStore());
		}

		private void reloadDecorationHelper() {
			store = new PreferenceStore();
			if (tabsInitialized)
				performOk(store);

			fHelper = new DecorationHelper(store);
		}

		@Override
		public void refresh() {
			reloadDecorationHelper();
			fViewer.refresh(true);
			setColorsAndFonts(fViewer.getTree().getItems());
		}

		private void setColorsAndFonts(TreeItem[] items) {
			for (int i = 0; i < items.length; i++) {
				DecorationResult decoration = getDecoration(items[i].getData());
				items[i].setBackground(decoration.getBackgroundColor());
				items[i].setForeground(decoration.getForegroundColor());
				items[i].setFont(decoration.getFont());
				setColorsAndFonts(items[i].getItems());
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((PreviewResource) parentElement).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((PreviewResource) element).children.isEmpty();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((Collection) inputElement).toArray();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// No-op
		}

		private DecorationResult getDecoration(Object element) {
			DecorationResult decoration = new DecorationResult();
			fHelper.decorate(decoration, (PreviewResource) element);
			return decoration;
		}

		private class ResLabelProvider extends LabelProvider
				implements IStyledLabelProvider {

			@Override
			public String getText(Object element) {
				return getStyledText(element).getString();
			}

			@Override
			public StyledString getStyledText(Object element) {
				StyledString result = new StyledString();
				DecorationResult decoration = getDecoration(element);
				String extra = decoration.getPrefix();
				if (extra != null) {
					result.append(extra, StyledString.DECORATIONS_STYLER);
				}
				result.append(((PreviewResource) element).getName());
				extra = decoration.getSuffix();
				if (extra != null) {
					result.append(extra, StyledString.DECORATIONS_STYLER);
				}
				return result;
			}

			@Override
			public Image getImage(Object element) {
				final String s;
				switch (((PreviewResource) element).type) {
				case IResource.PROJECT:
					s = SharedImages.IMG_OBJ_PROJECT;
					break;
				case IResource.FOLDER:
					s = ISharedImages.IMG_OBJ_FOLDER;
					break;
				default:
					s = ISharedImages.IMG_OBJ_FILE;
					break;
				}
				final Image baseImage = PlatformUI.getWorkbench().getSharedImages()
						.getImage(s);
				final ImageDescriptor overlay = getDecoration(element).getOverlay();
				if (overlay == null)
					return baseImage;
				try {
					return fImageCache.createImage(new DecorationOverlayIcon(
							baseImage, overlay, IDecoration.BOTTOM_RIGHT));
				} catch (Exception e) {
					Activator.logError(e.getMessage(), e);
				}

				return null;
			}
		}
	}

	private static class GitModelCommitMockup {

		private static final String author = "Author Name"; //$NON-NLS-1$
		private static final Date date = new Date();
		private static final String committer = "Committer Name";  //$NON-NLS-1$

		public String getMokeupText(String format) {
			PreferenceBasedDateFormatter formatter = PreferenceBasedDateFormatter
					.create();

			Map<String, String> bindings = new HashMap<>();
			bindings.put(GitChangeSetLabelProvider.BINDING_CHANGESET_DATE,
					formatter.formatDate(date));
			bindings.put(GitChangeSetLabelProvider.BINDING_CHANGESET_AUTHOR, author);
			bindings.put(GitChangeSetLabelProvider.BINDING_CHANGESET_COMMITTER, committer);
			bindings.put(
					GitChangeSetLabelProvider.BINDING_CHANGESET_SHORT_MESSAGE,
					SAMPLE_COMMIT_MESSAGE);

			return GitChangeSetLabelProvider.formatName(format, bindings);
		}
	}

	private static class PreviewResource extends DecoratableResource {

		private final String name;

		private final int type;

		private Collection children;

		public PreviewResource(String name, int type, String repositoryName,
				String branch, String branchStatus, int flags,
				@NonNull StagingState staged) {

			super(null);
			this.name = name;
			this.repositoryName = repositoryName;
			this.commitMessage = SAMPLE_COMMIT_MESSAGE;
			this.branch = branch;
			this.branchStatus = branchStatus;
			this.type = type;
			this.children = Collections.EMPTY_LIST;
			setTracked((flags & UNTRACKED) == 0);
			setIgnored((flags & IGNORED) != 0);
			setDirty((flags & DIRTY) != 0);
			setStagingState(staged);
			setConflicts((flags & CONFLICTS) != 0);
			setAssumeUnchanged((flags & ASSUME_UNCHANGED) != 0);
			setIsRepositoryContainer((flags & SUBMODULE) != 0);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getType() {
			return type;
		}

	}
}
