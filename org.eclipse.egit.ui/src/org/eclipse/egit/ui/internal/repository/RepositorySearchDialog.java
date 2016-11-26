/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 479108
 *    Simon Scholz <simon.scholz@vogella.com> - Bug 476505
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.components.FilteredCheckboxTree;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Searches for Git directories under a path that can be selected by the user
 */
public class RepositorySearchDialog extends WizardPage {

	private static final String PREF_DEEP_SEARCH = "RepositorySearchDialogDeepSearch"; //$NON-NLS-1$

	private static final String PREF_PATH = "RepositorySearchDialogSearchPath"; //$NON-NLS-1$

	private final Set<String> fExistingDirectories = new HashSet<>();

	private final boolean fillSearch;

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

	private final IEclipsePreferences prefs = InstanceScope.INSTANCE
			.getNode(Activator.getPluginId());

	private boolean allowBare;

	private static final class ContentProvider implements ITreeContentProvider {

		private final Object[] children = new Object[0];

		@Override
		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {
			return ((Set<String>) inputElement).toArray();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

		@Override
		public void dispose() {
			// nothing
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			// do not return null due to a bug in FilteredTree
			return children;
		}

		@Override
		public Object getParent(Object element) {
			// nothing
			return null;
		}

		@Override
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

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			if (fExistingDirectories.contains(element))
				return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);

			return null;
		}

