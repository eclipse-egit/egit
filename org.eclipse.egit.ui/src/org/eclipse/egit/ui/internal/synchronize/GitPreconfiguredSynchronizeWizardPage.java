/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Based on org.eclipse.egit.ui.internal.synchronize.GitSynchronizeWizardPage
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.egit.ui.internal.components.ButtonLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

class GitPreconfiguredSynchronizeWizardPage extends WizardPage {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace()
			.getRoot();

	private CheckboxTreeViewer treeViewer;

	private Image repositoryImage = UIIcons.REPOSITORY.createImage();

	private final Map<Repository, Set<IProject>> projMapping = new HashMap<Repository, Set<IProject>>();

	private final Map<Repository, PredefindedConfiguration> repoMapping = new HashMap<Repository, PredefindedConfiguration>();

	private enum PredefindedConfiguration {
		WORKING_TREE, REMOTE_TRACKING, CUSTOM;
	}

	private interface PredefindedColumnConfiguration {
		String getText();

		String getToolTip();

		boolean isEnabled(Repository repo);

		PredefindedConfiguration getType();
	}

	GitPreconfiguredSynchronizeWizardPage() {
		super(GitPreconfiguredSynchronizeWizardPage.class.getName());
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
				if (repoMapping.get(repository) == null)
					repoMapping.put(repository, null);

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
					return ((Repository) element).getDirectory()
							.getAbsolutePath();

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

		createPredefindedConfigurationColumn(new PredefindedColumnConfiguration() {
			public boolean isEnabled(Repository repo) {
				return true;
			}

			public PredefindedConfiguration getType() {
				return PredefindedConfiguration.WORKING_TREE;
			}

			public String getToolTip() {
				return UIText.GitSynchronizeWizard_WorkingTreeToolTip;
			}

			public String getText() {
				return UIText.GitSynchronizeWizard_WorkingTree;
			}
		});
		createPredefindedConfigurationColumn(new PredefindedColumnConfiguration() {
			public boolean isEnabled(Repository repo) {
				String branchName;
				try {
					branchName = repo.getBranch();
				} catch (IOException e) {
					// disable 'remote tracking option' when current branch
					// cannot be determinate
					return false;
				}
				StoredConfig config = repo.getConfig();
				String remote = config
						.getString("branch", branchName, "remote"); //$NON-NLS-1$ //$NON-NLS-2$
				String merge = config.getString("branch", branchName, "merge"); //$NON-NLS-1$ //$NON-NLS-2$

				return remote == null || merge == null;
			}

			public PredefindedConfiguration getType() {
				return PredefindedConfiguration.REMOTE_TRACKING;
			}

			public String getToolTip() {
				return UIText.GitSynchronizeWizard_RemoteTrackingToolTip;
			}

			public String getText() {
				return UIText.GitSynchronizeWizard_RemoteTracking;
			}
		});
		createPredefindedConfigurationColumn(new PredefindedColumnConfiguration() {
			public boolean isEnabled(Repository repo) {
				return true;
			}

			public PredefindedConfiguration getType() {
				return PredefindedConfiguration.CUSTOM;
			}

			public String getToolTip() {
				return UIText.GitSynchronizeWizard_CustomeToolTip;
			}

			public String getText() {
				return UIText.GitSynchronizeWizard_Custom;
			}
		});

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				PredefindedConfiguration conf = repoMapping.get(event.getElement());
				setPageComplete(conf != null && conf != PredefindedConfiguration.CUSTOM);
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
	public boolean canFlipToNextPage() {
		Object[] checkedElements = treeViewer.getCheckedElements();
		if (checkedElements.length == 0)
			return false;

		for (Object element : checkedElements) {
			PredefindedConfiguration conf = repoMapping.get(element);
			if (conf == PredefindedConfiguration.CUSTOM)
				return true;
		}

		return false;
	}

	@Override
	public void dispose() {
		if (repositoryImage != null)
			repositoryImage.dispose();

		super.dispose();
	}

	GitSynchronizeDataSet getSyncData() throws IOException {
		GitSynchronizeDataSet result = new GitSynchronizeDataSet();
		for (Object checked : treeViewer.getCheckedElements()) {
			GitSynchronizeData gsd = null;
			Repository repo = (Repository) checked;
			switch (repoMapping.get(checked)) {
			case CUSTOM:
				// gsd = null;
				break;
			case REMOTE_TRACKING:
				String branchName = repo.getBranch();
				StoredConfig config = repo.getConfig();

				String remote = config
						.getString("branch", branchName, "remote"); //$NON-NLS-1$ //$NON-NLS-2$
				String merge = config.getString("branch", branchName, "merge"); //$NON-NLS-1$ //$NON-NLS-2$
				merge = SyncRepoEntity.PATTERN.matcher(merge).replaceAll(""); //$NON-NLS-1$

				gsd = new GitSynchronizeData(repo, HEAD,
						remote + "/" + merge, true); //$NON-NLS-1$
				break;
			case WORKING_TREE:
				gsd = new GitSynchronizeData(repo, HEAD, HEAD, true);
				break;
			}
			if (gsd != null)
				result.add(gsd);
		}

		return result;
	}

	List<IProject> getProjects() {
		List<IProject> projects = new ArrayList<IProject>();
		for (Object checked : treeViewer.getCheckedElements())
			projects.addAll(projMapping.get(checked));

		return projects;
	}


	boolean requiresCustomeConfiguration() {
		if (repoMapping.isEmpty())
			return false;

		for (PredefindedConfiguration conf : repoMapping.values())
			if (conf == PredefindedConfiguration.CUSTOM)
				return true;

		return false;
	}

	Repository[] getRepositoriesForCustomeConfiguration() {
		HashSet<Repository> result = new HashSet<Repository>();
		for (Repository repo : repoMapping.keySet())
			if (repoMapping.get(repo) == PredefindedConfiguration.CUSTOM)
				result.add(repo);

		return result.toArray(new Repository[result.size()]);
	}

	private void createPredefindedConfigurationColumn(
			final PredefindedColumnConfiguration columnConf) {
		final TreeViewerColumn column = new TreeViewerColumn(treeViewer,
				SWT.CENTER);

		column.getColumn().setWidth(100);
		column.getColumn().setText(columnConf.getText());
		column.getColumn().setToolTipText(columnConf.getToolTip());

		final CheckboxCellEditor editor = new CheckboxCellEditor(
				treeViewer.getTree());
		column.setLabelProvider(new ButtonLabelProvider(treeViewer
				.getControl(), SWT.RADIO) {
			@Override
			protected boolean isChecked(Object element) {
				return repoMapping.get(element) == columnConf.getType();
			}

			@Override
			protected boolean isEnabled(Object element) {
				return columnConf.isEnabled((Repository) element);
			}
		});
		column.setEditingSupport(new EditingSupport(treeViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				repoMapping.put((Repository) element, columnConf.getType());
				column.getViewer().refresh();
				getWizard().getContainer().updateButtons();
				if (columnConf.getType() != PredefindedConfiguration.CUSTOM && (Boolean) value)
					setPageComplete(true);
			}

			@Override
			protected Object getValue(Object element) {
				return Boolean.valueOf(repoMapping.get(element) == columnConf
						.getType());
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return columnConf.isEnabled((Repository) element);
			}
		});
	}

}
