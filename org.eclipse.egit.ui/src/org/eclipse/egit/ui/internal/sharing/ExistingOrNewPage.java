/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Wizard page for connecting projects to Git repositories.
 */
class ExistingOrNewPage extends WizardPage {

	private final SharingWizard myWizard;
	private Button button;
	private Tree tree;
	private CheckboxTreeViewer viewer;
	private Text repositoryToCreate;
	private IPath minumumPath;
	private Text dotGitSegment;

	ExistingOrNewPage(SharingWizard w) {
		super(ExistingOrNewPage.class.getName());
		setTitle(UIText.ExistingOrNewPage_title);
		setDescription(UIText.ExistingOrNewPage_description);
		setImageDescriptor(UIIcons.WIZBAN_CONNECT_REPO);
		this.myWizard = w;
	}

	public void createControl(Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setLayout(new GridLayout(3,false));
		g.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		tree = new Tree(g, SWT.BORDER|SWT.MULTI|SWT.FULL_SELECTION|SWT.CHECK);
		viewer = new CheckboxTreeViewer(tree);
		tree.setHeaderVisible(true);
		tree.setLayout(new GridLayout());
		tree.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(3,1).create());
		viewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					ProjectAndRepo checkable = (ProjectAndRepo)event.getElement();
					for (TreeItem ti : tree.getItems()) {
						if (ti.getItemCount() > 0 ||
								((ProjectAndRepo)ti.getData()).getRepo().equals("")) //$NON-NLS-1$
							ti.setChecked(false);
						for(TreeItem subTi : ti.getItems()) {
							IProject project = ((ProjectAndRepo)subTi.getData()).getProject();
							if (checkable.getProject() != null
									&& !subTi.getData().equals(checkable)
									&& checkable.getProject().equals(project))
								subTi.setChecked(false);
						}
					}
				}
			}
		});
		TreeColumn c1 = new TreeColumn(tree,SWT.NONE);
		c1.setText(UIText.ExistingOrNewPage_HeaderProject);
		c1.setWidth(100);
		TreeColumn c2 = new TreeColumn(tree,SWT.NONE);
		c2.setText(UIText.ExistingOrNewPage_HeaderPath);
		c2.setWidth(400);
		TreeColumn c3 = new TreeColumn(tree,SWT.NONE);
		c3.setText(UIText.ExistingOrNewPage_HeaderRepository);
		c3.setWidth(200);
		for (IProject project : myWizard.projects) {
			RepositoryFinder repositoryFinder = new RepositoryFinder(project);
			try {
				Collection<RepositoryMapping> mappings;
				mappings = repositoryFinder.find(new NullProgressMonitor());
				Iterator<RepositoryMapping> mi = mappings.iterator();
				RepositoryMapping m = mi.hasNext() ? mi.next() : null;
				if (m == null) {
					// no mapping found, enable repository creation
					TreeItem treeItem = new TreeItem(tree, SWT.NONE);
					treeItem.setText(0, project.getName());
					treeItem.setText(1, project.getLocation().toOSString());
					treeItem.setText(2, ""); //$NON-NLS-1$
					treeItem.setData(
							new ProjectAndRepo(project, "")); //$NON-NLS-1$
				} else if (!mi.hasNext()){
					// exactly one mapping found
					TreeItem treeItem = new TreeItem(tree, SWT.NONE);
					treeItem.setText(0, project.getName());
					treeItem.setText(1, project.getLocation().toOSString());
					fillTreeItemWithGitDirectory(m, treeItem, false);
					treeItem.setData(new ProjectAndRepo(
							project, treeItem.getText(2)));
					treeItem.setChecked(true);
				}

				else {
					TreeItem treeItem = new TreeItem(tree, SWT.NONE);
					treeItem.setText(0, project.getName());
					treeItem.setText(1, project.getLocation().toOSString());
					treeItem.setData(new ProjectAndRepo(null, null));

					TreeItem treeItem2 = new TreeItem(treeItem, SWT.NONE);
					treeItem2.setText(0, project.getName());
					fillTreeItemWithGitDirectory(m, treeItem2, true);
					treeItem2.setData(new ProjectAndRepo(
							project, treeItem2.getText(2)));
					while (mi.hasNext()) {	// fill in additional mappings
						m = mi.next();
						treeItem2 = new TreeItem(treeItem, SWT.NONE);
						treeItem2.setText(0, project.getName());
						fillTreeItemWithGitDirectory(m, treeItem2, true);
						treeItem2.setData(new ProjectAndRepo(m.getContainer()
								.getProject(), treeItem2.getText(2)));
					}
					treeItem.setExpanded(true);
				}
			} catch (CoreException e) {
				TreeItem treeItem2 = new TreeItem(tree, SWT.BOLD|SWT.ITALIC);
				treeItem2.setText(e.getMessage());
			}
		}

		button = new Button(g, SWT.PUSH);
		button.setLayoutData(GridDataFactory.fillDefaults().create());
		button.setText(UIText.ExistingOrNewPage_CreateButton);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				File gitDir = new File(repositoryToCreate.getText(),Constants.DOT_GIT);
				try {
					Repository repository = new FileRepository(gitDir);
					repository.create();
					for (IProject project : getProjects(false).keySet()) {
						// If we don't refresh the project directories right
						// now we won't later know that a .git directory
						// exists within it and we won't mark the .git
						// directory as a team-private member. Failure
						// to do so might allow someone to delete
						// the .git directory without us stopping them.
						// (Half lie, we should optimize so we do not
						// refresh when the .git is not within the project)
						//
						if (!gitDir.toString().contains("..")) //$NON-NLS-1$
							project.refreshLocal(IResource.DEPTH_ONE,
									new NullProgressMonitor());
					}
					RepositoryUtil util = Activator.getDefault().getRepositoryUtil();
					util.addConfiguredRepository(gitDir);
				} catch (IOException e1) {
					String msg = NLS
							.bind(
									UIText.ExistingOrNewPage_ErrorFailedToCreateRepository,
									gitDir.toString());
					org.eclipse.egit.ui.Activator.handleError(msg, e1, true);
				} catch (CoreException e2) {
					String msg = NLS
							.bind(
									UIText.ExistingOrNewPage_ErrorFailedToRefreshRepository,
									gitDir);
					org.eclipse.egit.ui.Activator.handleError(msg, e2, true);
				}
				for (TreeItem ti : tree.getSelection()) {
					ti.setText(2, gitDir.toString());
					((ProjectAndRepo)ti.getData()).repo = gitDir.toString();
					ti.setChecked(true);
				}
				updateCreateOptions();
				getContainer().updateButtons();
			}
		});
		repositoryToCreate = new Text(g, SWT.SINGLE | SWT.BORDER);
		repositoryToCreate.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(1,1).create());
		repositoryToCreate.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				if (repositoryToCreate.getText().equals("")) { //$NON-NLS-1$
					button.setEnabled(false);
					return;
				}
				IPath fromOSString = Path.fromOSString(repositoryToCreate.getText());
				button.setEnabled(minumumPath
						.matchingFirstSegments(fromOSString) == fromOSString
						.segmentCount());
			}
		});
		dotGitSegment = new Text(g ,SWT.NONE);
		dotGitSegment.setEnabled(false);
		dotGitSegment.setEditable(false);
		dotGitSegment.setText(File.separatorChar + Constants.DOT_GIT);
		dotGitSegment.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).create());

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tree.select((TreeItem)e.item);
				updateCreateOptions();
			}
		});
		updateCreateOptions();
		Dialog.applyDialogFont(g);
		setControl(g);
	}

	private void fillTreeItemWithGitDirectory(RepositoryMapping m, TreeItem treeItem, boolean isAlternative) {
		if (m.getGitDir() == null)
			treeItem.setText(2, UIText.ExistingOrNewPage_SymbolicValueEmptyMapping);
		else {
			IPath container = m.getContainerPath();
			if (!container.isEmpty())
				container = Path.fromOSString("."); //$NON-NLS-1$
			IPath relativePath = container.append(m.getGitDir());
			if (isAlternative) {
				IPath withoutLastSegment = relativePath.removeLastSegments(1);
				IPath path;
				if (withoutLastSegment.isEmpty())
					path = Path.fromPortableString("."); //$NON-NLS-1$
				else
					path = withoutLastSegment;
				treeItem.setText(0, path.toString());
			}
			treeItem.setText(2, relativePath.toOSString());
			try {
				IProject project = m.getContainer().getProject();
				FileRepository repo = new FileRepository(m.getGitDirAbsolutePath().toFile());
				File workTree = repo.getWorkTree();
				IPath workTreePath = Path.fromOSString(workTree.getAbsolutePath());
				if (workTreePath.isPrefixOf(project.getLocation())) {
					IPath makeRelativeTo = project.getLocation().makeRelativeTo(workTreePath);
					String repoRelativePath = makeRelativeTo.append("/.project").toPortableString(); //$NON-NLS-1$
					ObjectId headCommitId = repo.resolve(Constants.HEAD);
					if (headCommitId != null) {
						// Not an empty repo
						RevWalk revWalk = new RevWalk(repo);
						RevCommit headCommit = revWalk.parseCommit(headCommitId);
						RevTree headTree = headCommit.getTree();
						TreeWalk projectInRepo = TreeWalk.forPath(repo, repoRelativePath, headTree);
						if (projectInRepo != null) {
							// the .project file is tracked by this repo
							treeItem.setChecked(true);
						}
						revWalk.dispose();
					}
				}
				repo.close();
			} catch (IOException e1) {
				Activator.logError("Failed to detect which repository to use", e1); //$NON-NLS-1$
			}
		}
	}

	private void updateCreateOptions() {
		minumumPath = null;
		IPath p = null;
		for (TreeItem ti : tree.getSelection()) {
			if (ti.getItemCount() > 0)
				continue;
			String path = ti.getText(2);
			if (!path.equals("")) { //$NON-NLS-1$
				p = null;
				break;
			}
			String gitDirParentCandidate = ti.getText(1);
			IPath thisPath = Path.fromOSString(gitDirParentCandidate);
			if (p == null)
				p = thisPath;
			else {
				int n = p.matchingFirstSegments(thisPath);
				p = p.removeLastSegments(p.segmentCount() - n);
			}
		}
		minumumPath = p;
		if (p != null) {
			repositoryToCreate.setText(p.toOSString());
		} else {
			repositoryToCreate.setText(""); //$NON-NLS-1$
		}
		button.setEnabled(p != null);
		repositoryToCreate.setEnabled(p != null);
		dotGitSegment.setEnabled(p != null);
		getContainer().updateButtons();
	}

	@Override
	public boolean isPageComplete() {
		if (viewer.getCheckedElements().length == 0)
			return false;
		for (Object checkedElement : viewer.getCheckedElements()) {
			String path = ((ProjectAndRepo)checkedElement).getRepo();
			if (((ProjectAndRepo)checkedElement).getRepo() != null &&
					path.equals("")) { //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	/**
	 * @param checked
	 *            pass true to get the checked elements, false to get the
	 *            selected elements
	 * @return map between project and repository root directory (converted to
	 *         an absolute path) for all projects selected by user
	 */
	public Map<IProject, File> getProjects(boolean checked) {
		final Object[] elements;
		if (checked)
			elements = viewer.getCheckedElements();
		else {
			ISelection selection = viewer.getSelection();
			if (selection instanceof IStructuredSelection)
				elements = ((IStructuredSelection) selection).toArray();
			else
				elements = new Object[0];
		}
		Map<IProject, File> ret = new HashMap<IProject, File>(elements.length);
		for (Object ti : elements) {
			final IProject project = ((ProjectAndRepo)ti).getProject();
			String path = ((ProjectAndRepo)ti).getRepo();
			final IPath selectedRepo = Path.fromOSString(path);
			IPath localPathToRepo = selectedRepo;
			if (!selectedRepo.isAbsolute()) {
				localPathToRepo = project.getLocation().append(selectedRepo);
			}
			ret.put(project, localPathToRepo.toFile());
		}
		return ret;
	}

	private static class ProjectAndRepo {
		private IProject project;
		private String repo;

		public ProjectAndRepo(IProject project, String repo) {
			this.project = project;
			this.repo = repo;
		}

		public IProject getProject() {
			return project;
		}

		public String getRepo() {
			return repo;
		}
	}
}
