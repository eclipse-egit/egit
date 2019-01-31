/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
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
import org.eclipse.swt.SWT;
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

	private static final String PREF_SKIP_HIDDEN = "RepositorySearchDialogSkipHidden"; //$NON-NLS-1$

	private static final String PREF_PATH = "RepositorySearchDialogSearchPath"; //$NON-NLS-1$

	private final Set<String> fExistingDirectories = new HashSet<>();

	private final boolean fillSearch;

	private Set<String> fResult;

	private FilteredCheckboxTree fTree;

	private CachedCheckboxTreeViewer fTreeViewer;

	private Text dir;

	private Button lookForNestedButton;

	private Button skipHiddenButton;

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

		Button browse = new Button(searchGroup, SWT.PUSH);
		browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		browse.setText(UIText.RepositorySearchDialog_browse);
		browse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setMessage(
						UIText.RepositorySearchDialog_BrowseDialogMessage);
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

		skipHiddenButton = new Button(searchGroup, SWT.CHECK);
		skipHiddenButton.setLayoutData(
				new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		skipHiddenButton
				.setSelection(prefs.getBoolean(PREF_SKIP_HIDDEN, true));
		skipHiddenButton.setText(UIText.RepositorySearchDialog_SkipHidden);
		skipHiddenButton.setToolTipText(
				UIText.RepositorySearchDialog_SkipHiddenTooltip);

		skipHiddenButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				prefs.putBoolean(PREF_SKIP_HIDDEN, skipHiddenButton.getSelection());
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
		dir.addModifyListener(e -> setNeedsSearch());

		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(new RepositoryLabelProvider());

		String initialPath = prefs.get(PREF_PATH,
				RepositoryUtil.getDefaultRepositoryDir());
		dir.setText(initialPath);

		setControl(main);
		enableOk();
		if (fillSearch && searchButton.isEnabled()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				if (!getControl().isDisposed()) {
					doSearch();
				}
			});
		}
	}

	private String findGitDirsRecursive(Path root, final Set<Path> gitDirs,
			IProgressMonitor monitor, final boolean lookForNested,
			boolean skipHidden) {

		long start = System.currentTimeMillis();
		final int[] dirCount = new int[1];
		final SubMonitor m = SubMonitor.convert(monitor);
		try {
			SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
				private long lastMonitorUpdate;

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.SKIP_SUBTREE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path d,
						BasicFileAttributes attrs) throws IOException {
					dirCount[0]++;
					if (m.isCanceled()) {
						return FileVisitResult.TERMINATE;
					} else if (d == null) {
						return FileVisitResult.CONTINUE;
					} else if (isHidden(d) || isGitInternal(d)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					updateMonitor(d);
					Path resolved = resolve(d);
					if (resolved == null) {
						return FileVisitResult.CONTINUE;
					}
					if (!suppressed(resolved)) {
						gitDirs.add(resolved.toAbsolutePath());
						updateMonitor(resolved);
						if (isDotGit(resolved)) { // non-bare
							if (!lookForNested
									|| (isSameFile(d, resolved)
											&& !hasSubmodule(resolved))) {
								return FileVisitResult.SKIP_SUBTREE;
							}
						} else { // bare
							return FileVisitResult.SKIP_SUBTREE;
						}
					}
					return FileVisitResult.CONTINUE;
				}

				private boolean isHidden(@NonNull Path d)
						throws IOException {
					return skipHidden && Files.isHidden(d)
							&& !isDotGit(d);
				}

				private boolean isGitInternal(@NonNull Path d) {
					Path fileName = d.getFileName();
					if (fileName == null) {
						return false;
					}
					Path p = d.getParent();
					String n = fileName.toString();
					return p != null && isDotGit(p)
							&& !Constants.MODULES.equals(n);
				}

				private Path resolve(@NonNull Path d) {
					File f = FileKey.resolve(d.toFile(), FS.DETECTED);
					if (f == null) {
						return null;
					}
					return f.toPath();
				}

				private boolean suppressed(@NonNull Path d) {
					return !allowBare && !isDotGit(d);
				}

				private boolean isDotGit(@NonNull Path d) {
					Path fileName = d.getFileName();
					if (fileName == null) {
						return false;
					}
					return Constants.DOT_GIT.equals(fileName.toString());
				}

				private boolean isSameFile(@NonNull Path f1, @NonNull Path f2) {
					try {
						return Files.isSameFile(f1, f2);
					} catch (IOException e) {
						return false;
					}
				}

				private boolean hasSubmodule(@NonNull Path dotGit) {
					Path gitmodules = dotGit.getParent()
							.resolve(Constants.DOT_GIT_MODULES);
					Path modules = dotGit.resolve(Constants.MODULES);
					return Files.exists(gitmodules)
							&& Files.exists(modules);
				}

				private void updateMonitor(@NonNull Path d) {
					long now = System.currentTimeMillis();
					if ((now - lastMonitorUpdate) > 100L) {
						m.setWorkRemaining(100);
						m.worked(1);
						m.setTaskName(MessageFormat.format(
								UIText.RepositorySearchDialog_RepositoriesFound_message,
								Integer.valueOf(gitDirs.size()),
								d.toAbsolutePath().toString()));
						lastMonitorUpdate = now;
					}
				}
			};
			Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE, visitor);
		} catch (IOException e) {
			Activator.error(e.getMessage(), e);
		}
		long time = System.currentTimeMillis() - start;
		return formatSummary(gitDirs, dirCount, time);
	}

	private String formatSummary(final Set<Path> gitDirs, final int[] dirCount,
			long time) {
		String fmtTime = ""; //$NON-NLS-1$
		if (time < 1000) {
			fmtTime = String.format("%dms", Long.valueOf(time)); //$NON-NLS-1$
		} else if (time < 10000) {
			fmtTime = String.format("%.1fs", Double.valueOf(time / 1000.0)); //$NON-NLS-1$
		} else {
			fmtTime = String.format("%ds", Long.valueOf(time / 1000)); //$NON-NLS-1$
		}
		return MessageFormat.format(UIText.RepositorySearchDialog_SearchResult,
				Integer.valueOf(gitDirs.size()), Integer.valueOf(dirCount[0]),
				fmtTime);
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
		final Set<Path> directories = new TreeSet<>(
				CommonUtils.PATH_STRING_COMPARATOR);
		final Path file = Paths.get(dir.getText());
		final boolean lookForNested = lookForNestedButton.getSelection();
		final boolean skipHidden = skipHiddenButton.getSelection();
		if (!Files.isDirectory(file)) {
			return;
		}

		prefs.put(PREF_PATH, file.toAbsolutePath().toString());
		try {
			prefs.flush();
		} catch (BackingStoreException e1) {
			// ignore here
		}

		final TreeSet<String> validDirs = new TreeSet<>(getCheckedItems());
		final String[] summary = new String[1];
		IRunnableWithProgress action = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				monitor.beginTask(
						UIText.RepositorySearchDialog_ScanningForRepositories_message,
						IProgressMonitor.UNKNOWN);
				try {
					summary[0] = findGitDirsRecursive(file, directories, monitor,
							lookForNested, skipHidden);
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

		for (Path foundDir : directories) {
			String absolutePath = foundDir.toAbsolutePath().toString();
			if (!fExistingDirectories.contains(absolutePath)
					&& !fExistingDirectories.contains(FileUtils
							.canonicalize(foundDir.toFile())
							.getAbsolutePath())) {
				validDirs.add(absolutePath);
			} else {
				foundOld++;
			}
		}

		if (foundOld > 0) {
			String message = summary[0] + '\n'
					+ MessageFormat.format(
					UIText.RepositorySearchDialog_SomeDirectoriesHiddenMessage,
					Integer.valueOf(foundOld));
			setMessage(message, IMessageProvider.INFORMATION);
		} else if (directories.isEmpty()) {
			setMessage(summary[0], IMessageProvider.INFORMATION);
		} else {
			setMessage(summary[0]);
		}

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
		try {
			Path file = Paths.get(dir.getText());
			if (!Files.isDirectory(file)) {
				setErrorMessage(MessageFormat.format(
						UIText.RepositorySearchDialog_DirectoryNotFoundMessage,
						dir.getText()));
				searchButton.setEnabled(false);
			} else {
				searchButton.setEnabled(true);
				setErrorMessage(null);
				setMessage(
						UIText.RepositorySearchDialog_NoSearchAvailableMessage,
						IMessageProvider.INFORMATION);
			}
		} catch (InvalidPathException e) {
			setErrorMessage(MessageFormat.format(
					UIText.RepositorySearchDialog_InvalidDirectoryMessage,
					e.getLocalizedMessage()));
			searchButton.setEnabled(false);
		}
		enableOk();
	}

	private void enableOk() {
		boolean enable = fTreeViewer.getCheckedElements().length > 0;
		setPageComplete(enable);
	}
}
