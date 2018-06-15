/*******************************************************************************
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2015, Philipp Bumann <bumannp@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.clean;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * A page for the Clean wizard presenting all things to be cleaned to the user.
 */
public class CleanRepositoryPage extends WizardPage {

	private Repository repository;
	private CheckboxTableViewer cleanTable;
	private boolean cleanDirectories;
	private boolean includeIgnored;

	/**
	 * Creates a new page for the given repository.
	 * @param repository repository to clean.
	 */
	public CleanRepositoryPage(Repository repository) {
		super(UIText.CleanRepositoryPage_title);
		this.repository = repository;

		setTitle(UIText.CleanRepositoryPage_title);
		setMessage(UIText.CleanRepositoryPage_message);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout());

		final Button radioCleanFiles = new Button(main, SWT.RADIO);
		radioCleanFiles.setText(UIText.CleanRepositoryPage_cleanFiles);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(radioCleanFiles);

		final Button radioCleanDirs = new Button(main, SWT.RADIO);
		radioCleanDirs.setText(UIText.CleanRepositoryPage_cleanDirs);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(radioCleanDirs);

		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cleanDirectories = radioCleanDirs.getSelection();
				updateCleanItems();
			}
		};

		radioCleanFiles.addSelectionListener(listener);
		radioCleanDirs.addSelectionListener(listener);

		radioCleanFiles.setSelection(true);

		final Image fileImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FILE);
		final Image dirImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FOLDER);

		cleanTable = CheckboxTableViewer.newCheckList(main, SWT.BORDER);
		cleanTable.setContentProvider(ArrayContentProvider.getInstance());
		cleanTable.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if(!(element instanceof String))
					return null;

				if(((String)element).endsWith("/")) //$NON-NLS-1$
					return dirImage;
				else
					return fileImage;
			}
		});
		setPageComplete(false);
		cleanTable.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updatePageComplete();
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).applyTo(cleanTable.getControl());

		Composite lowerComp = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lowerComp);
		lowerComp.setLayout(new GridLayout(3, false));

		final Button checkIncludeIgnored = new Button(lowerComp, SWT.CHECK);
		checkIncludeIgnored.setText(UIText.CleanRepositoryPage_includeIgnored);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(checkIncludeIgnored);
		checkIncludeIgnored.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeIgnored = checkIncludeIgnored.getSelection();
				updateCleanItems();
			}
		});

		Button selAll = new Button(lowerComp, SWT.PUSH);
		selAll.setText(UIText.WizardProjectsImportPage_selectAll);
		GridDataFactory.defaultsFor(selAll).applyTo(selAll);

		Button selNone = new Button(lowerComp, SWT.PUSH);
		selNone.setText(UIText.WizardProjectsImportPage_deselectAll);
		GridDataFactory.defaultsFor(selNone).applyTo(selNone);

		selAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (cleanTable.getInput() instanceof Set<?>) {
					Set<?> input = (Set<?>) cleanTable.getInput();
					cleanTable.setCheckedElements(input.toArray());
					updatePageComplete();
				}
			}
		});

		selNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cleanTable.setCheckedElements(new Object[0]);
				updatePageComplete();
			}
		});

		setControl(main);
	}

	private void updatePageComplete() {
		boolean hasCheckedElements = cleanTable.getCheckedElements().length != 0;
		setPageComplete(hasCheckedElements);
		if (hasCheckedElements)
			setMessage(null, NONE);
		else
			setMessage(UIText.CleanRepositoryPage_SelectFilesToClean, INFORMATION);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if(visible)
			getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateCleanItems();
				}
			});
	}

	private void updateCleanItems() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(UIText.CleanRepositoryPage_findingItems, IProgressMonitor.UNKNOWN);

					Git git = Git.wrap(repository);
					CleanCommand command = git.clean().setDryRun(true);
					command.setCleanDirectories(cleanDirectories);
					command.setIgnore(!includeIgnored);
					try {
						final Set<String> paths = command.call();

						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								cleanTable.setInput(paths);
							}
						});
					} catch (GitAPIException ex) {
						Activator.logError("cannot call clean command!", ex); //$NON-NLS-1$
					}

					monitor.done();
				}
			});
			updatePageComplete();
		} catch (InvocationTargetException e) {
			Activator.logError("Unexpected exception while finding items to clean", e); //$NON-NLS-1$
			clearPage();
		} catch (InterruptedException e) {
			clearPage();
		}
	}

	private void clearPage() {
		cleanTable.setInput(null);
	}

	/**
	 * Retrieves the items that the user chose to clean.
	 * @return the items to clean.
	 */
	public Set<String> getItemsToClean() {
		Set<String> result = new TreeSet<>();
		for(Object ele : cleanTable.getCheckedElements()) {
			String str = ele.toString();

			if(str.endsWith("/")) //$NON-NLS-1$
				result.add(str.substring(0, str.length() - 1));
			else
				result.add(str);
		}

		return result;
	}

	/**
	 * Do the cleaning with the selected values.
	 */
	public void finish() {
		try {
			final Set<String> itemsToClean = getItemsToClean();

			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					SubMonitor subMonitor = SubMonitor.convert(monitor,
							UIText.CleanRepositoryPage_cleaningItems, 1);

					Git git = Git.wrap(repository);
					CleanCommand command = git.clean().setDryRun(false);
					command.setCleanDirectories(cleanDirectories);
					command.setIgnore(!includeIgnored);
					command.setPaths(itemsToClean);
					try {
						command.call();
					} catch (GitAPIException ex) {
						Activator.logError("cannot call clean command!", ex); //$NON-NLS-1$
					}

					try {
						IProject[] projects = ProjectUtil.getProjectsContaining(repository, itemsToClean);
						ResourcesPlugin.getWorkspace().run(
								pm -> ProjectUtil.refreshResources(projects,
										pm),
								null, IWorkspace.AVOID_UPDATE,
								subMonitor.newChild(1));
					} catch (CoreException e) {
						// could not refresh... not a "real" problem
					}
				}
			});
		} catch (Exception e) {
			Activator.logError("Unexpected exception while cleaning", e); //$NON-NLS-1$
		}
	}

}
