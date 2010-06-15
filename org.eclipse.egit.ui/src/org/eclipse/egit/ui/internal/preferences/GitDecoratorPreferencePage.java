/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator.DecorationHelper;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource.Staged;
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
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
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

	private Preview preview;

	private static final Collection PREVIEW_FILESYSTEM_ROOT;

	private static final Map<String, String> FILE_AND_FOLDER_BINDINGS;

	private static final Map<String, String> PROJECT_BINDINGS;

	private static IPropertyChangeListener themeListener;

	static {
		final PreviewResource project = new PreviewResource(
				"Project", IResource.PROJECT, "repository", "master", true, false, true, Staged.NOT_STAGED, false, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final ArrayList<PreviewResource> children = new ArrayList<PreviewResource>();

		children
				.add(new PreviewResource(
						"folder", IResource.FOLDER, "repository", null, true, false, true, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"tracked.txt", IResource.FILE, "repository", null, true, false, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"untracked.txt", IResource.FILE, "repository", null, false, false, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"ignored.txt", IResource.FILE, "repository", null, false, true, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"dirty.txt", IResource.FILE, "repository", null, true, false, true, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"staged.txt", IResource.FILE, "repository", null, true, false, false, Staged.MODIFIED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"partially-staged.txt", IResource.FILE, "repository", null, true, false, true, Staged.MODIFIED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"added.txt", IResource.FILE, "repository", null, true, false, false, Staged.ADDED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"removed.txt", IResource.FILE, "repository", null, true, false, false, Staged.REMOVED, false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"conflict.txt", IResource.FILE, "repository", null, true, false, true, Staged.NOT_STAGED, true, false)); //$NON-NLS-1$ //$NON-NLS-2$
		children
				.add(new PreviewResource(
						"assume-valid.txt", IResource.FILE, "repository", null, true, false, false, Staged.NOT_STAGED, false, true)); //$NON-NLS-1$ //$NON-NLS-2$
		project.children = children;
		PREVIEW_FILESYSTEM_ROOT = Collections.singleton(project);

		FILE_AND_FOLDER_BINDINGS = new HashMap<String, String>();
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		FILE_AND_FOLDER_BINDINGS.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);

		PROJECT_BINDINGS = new HashMap<String, String>();
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
	}

	/**
	 * Constructs a decorator preference page
	 */
	public GitDecoratorPreferencePage() {
		setDescription(UIText.DecoratorPreferencesPage_description);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_NONE);

		SWTUtils.createPreferenceLink(
				(IWorkbenchPreferenceContainer) getContainer(), composite,
				"org.eclipse.ui.preferencePages.Decorators", //$NON-NLS-1$
				UIText.DecoratorPreferencesPage_labelDecorationsLink);

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHVFillGridData());

		generalTab = new GeneralTab(tabFolder);
		textDecorationTab = new TextDecorationTab(tabFolder);
		iconDecorationTab = new IconDecorationTab(tabFolder);

		initializeValues();

		preview = new Preview(composite);
		preview.refresh();

		generalTab.addObserver(preview);
		textDecorationTab.addObserver(preview);
		iconDecorationTab.addObserver(preview);

		// TODO: Add help text for this preference page

		themeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				preview.refresh();
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(
				themeListener);

		Dialog.applyDialogFont(parent);

		return composite;
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

		private Scale containerRecurseLimit;

		public GeneralTab(TabFolder parent) {
			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT, 1);

			recomputeAncestorDecorations = SWTUtils
					.createCheckBox(
							composite,
							UIText.DecoratorPreferencesPage_recomputeAncestorDecorations);
			recomputeAncestorDecorations
					.setToolTipText(UIText.DecoratorPreferencesPage_recomputeAncestorDecorationsTooltip);

			SWTUtils.createLabel(composite,
					UIText.DecoratorPreferencesPage_computeRecursiveLimit);
			containerRecurseLimit = createLabeledScaleControl(composite);
			containerRecurseLimit
					.setToolTipText(UIText.DecoratorPreferencesPage_computeRecursiveLimitTooltip);

			recomputeAncestorDecorations.addSelectionListener(this);
			containerRecurseLimit.addSelectionListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_generalTabFolder);
			tabItem.setControl(composite);
		}

		public void initializeValues(IPreferenceStore store) {
			recomputeAncestorDecorations.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
			containerRecurseLimit.setSelection(store
					.getInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT));
		}

		public void performDefaults(IPreferenceStore store) {
			recomputeAncestorDecorations
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
			containerRecurseLimit.setSelection(store
					.getDefaultInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT));
		}

		public void performOk(IPreferenceStore store) {
			store.setValue(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS,
					recomputeAncestorDecorations.getSelection());
			store.setValue(UIPreferences.DECORATOR_RECURSIVE_LIMIT,
					containerRecurseLimit.getSelection());
		}

		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			// Not interesting for us
		}

		private Scale createLabeledScaleControl(Composite parent) {

			final int[] values = new int[] { 0, 1, 2, 3, 5, 10, 15, 20, 50,
					100, Integer.MAX_VALUE };

			Composite composite = SWTUtils.createHVFillComposite(parent,
					SWTUtils.MARGINS_DEFAULT);

			Composite labels = SWTUtils.createHVFillComposite(composite,
					SWTUtils.MARGINS_NONE, values.length);
			GridLayout labelsLayout = (GridLayout) labels.getLayout();
			labelsLayout.makeColumnsEqualWidth = true;
			labelsLayout.horizontalSpacing = 0;
			labels.setLayoutData(SWTUtils.createGridData(-1, -1, SWT.FILL,
					SWT.FILL, false, false));

			for (int i = 0; i < values.length; ++i) {
				Label label = SWTUtils.createLabel(labels, "" + values[i]); //$NON-NLS-1$
				if (i == 0) {
					label.setAlignment(SWT.LEFT);
					label.setText("Off"); //$NON-NLS-1$
				} else if (i == values.length - 1) {
					label.setAlignment(SWT.RIGHT);
					label.setText("Inf."); //$NON-NLS-1$
				} else {
					label.setAlignment(SWT.CENTER);
				}
			}

			final Scale scale = new Scale(composite, SWT.HORIZONTAL);
			scale.setLayoutData(SWTUtils.createHVFillGridData());
			scale.setMaximum(values.length - 1);
			scale.setMinimum(0);
			scale.setIncrement(1);
			scale.setPageIncrement(1);

			scale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					// Workaround for GTK treating the slider as stepless
					scale.setSelection(scale.getSelection());
				}
			});

			return scale;
		}
	}

	/**
	 * The tab page for text-decoration preferences
	 */
	private class TextDecorationTab extends Tab implements ModifyListener {

		private final FormatEditor fileTextFormat;

		private final FormatEditor folderTextFormat;

		private final FormatEditor projectTextFormat;

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

			fileTextFormat.addModifyListener(this);
			folderTextFormat.addModifyListener(this);
			projectTextFormat.addModifyListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_textLabel);
			tabItem.setControl(composite);
		}

		public void initializeValues(IPreferenceStore store) {
			fileTextFormat.initializeValue(store);
			folderTextFormat.initializeValue(store);
			projectTextFormat.initializeValue(store);
		}

		public void performDefaults(IPreferenceStore store) {
			fileTextFormat.performDefaults(store);
			folderTextFormat.performDefaults(store);
			projectTextFormat.performDefaults(store);
		}

		public void performOk(IPreferenceStore store) {
			fileTextFormat.performOk(store);
			folderTextFormat.performOk(store);
			projectTextFormat.performOk(store);
		}

		public void modifyText(ModifyEvent e) {
			setChanged();
			notifyObservers();
		}

		private class FormatEditor extends SelectionAdapter {
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

			public void widgetSelected(SelectionEvent e) {
				final ILabelProvider labelProvider = new LabelProvider() {
					public String getText(Object element) {
						return ((Map.Entry) element).getKey()
								+ " - " + ((Map.Entry) element).getValue(); //$NON-NLS-1$
					}
				};

				final IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
					public Object[] getElements(Object inputElement) {
						return ((Collection) inputElement).toArray();
					}

					public void dispose() {
						// No-op
					}

					public void inputChanged(Viewer viewer, Object oldInput,
							Object newInput) {
						// No-op
					}
				};

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
	}

	/**
	 * Tab page for icon-related preferences
	 */
	private class IconDecorationTab extends Tab implements SelectionListener {

		private Button showTracked;

		private Button showUntracked;

		private Button showStaged;

		private Button showConflicts;

		private Button showAssumeValid;

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
			showAssumeValid = SWTUtils.createCheckBox(composite,
					UIText.DecoratorPreferencesPage_iconsShowAssumeValid);

			showTracked.addSelectionListener(this);
			showUntracked.addSelectionListener(this);
			showStaged.addSelectionListener(this);
			showConflicts.addSelectionListener(this);
			showAssumeValid.addSelectionListener(this);

			final TabItem tabItem = new TabItem(parent, SWT.NONE);
			tabItem.setText(UIText.DecoratorPreferencesPage_iconLabel);
			tabItem.setControl(composite);
		}

		public void initializeValues(IPreferenceStore store) {
			showTracked.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON));
			showUntracked.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON));
			showStaged.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON));
			showConflicts.setSelection(store
					.getBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON));
			showAssumeValid
					.setSelection(store
							.getBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON));
		}

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
			showAssumeValid
					.setSelection(store
							.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON));
		}

		public void performOk(IPreferenceStore store) {
			store.setValue(UIPreferences.DECORATOR_SHOW_TRACKED_ICON,
					showTracked.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON,
					showUntracked.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_STAGED_ICON, showStaged
					.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON,
					showConflicts.getSelection());
			store.setValue(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON,
					showAssumeValid.getSelection());
		}

		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}

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
		setValid(true);
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// No-op
	}

	/**
	 * OK was clicked. Store the preferences to the plugin store
	 *
	 * @return whether it is okay to close the preference page
	 */
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
		return true;
	}

	/**
	 * Defaults was clicked. Restore the Git decoration preferences to their
	 * default values
	 */
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		generalTab.performDefaults(store);
		textDecorationTab.performDefaults(store);
		iconDecorationTab.performDefaults(store);
		super.performDefaults();
		preview.refresh();
	}

	/**
	 * Returns the preference store that belongs to the our plugin.
	 *
	 * This is important because we want to store our preferences separately
	 * from the desktop.
	 *
	 * @return the preference store for this plugin
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		PlatformUI.getWorkbench().getThemeManager()
				.removePropertyChangeListener(themeListener);
		super.dispose();
	}

	/**
	 * Preview control for showing how changes in the dialog will affect
	 * decoration
	 */
	private class Preview extends LabelProvider implements Observer,
			ITreeContentProvider {

		private final ResourceManager fImageCache;

		private final TreeViewer fViewer;

		private DecorationHelper fHelper;

		public Preview(Composite composite) {
			// Has to happen before the tree control is constructed
			reloadDecorationHelper();
			SWTUtils.createLabel(composite,
					UIText.DecoratorPreferencesPage_preview);
			fImageCache = new LocalResourceManager(JFaceResources
					.getResources());

			fViewer = new TreeViewer(composite);
			fViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(this);
			fViewer.setInput(PREVIEW_FILESYSTEM_ROOT);
			fViewer.expandAll();
			fHelper = new DecorationHelper(new PreferenceStore());
		}

		private void reloadDecorationHelper() {
			PreferenceStore store = new PreferenceStore();
			performOk(store);
			fHelper = new DecorationHelper(store);
		}

		public void refresh() {
			reloadDecorationHelper();
			fViewer.refresh(true);
			setColorsAndFonts(fViewer.getTree().getItems());
		}

		private void setColorsAndFonts(TreeItem[] items) {
			for (int i = 0; i < items.length; i++) {
				PreviewDecoration decoration = getDecoration(items[i].getData());
				items[i].setBackground(decoration.getBackgroundColor());
				items[i].setForeground(decoration.getForegroundColor());
				items[i].setFont(decoration.getFont());
				setColorsAndFonts(items[i].getItems());
			}
		}

		public void update(Observable o, Object arg) {
			refresh();
		}

		public Object[] getChildren(Object parentElement) {
			return ((PreviewResource) parentElement).children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return !((PreviewResource) element).children.isEmpty();
		}

		public Object[] getElements(Object inputElement) {
			return ((Collection) inputElement).toArray();
		}

		public void dispose() {
			fImageCache.dispose();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// No-op
		}

		public String getText(Object element) {
			final PreviewDecoration decoration = getDecoration(element);
			final StringBuffer buffer = new StringBuffer();
			final String prefix = decoration.getPrefix();
			if (prefix != null)
				buffer.append(prefix);
			buffer.append(((PreviewResource) element).getName());
			final String suffix = decoration.getSuffix();
			if (suffix != null)
				buffer.append(suffix);
			return buffer.toString();
		}

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

		private PreviewDecoration getDecoration(Object element) {
			PreviewDecoration decoration = new PreviewDecoration();
			fHelper.decorate(decoration, (PreviewResource) element);
			return decoration;
		}
	}

	private static class PreviewResource implements IDecoratableResource {
		private final String name;

		private final String repositoryName;

		private final String branch;

		private final int type;

		private Collection children;

		private boolean tracked;

		private boolean ignored;

		private boolean dirty;

		private boolean conflicts;

		private Staged staged;

		private boolean assumeValid;

		public PreviewResource(String name, int type, String repositoryName, String branch,
				boolean tracked, boolean ignored, boolean dirty, Staged staged,
				boolean conflicts, boolean assumeValid) {

			this.name = name;
			this.repositoryName = repositoryName;
			this.branch = branch;
			this.type = type;
			this.children = Collections.EMPTY_LIST;
			this.tracked = tracked;
			this.ignored = ignored;
			this.dirty = dirty;
			this.staged = staged;
			this.conflicts = conflicts;
			this.assumeValid = assumeValid;
		}

		public String getName() {
			return name;
		}

		public String getRepositoryName() {
			return repositoryName;
		}

		public int getType() {
			return type;
		}

		public String getBranch() {
			return branch;
		}

		public boolean isTracked() {
			return tracked;
		}

		public boolean isIgnored() {
			return ignored;
		}

		public boolean isDirty() {
			return dirty;
		}

		public Staged staged() {
			return staged;
		}

		public boolean hasConflicts() {
			return conflicts;
		}

		public boolean isAssumeValid() {
			return assumeValid;
		}
	}

	private static class PreviewDecoration implements IDecoration {

		private List<String> prefixes = new ArrayList<String>();

		private List<String> suffixes = new ArrayList<String>();

		private ImageDescriptor overlay = null;

		private Color backgroundColor = null;

		private Font font = null;

		private Color foregroundColor = null;

		/**
		 * Adds an icon overlay to the decoration
		 * <p>
		 * Copies the behavior of <code>DecorationBuilder</code> of only
		 * allowing the overlay to be set once.
		 */
		public void addOverlay(ImageDescriptor overlayImage) {
			if (overlay == null)
				overlay = overlayImage;
		}

		public void addOverlay(ImageDescriptor overlayImage, int quadrant) {
			addOverlay(overlayImage);
		}

		public void addPrefix(String prefix) {
			prefixes.add(prefix);
		}

		public void addSuffix(String suffix) {
			suffixes.add(suffix);
		}

		public IDecorationContext getDecorationContext() {
			return new DecorationContext();
		}

		public void setBackgroundColor(Color color) {
			backgroundColor = color;
		}

		public void setForegroundColor(Color color) {
			foregroundColor = color;
		}

		public void setFont(Font font) {
			this.font = font;
		}

		public ImageDescriptor getOverlay() {
			return overlay;
		}

		public Color getBackgroundColor() {
			return backgroundColor;
		}

		public Color getForegroundColor() {
			return foregroundColor;
		}

		public Font getFont() {
			return font;
		}

		public String getPrefix() {
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> iter = prefixes.iterator(); iter.hasNext();) {
				sb.append(iter.next());
			}
			return sb.toString();
		}

		public String getSuffix() {
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> iter = suffixes.iterator(); iter.hasNext();) {
				sb.append(iter.next());
			}
			return sb.toString();
		}

	}
}
