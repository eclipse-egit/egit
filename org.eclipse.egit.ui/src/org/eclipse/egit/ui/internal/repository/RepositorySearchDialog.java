/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.FilteredCheckboxTree;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.dialogs.PatternFilter;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Searches for Git directories under a path that can be selected by the user
 */
public class RepositorySearchDialog extends TitleAreaDialog {

	private static final String PREF_DEEP_SEARCH = "RepositorySearchDialogDeepSearch"; //$NON-NLS-1$

	private static final String PREF_PATH = "RepositorySearchDialogSearchPath"; //$NON-NLS-1$

	private final Set<String> fExistingDirectories = new HashSet<String>();

	private Set<String> fResult;

	private FilteredCheckboxTree fTree;

	private CachedCheckboxTreeViewer fTreeViewer;

	private Text dir;

	private Button lookForNestedButton;

	private Button searchButton;

	private ToolItem checkAllItem;

	private ToolItem uncheckAllItem;

	private final ResourceManager fImageCache = new LocalResourceManager(
			JFaceResources.getResources());

	private final IEclipsePreferences prefs = new InstanceScope()
			.getNode(Activator.getPluginId());

	private static final class ContentProvider implements ITreeContentProvider {

		private final Object[] children = new Object[0];

		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {
			return ((Set<String>) inputElement).toArray();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

		public void dispose() {
			// nothing
		}

		public Object[] getChildren(Object parentElement) {
			// do not return null due to a bug in FilteredTree
			return children;
		}

		public Object getParent(Object element) {
			// nothing
			return null;
		}

		public boolean hasChildren(Object element) {
			// nothing
			return false;
		}

	}

	private final class RepositoryLabelProvider extends LabelProvider implements
			IColorProvider {

		@Override
		public Image getImage(Object element) {
			return fImageCache.createImage(UIIcons.REPOSITORY);
		}

		@Override
		public String getText(Object element) {
			return element.toString();
		}

		public Color getBackground(Object element) {
			return null;
		}

		public Color getForeground(Object element) {
			if (fExistingDirectories.contains(element))
				return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);

			return null;
		}

		public void dispose() {
			fImageCache.dispose();
		}

	}

	/**
	 * @param parentShell
	 * @param existingDirs
	 */
	public RepositorySearchDialog(Shell parentShell,
			Collection<String> existingDirs) {
		super(parentShell);
		this.fExistingDirectories.addAll(existingDirs);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		setHelpAvailable(false);
	}

