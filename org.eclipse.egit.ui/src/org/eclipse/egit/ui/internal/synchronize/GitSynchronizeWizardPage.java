/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

class GitSynchronizeWizardPage extends WizardPage {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace()
			.getRoot();

	private boolean forceFetch;

	private boolean shouldIncludeLocal = false;

	private TreeViewer treeViewer;

	private final Map<Repository, Set<IProject>> resources = new HashMap<>();

	private final Map<Repository, String> selectedBranches = new HashMap<>();

	private final Image branchImage = UIIcons.BRANCH.createImage();

	private final Image repositoryImage = UIIcons.REPOSITORY.createImage();

	GitSynchronizeWizardPage() {
		super(GitSynchronizeWizardPage.class.getName());
		setTitle(UIText.GitBranchSynchronizeWizardPage_title);
		setMessage(UIText.GitBranchSynchronizeWizardPage_description, WARNING);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		for (IProject project : ROOT.getProjects()) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (repositoryMapping == null)
				continue;
			Repository repo = repositoryMapping.getRepository();
			Set<IProject> projects = resources.get(repo);
			if (projects == null) {
				projects = new HashSet<>();
				resources.put(repo, projects);
			}
			projects.add(project);
		}

		treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.FULL_SELECTION);
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		TreeViewerColumn projectsColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		projectsColumn.getColumn().setText(
				UIText.GitBranchSynchronizeWizardPage_repository);
		projectsColumn.getColumn().setImage(repositoryImage);
		projectsColumn.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof Repository) {
					Repository repo = (Repository) element;
					String descr = ""; //$NON-NLS-1$
					try {
						descr += " [" + repo.getBranch() + "]";//$NON-NLS-1$ //$NON-NLS-2$
					} catch (IOException e) {
						Activator.logError(e.getMessage(), e);
					}

					Color decorationsColor = JFaceResources.getColorRegistry()
							.get(JFacePreferences.DECORATIONS_COLOR);

					String repoName = repo.getWorkTree().getName();
					int repoNameLen = repoName.length();
					StyleRange styleRange = new StyleRange(repoNameLen,
							repoNameLen + descr.length(), decorationsColor,
							null);

					cell.setImage(repositoryImage);
					cell.setText(repoName + descr);
					cell.setStyleRanges(new StyleRange[] { styleRange });
				}

				super.update(cell);
			}
		});

		TreeViewerColumn dstColumn = new TreeViewerColumn(treeViewer, SWT.LEAD);
		dstColumn.getColumn().setText(
				UIText.GitBranchSynchronizeWizardPage_destination);
		dstColumn.getColumn().setImage(branchImage);
		dstColumn.getColumn().setWidth(200);
		final ComboBoxCellEditor branchesEditor = new ComboBoxCellEditor(
				treeViewer.getTree(), new String[0]);
		branchesEditor
				.setActivationStyle(ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION
						| ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
		((CCombo) branchesEditor.getControl()).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CCombo combo = (CCombo) e.widget;
				TreeSelection sel = (TreeSelection) treeViewer.getSelection();
				int selectedIdx = combo.getSelectionIndex();
				Repository repo = (Repository) sel.getFirstElement();

				if (selectedIdx != -1) {
					selectedBranches.put(repo, combo.getItem(selectedIdx));
					setPageComplete(true);
				} else {
					selectedBranches.put(repo, null);
					setPageComplete(false);
				}
			}
		});
		dstColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				int intValue = ((Integer) value).intValue();
				if (intValue == -1)
					return;

				CCombo combo = (CCombo) branchesEditor.getControl();
				String branch = combo.getItem(intValue);

				selectedBranches.put((Repository) element, branch);
				treeViewer.refresh(element, true);

				validatePage();
			}

			@Override
			protected Object getValue(Object element) {
				String branch = selectedBranches.get(element);
				CCombo combo = (CCombo) branchesEditor.getControl();
				int index = branch == null ? 0 : combo.indexOf(branch);
				return Integer.valueOf(index);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				Repository repo = (Repository) element;
				List<String> refNames = new LinkedList<>();
				List<Ref> refs;
				try {
					refs = repo.getRefDatabase()
							.getRefsByPrefix(RefDatabase.ALL);
				} catch (IOException e) {
					refs = Collections.emptyList();
				}
				for (Ref ref : refs) {
					refNames.add(ref.getName());
				}

				List<Ref> additionalRefs;
				try {
					additionalRefs = repo.getRefDatabase().getAdditionalRefs();
				} catch (IOException e) {
					additionalRefs = Collections.emptyList();
				}
				for (Ref ref : additionalRefs) {
					refNames.add(ref.getName());
				}

				Collections.sort(refNames,
						CommonUtils.STRING_ASCENDING_COMPARATOR);

				branchesEditor.setItems(refNames.toArray(new String[0]));

				return branchesEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		dstColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String branch = selectedBranches.get(element);
				return branch == null ? "" : branch; //$NON-NLS-1$
			}
		});

		treeViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// nothing to do
			}

			@Override
			public void dispose() {
				// nothing to do
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return new Object[0];
			}
		});

		List<Repository> repositoriesList = new ArrayList<>(
				resources.keySet());
		Collections.sort(repositoriesList, new Comparator<Repository>() {
			@Override
			public int compare(Repository o1, Repository o2) {
				String name1 = o1.getWorkTree().getName();
				String name2 = o2.getWorkTree().getName();

				return name1.compareToIgnoreCase(name2);
			}
		});

		treeViewer.setInput(repositoriesList
				.toArray(new Repository[0]));
		projectsColumn.getColumn().pack();

		Composite buttonsComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout(4, false);
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);
		buttonsComposite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		final Button fetchChanges = new Button(buttonsComposite, SWT.CHECK);
		fetchChanges
		.setText(UIText.GitBranchSynchronizeWizardPage_fetchChangesFromRemote);
		fetchChanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				forceFetch = fetchChanges.getSelection();
			}
		});
		fetchChanges.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		fetchChanges.setSelection(Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH));

		final Button includeLocal = new Button(buttonsComposite, SWT.CHECK);
		includeLocal
				.setText(UIText.GitBranchSynchronizeWizardPage_includeUncommitedChanges);
		includeLocal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shouldIncludeLocal = includeLocal.getSelection();
			}
		});
		includeLocal.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		Dialog.applyDialogFont(composite);
		setPageComplete(false);
		setControl(composite);
	}

	@Override
	public void dispose() {
		if (branchImage != null)
			branchImage.dispose();

		if (repositoryImage != null)
			repositoryImage.dispose();

		super.dispose();
	}

	private void validatePage() {
		setPageComplete(!selectedBranches.isEmpty());
	}

	Map<Repository, String> getSelectedBranches() {
		return selectedBranches;
	}

	Set<IProject> getSelectedProjects() {
		Set<IProject> projects = new HashSet<>();
		for (Repository repo : selectedBranches.keySet())
			projects.addAll(resources.get(repo));

		return projects;
	}

	boolean shouldIncludeLocal() {
		return shouldIncludeLocal;
	}

	boolean forceFetch() {
		return forceFetch;
	}

}
