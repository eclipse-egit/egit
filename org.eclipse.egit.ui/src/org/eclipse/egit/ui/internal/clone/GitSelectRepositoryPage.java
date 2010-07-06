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
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.RepositoryUtil;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.RepositorySearchDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Select a repository, add or clone
 */
public class GitSelectRepositoryPage extends WizardPage {

	private final RepositoryUtil util;

	private TableViewer tv;

	private Button addRepo;

	private Button cloneRepo;

	/**
	 *
	 */
	public GitSelectRepositoryPage() {
		super(GitSelectRepositoryPage.class.getName());
		setTitle(UIText.GitSelectRepositoryPage_PageTitle);
		setMessage(UIText.GitSelectRepositoryPage_PageMessage);
		util = Activator.getDefault().getRepositoryUtil();
	}

	/**
	 * @return the repository
	 */
	public FileRepository getRepository() {
		Object obj = ((IStructuredSelection) tv.getSelection())
				.getFirstElement();
		if (obj == null)
			return null;
		return ((RepositoryTreeNode) obj).getRepository();
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).applyTo(
				main);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		tv = new TableViewer(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		tv.setContentProvider(new RepositoriesViewContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTable());
		tv.setLabelProvider(new RepositoriesViewLabelProvider());

		Composite tb = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tb);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(tb);

		cloneRepo = new Button(tb, SWT.PUSH);
		cloneRepo.setText(UIText.GitSelectRepositoryPage_CloneButton);
		cloneRepo.setToolTipText(UIText.GitSelectRepositoryPage_CloneTooltip);

		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL,
				SWT.BEGINNING).applyTo(cloneRepo);

		cloneRepo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<String> dirsBefore = util.getConfiguredRepositories();
				WizardDialog dlg = new WizardDialog(getShell(),
						new GitCloneWizard());

				if (dlg.open() == Window.OK) {
					List<String> dirsAfter = util.getConfiguredRepositories();
					if (!dirsBefore.containsAll(dirsAfter)) {
						tv.setInput(dirsAfter);
						for (String dir : dirsAfter) {
							if (!dirsBefore.contains(dir)) {
								try {
									RepositoryNode node = new RepositoryNode(
											null, new FileRepository(new File(dir)));
									tv.setSelection(new StructuredSelection(
											node));
								} catch (IOException e1) {
									Activator.handleError(e1.getMessage(), e1,
											false);
								}
							}
						}
					}
					checkPage();
				}
			}
		});

		addRepo = new Button(tb, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL,
				SWT.BEGINNING).applyTo(addRepo);
		addRepo.setText(UIText.GitSelectRepositoryPage_AddButton);
		addRepo.setToolTipText(UIText.GitSelectRepositoryPage_AddTooltip);
		addRepo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				List<String> configuredDirs = util.getConfiguredRepositories();
				RepositorySearchDialog dlg = new RepositorySearchDialog(
						getShell(), configuredDirs);
				if (dlg.open() == Window.OK && dlg.getDirectories().size() > 0) {

					Set<String> dirs = dlg.getDirectories();
					for (String dir : dirs)
						util.addConfiguredRepository(new File(dir));

					tv.setInput(util.getConfiguredRepositories());
					checkPage();

				}
			}

		});

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		tv.setInput(util.getConfiguredRepositories());

		// we need to select at least a repository to become complete
		setPageComplete(false);
		Dialog.applyDialogFont(main);
		setControl(main);

	}

	private void checkPage() {
		setErrorMessage(null);
		try {
			if (((List) tv.getInput()).isEmpty()) {
				setErrorMessage(UIText.GitSelectRepositoryPage_NoRepoFoundMessage);
				return;
			}

			if (tv.getSelection().isEmpty()) {
				setErrorMessage(UIText.GitSelectRepositoryPage_PleaseSelectMessage);
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

}
