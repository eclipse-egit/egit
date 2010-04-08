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
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Searches for Git directories under a path that can be selected by the user
 */
public class RepositorySearchDialog extends Dialog {

	private static final String PREF_DEEP_SEARCH = "RepositorySearchDialogDeepSearch"; //$NON-NLS-1$

	private static final String PREF_PATH = "RepositorySearchDialogSearchPath"; //$NON-NLS-1$

	private final Set<String> existingRepositoryDirs = new HashSet<String>();

	private Set<String> result;

	CheckboxTableViewer tv;

	private Button btnToggleSelect;

	private Table tab;

	private final class ContentProvider implements IStructuredContentProvider {

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

	}

	private final class LabelProvider extends BaseLabelProvider implements
			ITableLabelProvider, IColorProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			return element.toString();
		}

		public Color getBackground(Object element) {
			return null;
		}

		public Color getForeground(Object element) {
			if (existingRepositoryDirs.contains(element))
				return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);

			return null;
		}

	}

	/**
	 * @param parentShell
	 * @param existingDirs
	 */
	protected RepositorySearchDialog(Shell parentShell,
			Collection<String> existingDirs) {
		super(parentShell);
		this.existingRepositoryDirs.addAll(existingDirs);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
	}

	/**
	 *
	 * @return the directories
	 */
	public Set<String> getDirectories() {
		return result;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RepositorySearchDialog_searchRepositories);
	}

	@Override
	protected void okPressed() {
		result = new HashSet<String>();
		Object[] checked = tv.getCheckedElements();
		for (Object o : checked) {
			result.add((String) o);
		}
		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

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

		// we fill the room under the "Directory" label
		new Label(main, SWT.NONE);

		final Button btnLookForNested = new Button(main, SWT.CHECK);
		btnLookForNested.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		btnLookForNested
				.setSelection(prefs.getBoolean(PREF_DEEP_SEARCH, false));
		btnLookForNested
				.setText(UIText.RepositorySearchDialog_DeepSearch_button);

		btnLookForNested.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				prefs.putBoolean(PREF_DEEP_SEARCH, btnLookForNested
						.getSelection());
				try {
					prefs.flush();
				} catch (BackingStoreException e1) {
					// ignore
				}
			}

		});

		Button search = new Button(main, SWT.PUSH);
		search.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		search.setText(UIText.RepositorySearchDialog_search);

		tv = CheckboxTableViewer.newCheckList(main, SWT.BORDER);
		tab = tv.getTable();
		tab.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		tab.setEnabled(false);

		btnToggleSelect = new Button(main, SWT.NONE);
		btnToggleSelect.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false,
				false, 1, 1));
		btnToggleSelect
				.setText(UIText.RepositorySearchDialog_ToggleSelection_button);
		btnToggleSelect.setEnabled(false);
		btnToggleSelect.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				for (int i = 0; i < tab.getItemCount(); i++) {
					if (!existingRepositoryDirs.contains(tv.getElementAt(i)))
						tv.setChecked(tv.getElementAt(i), !tv.getChecked(tv
								.getElementAt(i)));
				}
				getButton(IDialogConstants.OK_ID).setEnabled(
						tv.getCheckedElements().length > 0);
			}
		});

		tv.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				if (existingRepositoryDirs.contains(event.getElement()))
					event.getCheckable().setChecked(event.getElement(), false);
				getButton(IDialogConstants.OK_ID).setEnabled(
						tv.getCheckedElements().length > 0);
			}
		});

		tv.setContentProvider(new ContentProvider());
		tv.setLabelProvider(new LabelProvider());

		search.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final TreeSet<String> directories = new TreeSet<String>();
				final File file = new File(dir.getText());
				final boolean lookForNested = btnLookForNested.getSelection();
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
						MessageDialog.openError(getShell(),
								UIText.RepositorySearchDialog_errorOccurred, e1
										.getCause().getMessage());
					} catch (InterruptedException e1) {
						// ignore
					}

					boolean foundNew = false;

					for (String foundDir : directories) {
						if (!existingRepositoryDirs.contains(foundDir)) {
							foundNew = true;
							break;
						}
					}

					btnToggleSelect.setEnabled(foundNew);
					tab.setEnabled(directories.size() > 0);
					tv.setInput(directories);
				}
			}

		});

		return main;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		// disable the OK button until the user selects something
		Control bar = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return bar;
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
