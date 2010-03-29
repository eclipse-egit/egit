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
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * Searches for Git directories under a path that can be selected by the user
 * TODO String externalization
 */
public class RepositorySearchDialog extends Dialog {

	private final Set<String> existingRepositoryDirs = new HashSet<String>();

	private final String myInitialPath;

	private Set<String> result;

	CheckboxTableViewer tv;

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
	 * @param initialPath
	 *            the initial path
	 * @param existingDirs
	 */
	protected RepositorySearchDialog(Shell parentShell, String initialPath,
			Collection<String> existingDirs) {
		super(parentShell);
		this.existingRepositoryDirs.addAll(existingDirs);
		this.myInitialPath = initialPath;
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

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));

		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label dirLabel = new Label(main, SWT.NONE);
		dirLabel.setText(UIText.RepositorySearchDialog_directory);
		final Text dir = new Text(main, SWT.NONE);
		if (myInitialPath != null)
			dir.setText(myInitialPath);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(dir);

		Button browse = new Button(main, SWT.PUSH);
		browse.setText(UIText.RepositorySearchDialog_browse);
		browse.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setFilterPath(dir.getText());
				String directory = dd.open();
				if (directory != null) {
					dir.setText(directory);
				}
			}

		});

		Button search = new Button(main, SWT.PUSH);
		search.setText(UIText.RepositorySearchDialog_search);
		GridDataFactory.fillDefaults().align(SWT.LEAD, SWT.CENTER).span(3, 1)
				.applyTo(search);

		tv = CheckboxTableViewer.newCheckList(main, SWT.NONE);
		Table tab = tv.getTable();
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(tab);

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
				if (file.exists()) {
					IRunnableWithProgress action = new IRunnableWithProgress() {

						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							RepositoriesView.recurseDir(file, directories,
									monitor);
						}
					};
					try {
						ProgressMonitorDialog pd = new ProgressMonitorDialog(
								getShell());
						pd.run(true, true, action);

					} catch (InvocationTargetException e1) {
						MessageDialog.openError(getShell(),
								UIText.RepositorySearchDialog_errorOccurred, e1
										.getCause().getMessage());
					} catch (InterruptedException e1) {
						// ignore
					}

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

}