		@Override
		public void dispose() {
			fImageCache.dispose();
		}

	}

	/**
	 * @param existingDirs
	 */
	public RepositorySearchDialog(Collection<String> existingDirs) {
		this(existingDirs, false, true);
	}

	/**
	 * @param existingDirs
	 * @param fillSearch
	 *            true to fill search results when initially displayed
	 * @param allowBare
	 *            if {@code true} allow bare repositories
	 */
	public RepositorySearchDialog(Collection<String> existingDirs,
			boolean fillSearch, boolean allowBare) {
		super("searchPage", UIText.RepositorySearchDialog_SearchTitle, //$NON-NLS-1$
				UIIcons.WIZBAN_IMPORT_REPO);
		this.fExistingDirectories.addAll(existingDirs);
		this.fillSearch = fillSearch;
		this.allowBare = allowBare;
	}

	/**
	 *
	 * @return the directories
	 */
	public Set<String> getDirectories() {
		return fResult;
	}

	@Override
	public void dispose() {
		fResult = getCheckedItems();
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		setMessage(UIText.RepositorySearchDialog_searchRepositoriesMessage);

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group searchGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		searchGroup.setText(UIText.RepositorySearchDialog_SearchCriteriaGroup);
		searchGroup.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.minSize(SWT.DEFAULT, SWT.DEFAULT).applyTo(searchGroup);

		Label dirLabel = new Label(searchGroup, SWT.NONE);
		dirLabel.setText(UIText.RepositorySearchDialog_directory);
		dir = new Text(searchGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).hint(300, SWT.DEFAULT)
				.minSize(100, SWT.DEFAULT).applyTo(dir);
		dir.setToolTipText(UIText.RepositorySearchDialog_EnterDirectoryToolTip);

		String defaultRepoPath = RepositoryUtil.getDefaultRepositoryDir();

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
					dir.setText(directory);
					prefs.put(PREF_PATH, directory);
					try {
						prefs.flush();
					} catch (BackingStoreException e1) {
						// ignore here
					}
					doSearch();
				}
			}

		});

		searchButton = new Button(searchGroup, SWT.PUSH);
		searchButton.setText(UIText.RepositorySearchDialog_Search);
		searchButton
				.setToolTipText(UIText.RepositorySearchDialog_SearchTooltip);
		searchButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doSearch();
			}
		});

		lookForNestedButton = new Button(searchGroup, SWT.CHECK);
		lookForNestedButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 4, 1));
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

		Group searchResultGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		searchResultGroup
				.setText(UIText.RepositorySearchDialog_SearchResultGroup);
		searchResultGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 0)
				.applyTo(searchResultGroup);

		PatternFilter filter = new PatternFilter() {

			@Override
			public boolean isElementVisible(Viewer viewer, Object element) {
				if (getCheckedItems().contains(element)) {
						return true;
				}
				return super.isElementVisible(viewer, element);
			}
		};

		fTree = new FilteredCheckboxTree(searchResultGroup, null, SWT.NONE,
				filter);
		fTreeViewer = fTree.getCheckboxTreeViewer();
		fTreeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				enableOk();
			}
		});

		// Set a reasonable minimum height here; otherwise the dialog comes up
		// with a tree that has only a few rows visible.
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

			@Override
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

			@Override
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

			@Override
			public void modifyText(ModifyEvent e) {
				setNeedsSearch();
			}

		});

		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(new RepositoryLabelProvider());

		setControl(main);

		if (fillSearch)
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!getControl().isDisposed())
						doSearch();
				}
			});
	}

	private void findGitDirsRecursive(File root, Set<File> gitDirs,
			IProgressMonitor monitor, int depth) {

		if (!root.exists() || !root.isDirectory()) {
			return;
		}

		// check the root first
		File resolved = FileKey.resolve(root, FS.DETECTED);
		if ((resolved != null) && !suppressed(root, resolved)) {
			gitDirs.add(resolved.getAbsoluteFile());
			monitor.setTaskName(NLS.bind(
					UIText.RepositorySearchDialog_RepositoriesFound_message,
					Integer.valueOf(gitDirs.size())));
		}

		// check depth and if we are not in private git folder ".git" itself
		if ((depth != 0) && !(resolved != null && isSameFile(root, resolved))) {
			File[] children = root.listFiles();
			if (children == null) {
				return;
			}
			for (File child : children) {
				if (monitor.isCanceled()) {
					return;
				}
				// skip files and .git subfolders in root
				if (child.isDirectory()
						&& !Constants.DOT_GIT.equals(child.getName())) {
					monitor.subTask(child.getPath());
					findGitDirsRecursive(child, gitDirs, monitor, depth - 1);
				}
			}
		}
	}

	private boolean suppressed(@NonNull File root, @NonNull File resolved) {
			return !allowBare && !Constants.DOT_GIT.equals(resolved.getName())
				&& isSameFile(root, resolved);
	}

	private boolean isSameFile(@NonNull File f1, @NonNull File f2) {
		try {
			return Files.isSameFile(f1.toPath(), f2.toPath());
		} catch (IOException e) {
			return false;
		}
	}

	private HashSet<String> getCheckedItems() {
		HashSet<String> ret = new HashSet<>();
		for (Object item : fTreeViewer.getCheckedLeafElements())
			ret.add((String) item);
		return ret;
	}

	private void doSearch() {
		setMessage(UIText.RepositorySearchDialog_searchRepositoriesMessage);
		setErrorMessage(null);
		// perform the search...
		final Set<File> directories = new HashSet<>();
		final File file = new File(dir.getText());
		final boolean lookForNested = lookForNestedButton.getSelection();
		if(!file.exists())
			return;

		prefs.put(PREF_PATH, file.getAbsolutePath());
		try {
			prefs.flush();
		} catch (BackingStoreException e1) {
			// ignore here
		}

		final TreeSet<String> validDirs = new TreeSet<>(getCheckedItems());

		IRunnableWithProgress action = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				monitor.beginTask(
						UIText.RepositorySearchDialog_ScanningForRepositories_message,
						IProgressMonitor.UNKNOWN);
				try {
					findGitDirsRecursive(file, directories, monitor,
							lookForNested ? -1 : 1);
				} catch (Exception ex) {
					throw new InvocationTargetException(ex);
				}
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
			}
		};
		try {
			getContainer().run(true, true, action);
		} catch (InvocationTargetException e1) {
			org.eclipse.egit.ui.Activator.handleError(
					UIText.RepositorySearchDialog_errorOccurred, e1.getCause(),
					true);
		} catch (InterruptedException e1) {
			// ignore
		}

		int foundOld = 0;

		for (File foundDir : directories) {
			String absolutePath = foundDir.getAbsolutePath();
			if (!fExistingDirectories.contains(absolutePath)
					&& !fExistingDirectories.contains(FileUtils
							.canonicalize(foundDir).getAbsolutePath())) {
				validDirs.add(absolutePath);
			} else {
				foundOld++;
			}
		}

		if (foundOld > 0) {
			String message = NLS.bind(
					UIText.RepositorySearchDialog_SomeDirectoriesHiddenMessage,
					Integer.valueOf(foundOld));
			setMessage(message, IMessageProvider.INFORMATION);
		} else if (directories.isEmpty())
			setMessage(UIText.RepositorySearchDialog_NothingFoundMessage,
					IMessageProvider.INFORMATION);

		checkAllItem.setEnabled(!validDirs.isEmpty());
		uncheckAllItem.setEnabled(!validDirs.isEmpty());
		fTree.clearFilter();
		// Remove the minimum height that was set initially so that we get a
		// scrollbar when the dialog is resized.
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTree);
		fTreeViewer.setInput(validDirs);

		if (!validDirs.isEmpty()) {
			fTree.getFilterControl().setFocus();
		}

		enableOk();
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
		setPageComplete(enable);
	}
}
