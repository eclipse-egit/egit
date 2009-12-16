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
import java.net.URI;
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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard page for connecting projects to Git repositories.
 */
class ExistingOrNewPage extends WizardPage {

	private final SharingWizard myWizard;
	private Button button;
	private Tree tree;
	private Text repositoryToCreate;
	private IPath minumumPath;

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
		tree = new Tree(g, SWT.BORDER|SWT.MULTI);
		tree.setHeaderVisible(true);
		tree.setLayout(new GridLayout());
		tree.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(3,1).create());
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
			TreeItem treeItem = new TreeItem(tree, SWT.NONE);
			treeItem.setData(project);
			treeItem.setText(0, project.getName());
			treeItem.setText(1, project.getLocation().toOSString());
			RepositoryFinder repositoryFinder = new RepositoryFinder(project);
			Collection<RepositoryMapping> find;
			try {
				find = repositoryFinder.find(new NullProgressMonitor());
				Iterator<RepositoryMapping> mi = find.iterator();

				// special case for a git repository in the project's root
				final File gitDirInProjectRoot = project.getLocation().append(
						".git").toFile(); //$NON-NLS-1$
				RepositoryMapping m = mi.hasNext() ? mi.next() : null;
				if (!gitDirInProjectRoot.isDirectory() && m == null) {
					// '.git/' isn't there and no mapping found,
					// enable repository creation
						treeItem.setText(2, ""); //$NON-NLS-1$
				} else {
					// at least one git dir found
					fillTreeItemWithGitDirectory(m, treeItem, false);
				}

				while (mi.hasNext()) {	// fill in additional mappings
					m = mi.next();
					TreeItem treeItem2 = new TreeItem(treeItem, SWT.NONE);
					treeItem2.setData(m.getContainer().getProject());
					fillTreeItemWithGitDirectory(m, treeItem2, true);
				}
			} catch (CoreException e) {
				TreeItem treeItem2 = new TreeItem(treeItem, SWT.BOLD|SWT.ITALIC);
				treeItem2.setText(e.getMessage());
			}
		}

		button = new Button(g, SWT.PUSH);
		button.setLayoutData(GridDataFactory.fillDefaults().create());
		button.setText(UIText.ExistingOrNewPage_CreateButton);
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				File gitDir = new File(repositoryToCreate.getText(),".git"); //$NON-NLS-1$
				try {
					Repository repository = new Repository(gitDir);
					repository.create();
					for (IProject project : getProjects().keySet()) {
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
				} catch (IOException e1) {
					MessageDialog.openError(getShell(), UIText.ExistingOrNewPage_ErrorFailedToCreateRepository, gitDir.toString() + ":\n" + e1.getMessage()); //$NON-NLS-1$
					Activator.logError("Failed to create repository at " + gitDir, e1); //$NON-NLS-1$
				} catch (CoreException e2) {
					Activator.logError(UIText.ExistingOrNewPage_ErrorFailedToRefreshRepository + gitDir, e2);
				}
				for (TreeItem ti : tree.getSelection()) {
					ti.setText(2, gitDir.toString());
				}
				updateCreateOptions();
				getContainer().updateButtons();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		repositoryToCreate = new Text(g, SWT.SINGLE | SWT.BORDER);
		repositoryToCreate.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(1,1).create());
		repositoryToCreate.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				if (e.text == null)
					return;
				IPath fromOSString = Path.fromOSString(e.text);
				button.setEnabled(minumumPath
						.matchingFirstSegments(fromOSString) == fromOSString
						.segmentCount());
			}
		});
		Text l = new Text(g,SWT.NONE);
		l.setEnabled(false);
		l.setEditable(false);
		l.setText(File.separatorChar + ".git"); //$NON-NLS-1$
		l.setLayoutData(GridDataFactory.fillDefaults().create());
		tree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateCreateOptions();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				// Empty
			}
		});
		updateCreateOptions();
		setControl(g);
	}

	private void fillTreeItemWithGitDirectory(RepositoryMapping m, TreeItem treeItem2, boolean isAlternative) {
		if (m.getGitDir() == null)
			treeItem2.setText(2, UIText.ExistingOrNewPage_SymbolicValueEmptyMapping);
		else {
			IPath container = m.getContainerPath();
			if (!container.isEmpty())
				container = container.addTrailingSeparator();
			IPath relativePath = container.append(m.getGitDir());
			if (isAlternative)
				treeItem2.setText(0, relativePath.removeLastSegments(1).addTrailingSeparator().toString());
			treeItem2.setText(2, relativePath.toString());
		}
	}

	private void updateCreateOptions() {
		minumumPath = null;
		IPath p = null;
		for (TreeItem ti : tree.getSelection()) {
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
		getContainer().updateButtons();
	}

	@Override
	public boolean isPageComplete() {
		if (tree.getSelectionCount() == 0)
			return false;
		for (TreeItem ti : tree.getSelection()) {
			String path = ti.getText(2);
			if (path.equals("")) { //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	/**
	 * @return map between project and repository root directory (converted to a
	 *         path relative to project's root) for all projects selected by
	 *         user
	 */
	public Map<IProject, File> getProjects() {
		final TreeItem[] selection = tree.getSelection();
		Map<IProject, File> ret = new HashMap<IProject, File>(selection.length);
		for (int i = 0; i < selection.length; ++i) {
			TreeItem treeItem = selection[i];
			while (treeItem.getData() ==  null && treeItem.getParentItem() != null) {
				treeItem = treeItem.getParentItem();
			}

			final IProject project = (IProject) treeItem.getData();
			final File selectedRepo = new File(treeItem.getText(2));
			File localPathToRepo = selectedRepo;
			if (selectedRepo.isAbsolute()) {
				final URI projectLocation = project.getLocationURI();
				localPathToRepo = new File(projectLocation.relativize(selectedRepo.toURI()).getPath());
			}
			ret.put(project, localPathToRepo);
		}
		return ret;
	}
}
