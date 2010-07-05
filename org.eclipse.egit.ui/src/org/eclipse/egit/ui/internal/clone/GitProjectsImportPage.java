package org.eclipse.egit.ui.internal.clone;

/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2010, Wim Jongman <wim.jongman@remainsoftware.com>
 * Copyright (C) 2010, Ryan Schmitt <ryan.schmitt@boeing.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The GitWizardProjectsImportPage is the page that allows the user to import
 * projects from a particular location. This is a modified copy of the
 * WizardProjectsImportPage class from the org.eclipse.ui.ide bundle.
 */
public class GitProjectsImportPage extends WizardPage {

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	private TreeViewer projectsList;

	private ProjectRecord[] selectedProjects = new ProjectRecord[0];

	private IProject[] wsProjects;

	// The last selected path to minimize searches
	private String lastPath;

	// The last time that the file or folder at the selected path was modified
	// to minimize searches
	private long lastModified;

	private Button selectAll;

	private Button deselectAll;

	/**
	 * Creates a new project creation wizard page.
	 */
	public GitProjectsImportPage() {
		super(GitProjectsImportPage.class.getName());
		setPageComplete(false);
		setTitle(UIText.WizardProjectsImportPage_ImportProjectsTitle);
		setDescription(UIText.WizardProjectsImportPage_ImportProjectsDescription);
	}

	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite workArea = new Composite(parent, SWT.NONE);
		setControl(workArea);

		workArea.setLayout(new GridLayout());
		workArea.setLayoutData(new GridData(GridData.FILL_BOTH
				| GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

		createProjectsRoot(workArea);
		createProjectsList(workArea);
		Dialog.applyDialogFont(workArea);

	}

	/**
	 * Create the checkbox list for the found projects.
	 *
	 * @param workArea
	 */
	private void createProjectsList(Composite workArea) {
		Label title = new Label(workArea, SWT.NONE);
		title.setText(UIText.WizardProjectsImportPage_ProjectsListTitle);

		Composite listComposite = new Composite(workArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = false;
		listComposite.setLayout(layout);

		listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

		PatternFilter filter = new PatternFilter() {

			@Override
			public boolean isElementVisible(Viewer viewer, Object element) {

				if (getCheckedProjects().contains(element))
					return true;

				return super.isElementVisible(viewer, element);
			}

			@Override
			public void setPattern(String patternString) {
				super.setPattern(patternString);
				// TODO: is there a better way to react on changes in the tree?
				// disable select all button when tree becomes empty due to
				// filtering
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						enableSelectAllButtons();
					}
				});

			}

		};
		// we have to use the old constructor in order to be 3.4 compatible
		// TODO once we drop 3.4 support, we should change to the new constructor
		FilteredTree filteredTree = new FilteredTree(listComposite, SWT.CHECK
				| SWT.BORDER, filter);
		filteredTree.setInitialText(UIText.WizardProjectsImportPage_filterText);
		projectsList = filteredTree.getViewer();
		GridData listData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		projectsList.getControl().setLayoutData(listData);

