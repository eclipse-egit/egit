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
import java.util.Set;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.RepositorySearchDialog;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Select a repository, add or clone
 */
public class GitSelectRepositoryPage extends WizardPage {

	private TableViewer tv;

	private ToolItem addRepo;

	private ToolItem cloneRepo;

	/**
	 *
	 */
	public GitSelectRepositoryPage() {
		super(GitSelectRepositoryPage.class.getName());
		setTitle(UIText.GitSelectRepositoryPage_PageTitle);
		setMessage(UIText.GitSelectRepositoryPage_PageMessage);
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		Object obj = ((IStructuredSelection) tv.getSelection())
				.getFirstElement();
		if (obj == null)
			return null;
		return ((RepositoryTreeNode) obj).getRepository();
	}

	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		tv = new TableViewer(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		tv.setContentProvider(new RepositoriesViewContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTable());
		new RepositoriesViewLabelProvider(tv);

		ToolBar tb = new ToolBar(main, SWT.HORIZONTAL);

		cloneRepo = new ToolItem(tb, SWT.PUSH);
		cloneRepo.setText(UIText.GitSelectRepositoryPage_CloneButton);
		cloneRepo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog dlg = new WizardDialog(getShell(),
						new GitCloneWizard());

				if (dlg.open() == Window.OK) {
					try {
						tv.setInput(RepositoriesView
								.getRepositoriesFromDirs(null));
						checkPage();
					} catch (InterruptedException e1) {
						// ignore
					}
				}
			}

		});

		addRepo = new ToolItem(tb, SWT.PUSH);
		addRepo.setText(UIText.GitSelectRepositoryPage_AddButton);
		addRepo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				RepositorySearchDialog dlg = new RepositorySearchDialog(
						getShell(), RepositoriesView.getDirs());
				if (dlg.open() == Window.OK && dlg.getDirectories().size() > 0) {
					try {
						Set<String> dirs = dlg.getDirectories();
						for (String dir : dirs)
							RepositoriesView.addDir(new File(dir));

						tv.setInput(RepositoriesView
								.getRepositoriesFromDirs(null));
					} catch (InterruptedException e1) {
						// ignore
					}
				}
			}

		});

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		try {
			tv.setInput(RepositoriesView.getRepositoriesFromDirs(null));
			checkPage();
		} catch (InterruptedException e) {
			// ignore
		}
		setControl(main);

	}

	private void checkPage() {
		setPageComplete(!tv.getSelection().isEmpty());
	}

}
