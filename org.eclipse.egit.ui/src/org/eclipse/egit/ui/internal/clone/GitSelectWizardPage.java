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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Asks the user to select a wizard and what to do with the imported projects
 * (automatic/manual/no share)
 */
public class GitSelectWizardPage extends WizardPage {
	/** */
	public static final int EXISTING_PROJECTS_WIZARD = 0;

	/** */
	public static final int NEW_WIZARD = 1;

	/** */
	public static final int GENERAL_WIZARD = 2;

	// TODO check if we need/can support Import... wizard
	// see also remarks in GitCreateProjectViaWizardWizard

	private final String PREF_WIZ = getName() + "WizardSel"; //$NON-NLS-1$

	private Button importExisting;

	private Button newProjectWizard;

	private Button generalWizard;

	private TreeViewer tv;

	private final Repository initialRepository;

	private final String initialPath;

	private int wizardSelection = EXISTING_PROJECTS_WIZARD;

	/**
	 * Default constructor
	 */
	public GitSelectWizardPage() {
		super(GitSelectWizardPage.class.getName());
		setTitle(UIText.GitImportWithDirectoriesPage_PageTitle);
		setMessage(UIText.GitImportWithDirectoriesPage_PageMessage);
		initialRepository = null;
		initialPath = null;
	}

	/**
	 * Default constructor
	 *
	 * @param repository
	 * @param path
	 */
	public GitSelectWizardPage(Repository repository, String path) {
		super(GitSelectWizardPage.class.getName());
		setTitle(UIText.GitImportWithDirectoriesPage_PageTitle);
		setMessage(UIText.GitImportWithDirectoriesPage_PageMessage);
		initialRepository = repository;
		initialPath = path;
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
			return node.getRepository().getWorkTree().getPath();
		return null;
	}

	/**
	 * @param repo
	 */
	public void setRepository(Repository repo) {
		List<WorkingDirNode> input = new ArrayList<>();
		if (repo != null)
			input.add(new WorkingDirNode(null, repo));
		tv.setInput(input);
		// expand root node
		tv.expandToLevel(2);
		// select the working directory as default
		tv.setSelection(new StructuredSelection(input.get(0)));
	}

	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NO_RADIO_GROUP);

		main.setLayout(new GridLayout(1, false));

		SelectionListener sl = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tv.getTree().setEnabled(!newProjectWizard.getSelection());
				if (importExisting.getSelection())
					wizardSelection = EXISTING_PROJECTS_WIZARD;
				else if (newProjectWizard.getSelection())
					wizardSelection = NEW_WIZARD;
				else if (generalWizard.getSelection())
					wizardSelection = GENERAL_WIZARD;
				else
					wizardSelection = EXISTING_PROJECTS_WIZARD;
				checkPage();
			}
		};

		Group wizardType = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(wizardType);
		wizardType.setText(UIText.GitSelectWizardPage_ProjectCreationHeader);
		wizardType.setLayout(new GridLayout(1, false));

		importExisting = new Button(wizardType, SWT.RADIO);
		importExisting.setText(UIText.GitSelectWizardPage_ImportExistingButton);
		importExisting.addSelectionListener(sl);

		newProjectWizard = new Button(wizardType, SWT.RADIO);
		newProjectWizard
				.setText(UIText.GitSelectWizardPage_UseNewProjectsWizardButton);
		newProjectWizard.addSelectionListener(sl);

		generalWizard = new Button(wizardType, SWT.RADIO);
		generalWizard.setText(UIText.GitSelectWizardPage_ImportAsGeneralButton);
		generalWizard.addSelectionListener(sl);

		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		try {
			wizardSelection = settings.getInt(PREF_WIZ);
		} catch (NumberFormatException e) {
			wizardSelection = EXISTING_PROJECTS_WIZARD;
		}
		switch (wizardSelection) {
		default:
		case EXISTING_PROJECTS_WIZARD:
			importExisting.setSelection(true);
			break;
		case GENERAL_WIZARD:
			generalWizard.setSelection(true);
			break;
		case NEW_WIZARD:
			newProjectWizard.setSelection(true);
			break;

		}

		tv = new TreeViewer(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		RepositoriesViewContentProvider cp = new RepositoriesViewContentProvider();
		tv.setContentProvider(cp);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200)
				.applyTo(tv.getTree());
		tv.setLabelProvider(new RepositoriesViewLabelProvider());

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		if (initialRepository != null) {
			List<WorkingDirNode> input = new ArrayList<>();
			WorkingDirNode node = new WorkingDirNode(null, initialRepository);
			input.add(node);
			tv.setInput(input);
			// expand root node
			tv.expandToLevel(2);
			// select the working directory as default
			if (initialPath == null)
				tv.setSelection(new StructuredSelection(input.get(0)));
			else {
				RepositoryTreeNode parentNode = node;

				IPath fullPath = new Path(initialPath);
				IPath workdirPath = new Path(initialRepository.getWorkTree()
						.getPath());
				if (workdirPath.isPrefixOf(fullPath)) {
					IPath relPath = fullPath.removeFirstSegments(workdirPath
							.segmentCount());
					for (String segment : relPath.segments()) {
						for (Object child : cp.getChildren(parentNode)) {
							if (child instanceof FolderNode) {
								FolderNode childFolder = (FolderNode) child;
								if (childFolder.getObject().getName().equals(
										segment)) {
									parentNode = childFolder;
									break;
								}
							}
						}
					}
					tv.setSelection(new StructuredSelection(parentNode));
				}
			}
		}
		tv.getTree().setEnabled(!newProjectWizard.getSelection());
		Dialog.applyDialogFont(main);
		setControl(main);

	}

	/**
	 * @return the wizard selection
	 */
	public int getWizardSelection() {
		return wizardSelection;
	}

	/**
	 * check routine
	 */
	protected void checkPage() {

		// we save the selected radio button in the preferences
		IDialogSettings settings = Activator.getDefault().getDialogSettings();

		settings.put(PREF_WIZ, getWizardSelection());

		setErrorMessage(null);

		if (newProjectWizard.getSelection()) {
			setPageComplete(true);
			return;
		}

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
