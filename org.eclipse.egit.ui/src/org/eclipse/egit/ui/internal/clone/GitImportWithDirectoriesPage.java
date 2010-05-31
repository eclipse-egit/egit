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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Select a wizard and a directory from a Git Working Directory
 */
public class GitImportWithDirectoriesPage extends GitSelectWizardPage {

	private TreeViewer tv;

	/**
	 *
	 */
	public GitImportWithDirectoriesPage() {
		super();
		setTitle(UIText.GitImportWithDirectoriesPage_PageTitle);
		setMessage(UIText.GitImportWithDirectoriesPage_PageMessage);
	}

	/**
	 * @param repo
	 */
	public void setRepository(Repository repo) {
		List<WorkingDirNode> input = new ArrayList<WorkingDirNode>();
		if (repo != null)
			input.add(new WorkingDirNode(null, repo));
		tv.setInput(input);
		// select the working directory as default
		tv.setSelection(new StructuredSelection(input.get(0)));
	}

	public void createControl(Composite parent) {
		super.createControl(parent);

		Composite main = (Composite) getControl();

		tv = new TreeViewer(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		tv.setContentProvider(new RepositoriesViewContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTree());
		tv.setLabelProvider(new RepositoriesViewLabelProvider());

		SelectionListener sl = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				tv.getTree().setEnabled(!newProjectWizard.getSelection());
			}

		};

		generalWizard.addSelectionListener(sl);
		importExisting.addSelectionListener(sl);
		newProjectWizard.addSelectionListener(sl);

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		checkPage();
		setControl(main);
	}

	/**
	 * @return the selected path
	 */
	public String getPath() {
		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
		RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
		if (node != null && node.getType() == RepositoryTreeNodeType.FOLDER)
			return ((File) node.getObject()).getPath();
		if (node != null && node.getType() == RepositoryTreeNodeType.WORKINGDIR)
			return node.getRepository().getWorkDir().getPath();
		return null;
	}

	protected void checkPage() {
		super.checkPage();
		if (getErrorMessage() != null)
			return;

		if (newProjectWizard.getSelection())
			return;

		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
		try {
			if (sel.isEmpty()) {
				setErrorMessage(UIText.GitImportWithDirectoriesPage_SelectFolderMessage);
				return;
			}
			RepositoryTreeNode node = (RepositoryTreeNode) sel
					.getFirstElement();
			if (node.getType() != RepositoryTreeNodeType.FOLDER
					&& node.getType() != RepositoryTreeNodeType.WORKINGDIR) {
				setErrorMessage(UIText.GitImportWithDirectoriesPage_SelectFolderMessage);
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

}
