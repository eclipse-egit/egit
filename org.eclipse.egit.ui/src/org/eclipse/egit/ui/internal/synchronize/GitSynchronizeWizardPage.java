/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.ide.IDE;

class GitSynchronizeWizardPage extends WizardPage {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	private CheckboxTreeViewer treeViewer;

	private Map<Repository, Set<IProject>> repositories;

	private Set<Repository> selectedRepositories = new HashSet<Repository>();

	private Set<IProject> selectedProjects = new HashSet<IProject>();

	private Map<Repository, String> selectedBranches = new HashMap<Repository, String>();

	private Image branchesImage = UIIcons.BRANCHES.createImage();

	private Image repositoryImage = UIIcons.REPOSITORY.createImage();

	private IProject[] selectProjects;

	GitSynchronizeWizardPage() {
		super(GitSynchronizeWizardPage.class.getName());
		setTitle(UIText.GitBranchSynchronizeWizardPage_title);
		setDescription(UIText.GitBranchSynchronizeWizardPage_description);
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		repositories = new HashMap<Repository, Set<IProject>>();
		for (IProject project : ROOT.getProjects()) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null) {
				Repository repository = mapping.getRepository();
				Set<IProject> set = repositories.get(repository);
				if (set == null) {
					set = new HashSet<IProject>();
					repositories.put(repository, set);
				}
				set.add(project);
			}
		}

		treeViewer = new ContainerCheckedTreeViewer(composite, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		TreeViewerColumn repositoriesColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		repositoriesColumn.getColumn().setText(
				UIText.GitBranchSynchronizeWizardPage_repositories);
		repositoriesColumn.getColumn().setImage(repositoryImage);
		repositoriesColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Repository) {
					return ((Repository) element).getDirectory()
							.getAbsolutePath();
				}
				return ((IProject) element).getName();
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof Repository) {
					return repositoryImage;
				}
				return PlatformUI.getWorkbench().getSharedImages().getImage(
						IDE.SharedImages.IMG_OBJ_PROJECT);
			}
		});

		TreeViewerColumn branchesColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		branchesColumn.getColumn().setText(UIText.GitBranchSynchronizeWizardPage_branches);
		branchesColumn.getColumn().setImage(branchesImage);
		branchesColumn.getColumn().setWidth(200);
		final ComboBoxCellEditor branchesEditor = new ComboBoxCellEditor(
				treeViewer.getTree(), new String[0]);
		branchesColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				int intValue = ((Integer) value).intValue();
				if (intValue == -1) {
					return;
				}

				CCombo combo = (CCombo) branchesEditor.getControl();
				String branch = combo.getItem(intValue);

				if (element instanceof IProject) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping((IResource) element);
					Repository repository = mapping.getRepository();
					selectedBranches.put(repository, branch);
					treeViewer.refresh(repository, true);
				} else {
					selectedBranches.put((Repository) element, branch);
					treeViewer.refresh(element, true);
				}

				validatePage();
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof IProject) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping((IResource) element);
					String branch = selectedBranches.get(mapping
							.getRepository());
					CCombo combo = (CCombo) branchesEditor.getControl();
					int index = branch == null ? 0 : combo.indexOf(branch);
					return Integer.valueOf(index);
				} else {
					String branch = selectedBranches.get(element);
					CCombo combo = (CCombo) branchesEditor.getControl();
					int index = branch == null ? 0 : combo.indexOf(branch);
					return Integer.valueOf(index);
				}
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				if (element instanceof IProject) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping((IResource) element);
					Set<String> refs = mapping.getRepository().getAllRefs()
							.keySet();
					branchesEditor.setItems(refs
							.toArray(new String[refs.size()]));
				} else {
					Set<String> refs = ((Repository) element).getAllRefs()
							.keySet();
					branchesEditor.setItems(refs
							.toArray(new String[refs.size()]));
				}
				return branchesEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		branchesColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IProject) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping((IResource) element);
					String branch = selectedBranches.get(mapping
							.getRepository());
					return branch == null ? "" : branch; //$NON-NLS-1$
				} else {
					String branch = selectedBranches.get(element);
					return branch == null ? "" : branch; //$NON-NLS-1$
				}
			}
		});

		treeViewer.setContentProvider(new ITreeContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// nothing to do
			}

			public void dispose() {
				// nothing to do
			}

			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			public boolean hasChildren(Object element) {
				if (element instanceof Repository) {
					return !repositories.get(element).isEmpty();
				}
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Repository) {
					return repositories.get(parentElement).toArray();
				}
				return new Object[0];
			}
		});

		final Object[] array = repositories.keySet().toArray();
		treeViewer.setInput(array);
		if (selectProjects == null)
			treeViewer.setCheckedElements(array);
		else
			treeViewer.setCheckedElements(selectProjects);
		repositoriesColumn.getColumn().pack();

		save();

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				selectedRepositories.clear();
				selectedProjects.clear();

				save();
				validatePage();
			}
		});

		Composite buttonsComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);
		buttonsComposite.setLayoutData(new GridData(SWT.BEGINNING,
				SWT.BEGINNING, false, false));

		Button selectAllBtn = new Button(buttonsComposite, SWT.PUSH);
		selectAllBtn.setText(UIText.GitBranchSynchronizeWizardPage_selectAll);
		selectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				treeViewer.setCheckedElements(array);
				save();
				validatePage();
			}
		});
		selectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false,
				false));

		Button deselectAllBtn = new Button(buttonsComposite, SWT.PUSH);
		deselectAllBtn.setText(UIText.GitBranchSynchronizeWizardPage_deselectAll);
		deselectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// uncheck everything
				treeViewer.setCheckedElements(new Object[0]);
				// clear all selection
				selectedRepositories.clear();
				selectedProjects.clear();
				validatePage();
			}
		});
		deselectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
				false, false));

		Dialog.applyDialogFont(composite);
		setPageComplete(false);
		setControl(composite);
	}

	@Override
	public void dispose() {
		if (branchesImage != null) {
			branchesImage.dispose();
		}
		if (repositoryImage != null) {
			repositoryImage.dispose();
		}
		super.dispose();
	}

	private void save() {
		// record any candidate repositories that should be synchronized
		for (Object grayedElement : treeViewer.getGrayedElements()) {
			selectedRepositories.add((Repository) grayedElement);
		}

		for (Object checkedElement : treeViewer.getCheckedElements()) {
			if (checkedElement instanceof Repository) {
				Repository repo = (Repository) checkedElement;
				if (selectedRepositories.add(repo)) {
					// if this repository hasn't been added yet, it implies it's
					// a checked element which means all the projects it owns
					// should be selected
					selectedProjects.addAll(repositories.get(repo));
				}
			} else {
				selectedProjects.add((IProject) checkedElement);
			}
		}
	}

	private void validatePage() {
		boolean complete = !selectedRepositories.isEmpty();
		if (complete)
			for (Repository repository : selectedRepositories)
				if (!selectedBranches.containsKey(repository)) {
					complete = false;
					break;
				}
		setPageComplete(complete);
	}

	void selectProjects(IProject[] projs) {
		this.selectProjects = projs;
	}

	Map<Repository, String> getSelectedBranches() {
		return selectedBranches;
	}

	Set<IProject> getSelectedProjects() {
		return selectedProjects;
	}

}
