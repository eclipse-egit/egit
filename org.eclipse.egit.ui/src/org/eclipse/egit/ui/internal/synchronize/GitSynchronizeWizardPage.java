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

import static org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.getAllRepoEntities;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.CheckboxLabelProvider;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.SyncRefEntity;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
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
import org.eclipse.swt.widgets.Composite;

class GitSynchronizeWizardPage extends WizardPage {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace()
			.getRoot();

	private CheckboxTreeViewer treeViewer;

	private Image branchesImage = UIIcons.BRANCHES.createImage();

	private Image repositoryImage = UIIcons.REPOSITORY.createImage();

	private final Map<Repository, SyncData> repoMapping = new HashMap<Repository, SyncData>();

	private final Map<Repository, Set<IProject>> projMapping = new HashMap<Repository, Set<IProject>>();

	private static final class SyncData {
		String srcRev;

		String dstRev;

		boolean includeLocal;
	}

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

		for (IProject project : ROOT.getProjects()) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null) {
				Repository repository = mapping.getRepository();
				if (repoMapping.get(repository) == null) {
					SyncData data = new SyncData();
					repoMapping.put(repository, data);
				}
				Set<IProject> projects = projMapping.get(repository);
				if (projects == null) {
					projects = new HashSet<IProject>();
					projMapping.put(repository, projects);
				}
				projects.add(project);
			}
		}

		treeViewer = new CheckboxTreeViewer(composite, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		TreeViewerColumn reposColumn = new TreeViewerColumn(treeViewer,
				SWT.LEAD);
		reposColumn.getColumn().setText(
				UIText.GitBranchSynchronizeWizardPage_repositories);
		reposColumn.getColumn().setImage(repositoryImage);
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		reposColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				if (element instanceof SyncRepoEntity)
					return ((Repository) element).getDirectory().getAbsolutePath();

				return null;
			}

			@Override
			public String getText(Object element) {
				return ((Repository) element).getWorkTree().getName();
			}

			@Override
			public Image getImage(Object element) {
				return repositoryImage;
			}
		});

		TreeViewerColumn srcColumn = new TreeViewerColumn(treeViewer, SWT.LEAD);
		srcColumn.getColumn().setText(UIText.GitSynchronizeWizard_SourceBranch);
		srcColumn.getColumn().setImage(branchesImage);
		srcColumn.getColumn().setWidth(180);
		final ComboBoxCellEditor srcBranchesEditor = new ComboBoxCellEditor(
				treeViewer.getTree(), new String[0]);
		srcColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				String branch = getSelectedBranchName(srcBranchesEditor, value);
				if (branch == null)
					return;

				SyncData data = repoMapping.get(element);
				data.srcRev = branch;

				repoMapping.put((Repository) element, data);
				treeViewer.refresh(element, true);

				boolean isCompleated = data.dstRev != null;
				setPageComplete(isCompleated);
				treeViewer.setChecked(element, isCompleated);
			}

			@Override
			protected Object getValue(Object element) {
				SyncData syncData = repoMapping.get(element);
				if (syncData == null)
					return Integer.valueOf(0);

				String branch = syncData.srcRev;
				CCombo combo = (CCombo) srcBranchesEditor.getControl();

				return Integer.valueOf(branch == null ? 0 : combo
						.indexOf(branch));
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return defaultCellEditor(srcBranchesEditor, element);
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		srcColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SyncData syncData = repoMapping.get(element);

				return syncData.srcRev == null ? "" : syncData.srcRev; //$NON-NLS-1$
			}
		});

		TreeViewerColumn dstColumn = new TreeViewerColumn(treeViewer, SWT.LEAD);
		dstColumn.getColumn().setText(
				UIText.GitSynchronizeWizard_DestinationBranch);
		dstColumn.getColumn().setImage(branchesImage);
		dstColumn.getColumn().setWidth(180);
		final ComboBoxCellEditor dstBranchesEditor = new ComboBoxCellEditor(
				treeViewer.getTree(), new String[0]);
		dstColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				String branch = getSelectedBranchName(dstBranchesEditor, value);
				if (branch == null)
					return;

				SyncData syncData = repoMapping.get(element);
				syncData.dstRev = branch;

				repoMapping.put((Repository) element, syncData);
				treeViewer.refresh(element, true);

				boolean isCompleated = syncData.srcRev != null;
				setPageComplete(isCompleated);
				treeViewer.setChecked(element, isCompleated);
			}

			@Override
			protected Object getValue(Object element) {
				SyncData syncData = repoMapping.get(element);
				if (syncData == null)
					return Integer.valueOf(0);

				String branch = syncData.srcRev;
				CCombo combo = (CCombo) dstBranchesEditor.getControl();
				return Integer.valueOf(branch == null ? 0 : combo
						.indexOf(branch));
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return defaultCellEditor(dstBranchesEditor, element);
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		dstColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SyncData syncData = repoMapping.get(element);

				return syncData.dstRev == null ? "" : syncData.dstRev; //$NON-NLS-1$
			}
		});

		final TreeViewerColumn includeLocalColumn = new TreeViewerColumn(
				treeViewer, SWT.CENTER);
		includeLocalColumn.getColumn().setWidth(80);
		includeLocalColumn.getColumn().setText(
				UIText.GitSynchronizeWizard_IncludeLocal);
		includeLocalColumn.getColumn().setToolTipText(
				UIText.GitSynchronizeWizard_IncludeLocalToolTip);
		final CheckboxCellEditor includeLocalEditor = new CheckboxCellEditor(
				treeViewer.getTree());
		includeLocalColumn.setLabelProvider(new CheckboxLabelProvider(
				treeViewer.getControl()) {
			@Override
			protected boolean isChecked(Object element) {
				return repoMapping.get(element).includeLocal;
			}
		});
		includeLocalColumn.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				SyncData syncData = repoMapping.get(element);
				syncData.includeLocal = ((Boolean) value).booleanValue();
			}

			@Override
			protected Object getValue(Object element) {
				return Boolean.valueOf(repoMapping.get(element).includeLocal);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return includeLocalEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Repository element = (Repository) event.getElement();
				if (event.getChecked()) {
					SyncData data = repoMapping.get(element);
					data.srcRev = ((CCombo) srcBranchesEditor.getControl())
							.getText();
					data.dstRev = ((CCombo) dstBranchesEditor.getControl())
							.getText();
					Boolean includeLocal = (Boolean) includeLocalEditor.getValue();
					data.includeLocal = includeLocal == null ? false : includeLocal.booleanValue();
					repoMapping.put(element, data);
				} else {
					SyncData data = repoMapping.get(element);
					data.srcRev = data.srcRev = null;
					repoMapping.put(element, data);
				}

				boolean isPageCompleted = true;
				for (Object checked : treeViewer.getCheckedElements()) {
					SyncData data = repoMapping.get(checked);
					if (data.srcRev == null || data.dstRev == null) {
						isPageCompleted = false;
						break;
					}
				}
				setPageComplete(isPageCompleted);
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

		treeViewer.setInput(repoMapping.keySet().toArray(
				new Object[repoMapping.size()]));
		reposColumn.getColumn().pack();

		Dialog.applyDialogFont(composite);
		setPageComplete(false);
		setControl(composite);
	}

	@Override
	public void dispose() {
		if (branchesImage != null)
			branchesImage.dispose();

		if (repositoryImage != null)
			repositoryImage.dispose();

		super.dispose();
	}

	GitSynchronizeDataSet getSyncData() throws IOException {
		GitSynchronizeDataSet result = new GitSynchronizeDataSet();
		for (Object checked : treeViewer.getCheckedElements()) {
			SyncData data = repoMapping.get(checked);
			if (data.srcRev != null && data.dstRev != null)
				result.add(new GitSynchronizeData((Repository) checked,
						data.srcRev, data.dstRev, data.includeLocal));
		}

		return result;
	}

	IProject[] getProjects() {
		Set<IProject> projects = new HashSet<IProject>();
		for (Object checked : treeViewer.getCheckedElements())
			projects.addAll(projMapping.get(checked));

		return projects.toArray(new IProject[projects.size()]);
	}

	private CellEditor defaultCellEditor(ComboBoxCellEditor branchesEditor,
			Object element) {
		List<SyncRefEntity> refs = getAllRepoEntities((Repository) element).getRefList();
		String[] items = new String[refs.size()];
		for (int i = 0; i < refs.size(); i++)
			items[i] = refs.get(i).getDescription();

		branchesEditor.setItems(items);

		return branchesEditor;
	}

	private String getSelectedBranchName(ComboBoxCellEditor cellEditor,
			Object value) {
		int intValue = ((Integer) value).intValue();
		if (intValue == -1)
			return null;

		CCombo combo = (CCombo) cellEditor.getControl();
		String branch = combo.getItem(intValue);

		return branch;
	}

}