		projectsList.setContentProvider(new ITreeContentProvider() {

			public Object[] getChildren(Object parentElement) {
				return null;
			}

			public Object[] getElements(Object inputElement) {
				return getValidProjects();
			}

			public boolean hasChildren(Object element) {
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public void dispose() {
				// ignore
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// ignore
			}

		});

		projectsList.getTree().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});

		projectsList.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((ProjectRecord) element).getProjectLabel();
			}
		});

		projectsList.setInput(this);
		projectsList.setComparator(new ViewerComparator());
		createSelectionButtons(listComposite);
	}

	/**
	 * Create the selection buttons in the listComposite.
	 *
	 * @param listComposite
	 */
	private void createSelectionButtons(Composite listComposite) {
		Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);

		buttonsComposite.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING));

		selectAll = new Button(buttonsComposite, SWT.PUSH);
		selectAll.setText(UIText.WizardProjectsImportPage_selectAll);
		selectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// only the root has children
				for (final TreeItem item : projectsList.getTree().getItems())
					item.setChecked(true);
				setPageComplete(true);
			}
		});
		Dialog.applyDialogFont(selectAll);
		setButtonLayoutData(selectAll);

		deselectAll = new Button(buttonsComposite, SWT.PUSH);
		deselectAll.setText(UIText.WizardProjectsImportPage_deselectAll);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (final TreeItem item : projectsList.getTree().getItems())
					item.setChecked(false);
				projectsList.setInput(this); // filter away selected projects
				setPageComplete(false);
			}
		});
		Dialog.applyDialogFont(deselectAll);
		setButtonLayoutData(deselectAll);

	}

	/**
	 * Create the area where you select the root directory for the projects.
	 *
	 * @param workArea
	 *            Composite
	 */
	private void createProjectsRoot(Composite workArea) {

		// project specification group
		Composite projectGroup = new Composite(workArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
	}

	/**
	 * Update the list of projects based on path. This will not check any
	 * projects.
	 *
	 * @param path
	 */
	void setProjectsList(final String path) {
		// on an empty path empty selectedProjects
		if (path == null || path.length() == 0) {
			selectedProjects = new ProjectRecord[0];
			projectsList.refresh(true);
			checkPageComplete();
			lastPath = path;
			setErrorMessage(UIText.GitProjectsImportPage_NoProjectsMessage);
			return;
		}

		final File directory = new File(path);
		long modified = directory.lastModified();
		if (path.equals(lastPath) && lastModified == modified) {
			// since the file/folder was not modified and the path did not
			// change, no refreshing is required
			return;
		}

		setErrorMessage(null);

		lastPath = path;
		lastModified = modified;

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {

					monitor.beginTask(
							UIText.WizardProjectsImportPage_SearchingMessage,
							100);
					selectedProjects = new ProjectRecord[0];
					Collection<File> files = new ArrayList<File>();
					monitor.worked(10);
					if (directory.isDirectory()) {

						if (!collectProjectFilesFromDirectory(files, directory,
								null, monitor)) {
							return;
						}
						Iterator<File> filesIterator = files.iterator();
						selectedProjects = new ProjectRecord[files.size()];
						int index = 0;
						monitor.worked(50);
						monitor.subTask(UIText.WizardProjectsImportPage_ProcessingMessage);
						while (filesIterator.hasNext()) {
							File file = filesIterator.next();
							selectedProjects[index] = new ProjectRecord(file);
							index++;
						}

						if (files.isEmpty())
							setErrorMessage(UIText.GitProjectsImportPage_NoProjectsMessage);
					} else {
						monitor.worked(60);
					}
					monitor.done();
				}

			});
		} catch (InvocationTargetException e) {
			Activator.logError(e.getMessage(), e);
		} catch (InterruptedException e) {
			// Nothing to do if the user interrupts.
		}

		projectsList.refresh(true);
		if (getValidProjects().length < selectedProjects.length) {
			setMessage(UIText.WizardProjectsImportPage_projectsInWorkspace,
					WARNING);
		} else {
			setMessage(UIText.WizardProjectsImportPage_ImportProjectsDescription);
		}
		enableSelectAllButtons();
		checkPageComplete();
	}

	private void enableSelectAllButtons() {
		if (projectsList.getTree().getItemCount() > 0) {
			selectAll.setEnabled(true);
			deselectAll.setEnabled(true);
		} else {
			selectAll.setEnabled(false);
			deselectAll.setEnabled(false);
		}
	}

	/**
	 * Collect the list of .project files that are under directory into files.
	 *
	 * @param files
	 * @param directory
	 * @param directoriesVisited
	 *            Set of canonical paths of directories, used as recursion guard
	 * @param monitor
	 *            The monitor to report to
	 * @return boolean <code>true</code> if the operation was completed.
	 */
	private boolean collectProjectFilesFromDirectory(Collection<File> files,
			File directory, Set<String> directoriesVisited,
			IProgressMonitor monitor) {

		if (monitor.isCanceled()) {
			return false;
		}
		monitor.subTask(NLS.bind(
				UIText.WizardProjectsImportPage_CheckingMessage, directory
						.getPath()));
		File[] contents = directory.listFiles();
		if (contents == null)
			return false;

		// Initialize recursion guard for recursive symbolic links
		if (directoriesVisited == null) {
			directoriesVisited = new HashSet<String>();
			try {
				directoriesVisited.add(directory.getCanonicalPath());
			} catch (IOException exception) {
				StatusManager.getManager().handle(
						new Status(IStatus.ERROR, Activator.getPluginId(),
								exception.getLocalizedMessage(), exception));
			}
		}

		// first look for project description files
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file);
				// don't search sub-directories since we can't have nested
				// projects
				return true;
			}
		}
		// no project description found, so recurse into sub-directories
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				if (!contents[i].getName().equals(METADATA_FOLDER)) {
					try {
						String canonicalPath = contents[i].getCanonicalPath();
						if (!directoriesVisited.add(canonicalPath)) {
							// already been here --> do not recurse
							continue;
						}
					} catch (IOException exception) {
						StatusManager.getManager().handle(
								new Status(IStatus.ERROR, Activator
										.getPluginId(), exception
										.getLocalizedMessage(), exception));

					}
					collectProjectFilesFromDirectory(files, contents[i],
							directoriesVisited, monitor);
				}
			}
		}
		return true;
	}

	/**
	 * Create the selected projects
	 *
	 * @return boolean <code>true</code> if all project creations were
	 *         successful.
	 */
	boolean createProjects() {
		final Set<ProjectRecord> selected = getCheckedProjects();
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("", selected.size()); //$NON-NLS-1$
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					for (ProjectRecord projectRecord : selected) {
						createExistingProject(projectRecord,
								new SubProgressMonitor(monitor, 1));
					}
				} finally {
					monitor.done();
				}
			}
		};
		// run the new project creation operation
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			// one of the steps resulted in a core exception
			Throwable t = e.getTargetException();
			Activator.handleError(UIText.WizardProjectImportPage_errorMessage,
					t, true);
			return false;
		}
		return true;
	}

	/**
	 * Create the project described in record. If it is successful return true.
	 *
	 * @param record
	 * @param monitor
	 * @return boolean <code>true</code> if successful
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	private boolean createExistingProject(final ProjectRecord record,
			IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		String projectName = record.getProjectName();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		if (record.description == null) {
			// error case
			record.description = workspace.newProjectDescription(projectName);
			IPath locationPath = new Path(record.projectSystemFile
					.getAbsolutePath());

			// If it is under the root use the default location
			if (Platform.getLocation().isPrefixOf(locationPath)) {
				record.description.setLocation(null);
			} else {
				record.description.setLocation(locationPath);
			}
		} else {
			record.description.setName(projectName);
		}

		try {
			monitor.beginTask(
					UIText.WizardProjectsImportPage_CreateProjectsTask, 100);
			project.create(record.description, new SubProgressMonitor(monitor,
					30));
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
					monitor, 50));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}

		return true;
	}

	/**
	 * Method used for test suite.
	 *
	 * @return CheckboxTreeViewer the viewer containing all the projects found
	 */
	public TreeViewer getProjectsList() {
		return projectsList;
	}

	/**
	 * Retrieve all the projects in the current workspace.
	 *
	 * @return IProject[] array of IProject in the current workspace
	 */
	private IProject[] getProjectsInWorkspace() {
		if (wsProjects == null) {
			wsProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		}
		return wsProjects;
	}

	/**
	 * Get the array of valid project records that can be imported from the
	 * source workspace or archive, selected by the user. If a project with the
	 * same name exists in both the source workspace and the current workspace,
	 * it will not appear in the list of projects to import and thus cannot be
	 * selected for import.
	 *
	 * Method declared public for test suite.
	 *
	 * @return ProjectRecord[] array of projects that can be imported into the
	 *         workspace
	 */
	public ProjectRecord[] getValidProjects() {
		List<ProjectRecord> validProjects = new ArrayList<ProjectRecord>();
		for (int i = 0; i < selectedProjects.length; i++) {
			if (!isProjectInWorkspace(selectedProjects[i].getProjectName())) {
				validProjects.add(selectedProjects[i]);
			}
		}
		return validProjects.toArray(new ProjectRecord[validProjects.size()]);
	}

	/**
	 * Determine if the project with the given name is in the current workspace.
	 *
	 * @param projectName
	 *            String the project name to check
	 * @return boolean true if the project with the given name is in this
	 *         workspace
	 */
	private boolean isProjectInWorkspace(String projectName) {
		if (projectName == null) {
			return false;
		}
		IProject[] workspaceProjects = getProjectsInWorkspace();
		for (int i = 0; i < workspaceProjects.length; i++) {
			if (projectName.equals(workspaceProjects[i].getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return All the currently checked projects in the projectsList tree
	 */
	private Set<ProjectRecord> getCheckedProjects() {
		HashSet<ProjectRecord> ret = new HashSet<ProjectRecord>();
		for (TreeItem item : projectsList.getTree().getItems())
			if (item.getChecked())
				ret.add((ProjectRecord) item.getData());

		return ret;
	}

	private void checkPageComplete() {
		setPageComplete(!getCheckedProjects().isEmpty());
	}
}