	/**
	 *
	 * @return the directories
	 */
	public Set<String> getDirectories() {
		return fResult;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RepositorySearchDialog_AddGitRepositories);
		setTitleImage(fImageCache.createImage(UIIcons.WIZBAN_IMPORT_REPO));
	}

	@Override
	protected void okPressed() {
		fResult = new HashSet<String>();
		fResult.addAll(getCheckedItems());
		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite titleParent = (Composite) super.createDialogArea(parent);

		setTitle(UIText.RepositorySearchDialog_SearchTitle);
		setMessage(UIText.RepositorySearchDialog_searchRepositoriesMessage);

		Composite main = new Composite(titleParent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group searchGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		searchGroup.setText(UIText.RepositorySearchDialog_SearchCriteriaGroup);
		searchGroup.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(searchGroup);

		Label dirLabel = new Label(searchGroup, SWT.NONE);
		dirLabel.setText(UIText.RepositorySearchDialog_directory);
		dir = new Text(searchGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).hint(300, SWT.DEFAULT).applyTo(dir);
		dir.setToolTipText(UIText.RepositorySearchDialog_EnterDirectoryToolTip);

		String defaultRepoPath = org.eclipse.egit.ui.Activator.getDefault()
				.getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);

		String initialPath = prefs.get(PREF_PATH, defaultRepoPath);

		dir.setText(initialPath);

		Button browse = new Button(searchGroup, SWT.PUSH);
		browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		browse.setText(UIText.RepositorySearchDialog_browse);
		browse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setFilterPath(dir.getText());
				String directory = dd.open();
				if (directory != null) {
					setNeedsSearch();
					dir.setText(directory);
					prefs.put(PREF_PATH, directory);
					try {
						prefs.flush();
					} catch (BackingStoreException e1) {
						// ignore here
					}
				}
			}

		});

		lookForNestedButton = new Button(searchGroup, SWT.CHECK);
		lookForNestedButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 3, 1));
		lookForNestedButton.setSelection(prefs.getBoolean(PREF_DEEP_SEARCH,
				false));
		lookForNestedButton
				.setText(UIText.RepositorySearchDialog_DeepSearch_button);
		lookForNestedButton
				.setToolTipText(UIText.RepositorySearchDialog_SearchRecursiveToolTip);

		lookForNestedButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				prefs.putBoolean(PREF_DEEP_SEARCH, lookForNestedButton
						.getSelection());
				try {
					prefs.flush();
				} catch (BackingStoreException e1) {
					// ignore
				}
				setNeedsSearch();
			}

		});

		searchButton = new Button(searchGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().span(3, 1)
				.align(SWT.BEGINNING, SWT.FILL).applyTo(searchButton);
		searchButton.setText(UIText.RepositorySearchDialog_Search);
		searchButton
				.setToolTipText(UIText.RepositorySearchDialog_SearchTooltip);
		searchButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doSearch();
			}
		});


		Group searchResultGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		searchResultGroup
				.setText(UIText.RepositorySearchDialog_SearchResultGroup);
		searchResultGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().applyTo(searchResultGroup);

		PatternFilter filter = new PatternFilter() {

			@Override
			public boolean isElementVisible(Viewer viewer, Object element) {

				if (getCheckedItems().contains(element))
					return true;

				return super.isElementVisible(viewer, element);
			}
		};

		fTree = new FilteredCheckboxTree(searchResultGroup, null, SWT.NONE,
				filter);
		fTreeViewer = fTree.getCheckboxTreeViewer();
		fTreeViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				enableOk();
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).minSize(0, 300)
				.applyTo(fTree);

		ToolBar toolbar = new ToolBar(searchResultGroup, SWT.FLAT
				| SWT.VERTICAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(
				toolbar);

		checkAllItem = new ToolItem(toolbar, SWT.PUSH);
		checkAllItem
				.setToolTipText(UIText.RepositorySearchDialog_CheckAllRepositories);
		checkAllItem.setEnabled(false);
		Image checkImage = UIIcons.CHECK_ALL.createImage();
		UIUtils.hookDisposal(checkAllItem, checkImage);
		checkAllItem.setImage(checkImage);
		checkAllItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fTreeViewer.setAllChecked(true);
				enableOk();
			}

		});

		uncheckAllItem = new ToolItem(toolbar, SWT.PUSH);
		uncheckAllItem
				.setToolTipText(UIText.RepositorySearchDialog_UncheckAllRepositories);
		uncheckAllItem.setEnabled(false);
		Image uncheckImage = UIIcons.UNCHECK_ALL.createImage();
		UIUtils.hookDisposal(uncheckAllItem, uncheckImage);
		uncheckAllItem.setImage(uncheckImage);
		uncheckAllItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fTreeViewer.setAllChecked(false);
				enableOk();
			}

		});

		// TODO this isn't the most optimal way of handling this... ideally we
		// should have some type of delay
		// if we could use databinding an observeDelayedValue would totally work
		// here
		dir.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				setNeedsSearch();
			}

		});

		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(new RepositoryLabelProvider());

		applyDialogFont(main);

		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		enableOk();
	}

	private void findGitDirsRecursive(File root, Set<String> strings,
			IProgressMonitor monitor, boolean lookForNestedRepositories) {

		if (!root.exists() || !root.isDirectory()) {
			return;
		}
		File[] children = root.listFiles();
		// simply ignore null
		if (children == null)
			return;

		for (File child : children) {
			if (monitor.isCanceled()) {
				return;
			}

			if (child.isDirectory()
					&& RepositoryCache.FileKey.isGitRepository(child,
							FS.DETECTED)) {
				try {
					strings.add(child.getCanonicalPath());
				} catch (IOException e) {
					// ignore here
				}
				monitor
						.setTaskName(NLS
								.bind(
										UIText.RepositorySearchDialog_RepositoriesFound_message,
										Integer.valueOf(strings.size())));
				if (!lookForNestedRepositories)
					return;
			} else if (child.isDirectory()) {
				monitor.subTask(child.getPath());
				findGitDirsRecursive(child, strings, monitor,
						lookForNestedRepositories);
			}
		}

	}

	private HashSet<String> getCheckedItems() {
		HashSet<String> ret = new HashSet<String>();
		for (Object item : fTreeViewer.getCheckedElements())
			ret.add((String) item);
		return ret;
	}

	private void doSearch() {

		setMessage(null);
		setErrorMessage(null);
		// perform the search...
		final Set<String> directories = new HashSet<String>();
		final File file = new File(dir.getText());
		final boolean lookForNested = lookForNestedButton.getSelection();
		if (file.exists()) {
			try {
				prefs.put(PREF_PATH, file.getCanonicalPath());
				try {
					prefs.flush();
				} catch (BackingStoreException e1) {
					// ignore here
				}
			} catch (IOException e2) {
				// ignore
			}

			IRunnableWithProgress action = new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					try {
						findGitDirsRecursive(file, directories, monitor,
								lookForNested);
					} catch (Exception ex) {
						Activator.getDefault().getLog().log(
								new Status(IStatus.ERROR, Activator
										.getPluginId(), ex.getMessage(), ex));
					}
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}
				}
			};
			try {
				ProgressMonitorDialog pd = new ProgressMonitorDialog(getShell());
				pd
						.getProgressMonitor()
						.setTaskName(
								UIText.RepositorySearchDialog_ScanningForRepositories_message);
				pd.run(true, true, action);

			} catch (InvocationTargetException e1) {
				org.eclipse.egit.ui.Activator.handleError(
						UIText.RepositorySearchDialog_errorOccurred, e1, true);
			} catch (InterruptedException e1) {
				// ignore
			}

			int foundOld = 0;

			final TreeSet<String> validDirs = new TreeSet<String>();

			for (String foundDir : directories) {
				if (!fExistingDirectories.contains(foundDir)) {
					validDirs.add(foundDir);
				} else {
					foundOld++;
				}
			}

			if (foundOld > 0) {
				String message = NLS
						.bind(
								UIText.RepositorySearchDialog_SomeDirectoriesHiddenMessage,
								Integer.valueOf(foundOld));
				setMessage(message, IMessageProvider.INFORMATION);
			} else if (directories.isEmpty())
				setMessage(UIText.RepositorySearchDialog_NothingFoundMessage,
						IMessageProvider.INFORMATION);

			checkAllItem.setEnabled(!validDirs.isEmpty());
			uncheckAllItem.setEnabled(!validDirs.isEmpty());
			fTree.clearFilter();
			fTreeViewer.setInput(validDirs);
			// this sets all to selected
			fTreeViewer.setAllChecked(true);
			enableOk();

		}

	}

	private void setNeedsSearch() {
		fTreeViewer.setInput(null);
		final File file = new File(dir.getText());
		if (!file.exists()) {
			setErrorMessage(NLS.bind(
					UIText.RepositorySearchDialog_DirectoryNotFoundMessage, dir
							.getText()));
		} else {
			setErrorMessage(null);
			setMessage(UIText.RepositorySearchDialog_NoSearchAvailableMessage,
					IMessageProvider.INFORMATION);
		}
		enableOk();
	}

	private void enableOk() {
		boolean enable = fTreeViewer.getCheckedElements().length > 0;
		getButton(OK).setEnabled(enable);
		if (enable)
			getButton(OK).setFocus();
	}
}
