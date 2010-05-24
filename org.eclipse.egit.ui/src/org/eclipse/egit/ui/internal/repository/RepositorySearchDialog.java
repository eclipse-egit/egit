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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.FilteredCheckboxTree;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.RepositoryCache;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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

	private CheckboxTreeViewer fTreeViewer;

	private Button fSelectAllButton;

	private Button fDeselectAllButton;

	private final ResourceManager fImageCache = new LocalResourceManager(JFaceResources
			.getResources());

	private final class ContentProvider implements ITreeContentProvider {

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
			// nothing
			return null;
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

	private final class RepositoryLabelProvider extends LabelProvider implements IColorProvider {

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
		newShell.setText(UIText.RepositorySearchDialog_searchRepositories);
		setTitleImage(fImageCache.createImage(UIIcons.WIZBAN_IMPORT_REPO));
	}

	@Override
	protected void okPressed() {
		fResult = new HashSet<String>();
		Object[] checked = fTreeViewer.getCheckedElements();
		for (Object o : checked) {
			fResult.add((String) o);
		}
		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		super.createDialogArea(parent);
		setTitle(UIText.RepositorySearchDialog_searchRepositories);
		setMessage(UIText.RepositorySearchDialog_searchRepositoriesMessage);
		final IEclipsePreferences prefs = new InstanceScope().getNode(Activator
				.getPluginId());

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(4, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label dirLabel = new Label(main, SWT.NONE);
		dirLabel.setText(UIText.RepositorySearchDialog_directory);
		final Text dir = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).hint(300,
				SWT.DEFAULT).applyTo(dir);

		String initialPath = prefs.get(PREF_PATH, ResourcesPlugin
				.getWorkspace().getRoot().getLocation().toOSString());

		dir.setText(initialPath);

		Button browse = new Button(main, SWT.PUSH);
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
				}
			}

		});

		fTree = new FilteredCheckboxTree(main, null, SWT.BORDER);
		fTreeViewer = fTree.getCheckboxTreeViewer();
		GridDataFactory.fillDefaults().grab(true, true).span(4, 1).minSize(0, 300).applyTo(fTree);
		fTree.setEnabled(false);

		final Button lookForNestedButton = new Button(main, SWT.CHECK);
		lookForNestedButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		lookForNestedButton
				.setSelection(prefs.getBoolean(PREF_DEEP_SEARCH, false));
		lookForNestedButton
				.setText(UIText.RepositorySearchDialog_DeepSearch_button);

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
			}

		});


		fSelectAllButton = new Button(main, SWT.NONE);
		fSelectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
				false, 1, 1));
		fSelectAllButton
				.setText(UIText.RepositorySearchDialog_SelectAll_button);
		fSelectAllButton.setEnabled(false);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fTreeViewer.setAllChecked(true);
				getButton(IDialogConstants.OK_ID).setEnabled(
						fTreeViewer.getCheckedElements().length > 0);
			}
		});

		fDeselectAllButton = new Button(main, SWT.NONE);
		fDeselectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
				false, 1, 1));
		fDeselectAllButton
				.setText(UIText.RepositorySearchDialog_DeselectAll_button);
		fDeselectAllButton.setEnabled(false);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fTreeViewer.setAllChecked(false);
				getButton(IDialogConstants.OK_ID).setEnabled(
						fTreeViewer.getCheckedElements().length > 0);
			}
		});

		// TODO this isn't the most optimal way of handling this... ideally we should have some type of delay
		// if we could use databinding an observeDelayedValue would totally work here
		dir.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				// perform the search...
				final TreeSet<String> directories = new TreeSet<String>();
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
								throws InvocationTargetException,
								InterruptedException {

							try {
								findGitDirsRecursive(file, directories,
										monitor, lookForNested);
							} catch (Exception ex) {
								Activator.getDefault().getLog().log(
										new Status(IStatus.ERROR, Activator
												.getPluginId(),
												ex.getMessage(), ex));
							}
						}
					};
					try {
						ProgressMonitorDialog pd = new ProgressMonitorDialog(
								getShell());
						pd
								.getProgressMonitor()
								.setTaskName(
										UIText.RepositorySearchDialog_ScanningForRepositories_message);
						pd.run(true, true, action);

					} catch (InvocationTargetException e1) {
						org.eclipse.egit.ui.Activator.handleError(
								UIText.RepositorySearchDialog_errorOccurred,
								e1, true);
					} catch (InterruptedException e1) {
						// ignore
					}

					boolean foundNew = false;

					for (String foundDir : directories) {
						if (!fExistingDirectories.contains(foundDir)) {
							foundNew = true;
							break;
						}
					}

					fSelectAllButton.setEnabled(foundNew);
					fDeselectAllButton.setEnabled(foundNew);
					fTree.setEnabled(directories.size() > 0);
					fTreeViewer.setInput(directories);
				}
			}
		});

		fTreeViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				if (fExistingDirectories.contains(event.getElement()))
					event.getCheckable().setChecked(event.getElement(), false);
				getButton(IDialogConstants.OK_ID).setEnabled(
						fTreeViewer.getCheckedElements().length > 0);
			}
		});

		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(new RepositoryLabelProvider());

		return main;
	}

	private void findGitDirsRecursive(File root, TreeSet<String> strings,
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
					&& RepositoryCache.FileKey.isGitRepository(child)) {
				try {
					strings.add(child.getCanonicalPath());
				} catch (IOException e) {
					// ignore here
				}
				monitor
						.setTaskName(NLS
								.bind(
										UIText.RepositorySearchDialog_RepositoriesFound_message,
										new Integer(strings.size())));
				if (!lookForNestedRepositories)
					return;
			} else if (child.isDirectory()) {
				monitor.subTask(child.getPath());
				findGitDirsRecursive(child, strings, monitor,
						lookForNestedRepositories);
			}
		}

	}

}
