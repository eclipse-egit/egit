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

import static org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT;

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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Ref;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

class GitSynchronizeWizardPage extends WizardPage {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	private boolean shouldIncludeLocal = false;

	private CheckboxTreeViewer treeViewer;

	private Map<IProject, Repository> projects;

	private Set<IProject> selectedProjects = new HashSet<IProject>();

	private Map<Repository, String> selectedBranches = new HashMap<Repository, String>();

	private Image branchImage = UIIcons.BRANCH.createImage();

	private Image projectImage = PlatformUI.getWorkbench().getSharedImages()
			.getImage(IMG_OBJ_PROJECT);

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

		projects = new HashMap<IProject, Repository>();
		for (IProject project : ROOT.getProjects()) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null)
				projects.put(project, mapping.getRepository());
		}

		treeViewer = new ContainerCheckedTreeViewer(composite, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		TreeViewerColumn prjectsColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		prjectsColumn.getColumn().setText(
				UIText.GitBranchSynchronizeWizardPage_projects);
		prjectsColumn.getColumn().setImage(projectImage);
		prjectsColumn.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof IProject) {
					IProject project = (IProject) element;
					String projectName = project.getName();
					int projNameLen = projectName.length();

					Repository repo = projects.get(project);
					String descr = " [" + repo.getWorkTree().getName(); //$NON-NLS-1$
					try {
						descr +=  " " + repo.getBranch() + "]";//$NON-NLS-1$ //$NON-NLS-2$
					} catch (IOException e) {
						Activator.logError(e.getMessage(), e);
						descr += "]"; //$NON-NLS-1$
					}

					Color decorationsColor = JFaceResources.getColorRegistry()
							.get(JFacePreferences.DECORATIONS_COLOR);

					StyleRange styleRange = new StyleRange(projNameLen,
							projNameLen + descr.length(), decorationsColor,
							null);

					cell.setImage(projectImage);
					cell.setText(projectName + descr);
					cell.setStyleRanges(new StyleRange[] {styleRange});
				}

				super.update(cell);
			}
		});

		TreeViewerColumn dstColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		dstColumn.getColumn().setText(UIText.GitBranchSynchronizeWizardPage_destination);
		dstColumn.getColumn().setImage(branchImage);
		dstColumn.getColumn().setWidth(200);
		final ComboBoxCellEditor branchesEditor = new ComboBoxCellEditor(
				treeViewer.getTree(), new String[0]);
		dstColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				int intValue = ((Integer) value).intValue();
				if (intValue == -1)
					return;

				CCombo combo = (CCombo) branchesEditor.getControl();
				String branch = combo.getItem(intValue);

				RepositoryMapping mapping = RepositoryMapping
						.getMapping((IResource) element);
				Repository repository = mapping.getRepository();
				selectedBranches.put(repository, branch);
				treeViewer.refresh(element, true);

				validatePage();
			}

			@Override
			protected Object getValue(Object element) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping((IResource) element);
				String branch = selectedBranches.get(mapping
						.getRepository());
				CCombo combo = (CCombo) branchesEditor.getControl();
				int index = branch == null ? 0 : combo.indexOf(branch);

				return Integer.valueOf(index);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping((IResource) element);
				Repository repo = mapping.getRepository();
				List<String> refs = new LinkedList<String>(repo.getAllRefs()
						.keySet());

				List<Ref> additionalRefs;
				try {
					additionalRefs = repo.getRefDatabase().getAdditionalRefs();
				} catch (IOException e) {
					additionalRefs = null;
				}
				if (additionalRefs != null)
					for (Ref ref : additionalRefs)
						refs.add(ref.getName());

				Collections.sort(refs, CommonUtils.STRING_ASCENDING_COMPARATOR);

				branchesEditor.setItems(refs.toArray(new String[refs.size()]));

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
				RepositoryMapping mapping = RepositoryMapping
						.getMapping((IResource) element);
				String branch = selectedBranches.get(mapping
						.getRepository());
				return branch == null ? "" : branch; //$NON-NLS-1$
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
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getChildren(Object parentElement) {
				return new Object[0];
			}
		});

		List<IProject> projectsList = new ArrayList<IProject>(projects.keySet());
		Collections.sort(projectsList, new Comparator<IProject>() {
			public int compare(IProject o1, IProject o2) {
				return o2.getName().compareTo(o1.getName());
			}
		});

		final Object[] array = projectsList.toArray();
		treeViewer.setInput(array);
		treeViewer.setCheckedElements(array);
		prjectsColumn.getColumn().pack();

		save();

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				selectedProjects.clear();
				selectedProjects.clear();

				save();
				validatePage();
			}
		});

		Composite buttonsComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);
		buttonsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		final Button includeLocal = new Button(buttonsComposite, SWT.CHECK);
		includeLocal
				.setText(UIText.GitBranchSynchronizeWizardPage_includeUncommitedChanges);
		includeLocal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shouldIncludeLocal = includeLocal.getSelection();
			}
		});
		includeLocal.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Button selectAllBtn = new Button(buttonsComposite, SWT.PUSH);
		selectAllBtn.setText(UIText.GitBranchSynchronizeWizardPage_selectAll);
		selectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				treeViewer.setCheckedElements(array);
				save();
				validatePage();
			}
		});

		Button deselectAllBtn = new Button(buttonsComposite, SWT.PUSH);
		deselectAllBtn.setText(UIText.GitBranchSynchronizeWizardPage_deselectAll);
		deselectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// uncheck everything
				treeViewer.setCheckedElements(new Object[0]);
				// clear all selection
				selectedProjects.clear();
				selectedProjects.clear();
				validatePage();
			}
		});

		Dialog.applyDialogFont(composite);
		setPageComplete(false);
		setControl(composite);
	}

	@Override
	public void dispose() {
		if (branchImage != null)
			branchImage.dispose();

		super.dispose();
	}

	private void save() {
		// record any candidate repositories that should be synchronized
		for (Object grayedElement : treeViewer.getGrayedElements())
			selectedProjects.add((IProject) grayedElement);

		for (Object checkedElement : treeViewer.getCheckedElements())
			selectedProjects.add((IProject) checkedElement);
	}

	private void validatePage() {
		boolean complete = !selectedProjects.isEmpty();
		if (complete)
			for (IProject project : selectedProjects)
				if (!selectedBranches.containsKey(projects.get(project))) {
					complete = false;
					break;
				}

		setPageComplete(complete);
	}

	Map<Repository, String> getSelectedBranches() {
		return selectedBranches;
	}

	Set<IProject> getSelectedProjects() {
		return selectedProjects;
	}

	boolean shouldIncludeLocal() {
		return shouldIncludeLocal;
	}

}
