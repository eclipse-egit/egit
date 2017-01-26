/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2010, 2017 Wim Jongman <wim.jongman@remainsoftware.com>
 * Copyright (C) 2010, Ryan Schmitt <ryan.schmitt@boeing.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.components.FilteredCheckboxTree;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.WorkingSetGroup;

/**
 * The GitWizardProjectsImportPage is the page that allows the user to import
 * projects from a particular location. This is a modified copy of the
 * WizardProjectsImportPage class from the org.eclipse.ui.ide bundle.
 */
public class GitProjectsImportPage extends WizardPage {

	private final class ProjectLabelProvider extends GitLabelProvider implements
			IColorProvider {

		@Override
		public Color getForeground(Object element) {
			if (isProjectInWorkspace(((ProjectRecord) element).getProjectName()))
				return PlatformUI.getWorkbench().getDisplay().getSystemColor(
						SWT.COLOR_GRAY);
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}

	private final static String STORE_NESTED_PROJECTS = "GitProjectsImportPage.STORE_NESTED_PROJECTS"; //$NON-NLS-1$

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	private CachedCheckboxTreeViewer projectsList;

	private Button nestedProjectsCheckbox;

	private boolean nestedProjects = true;

	private boolean lastNestedProjects = true;

	private ProjectRecord[] selectedProjects = new ProjectRecord[0];

	private IProject[] wsProjects;

	// The last selected path to minimize searches
	private String lastPath;

	// The last time that the file or folder at the selected path was modified
	// to minimize searches
	private long lastModified;

	private Button selectAll;

	private Button deselectAll;

	private WorkingSetGroup workingSetGroup;

	/**
	 * Creates a new project creation wizard page.
	 */
	public GitProjectsImportPage() {
		super(GitProjectsImportPage.class.getName());
		setPageComplete(false);
		setTitle(UIText.WizardProjectsImportPage_ImportProjectsTitle);
		setDescription(UIText.WizardProjectsImportPage_ImportProjectsDescription);
	}

	@Override
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite workArea = new Composite(parent, SWT.NONE);
		setControl(workArea);

		workArea.setLayout(GridLayoutFactory.fillDefaults().create());
		workArea.setLayoutData(new GridData(GridData.FILL_BOTH
				| GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

		createProjectsRoot(workArea);
		createProjectsList(workArea);
		createOptionsArea(workArea);
		createWorkingSetGroup(workArea);
		restoreWidgetValues();
		Dialog.applyDialogFont(workArea);

	}

	private void createWorkingSetGroup(Composite workArea) {
		// TODO: replace hardcoded ids once bug 245106 is fixed
		String[] workingSetTypes = new String[] {
				"org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
				"org.eclipse.jdt.ui.JavaWorkingSetPage" //$NON-NLS-1$
		};
		workingSetGroup = new WorkingSetGroup(workArea, null, workingSetTypes);
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

				if (element instanceof ProjectRecord) {
					ProjectRecord p = (ProjectRecord) element;
					if (wordMatches(p.getProjectName()))
						return true;
					String projectPath = p.getProjectSystemFile().getParent();
					if (projectPath.startsWith(lastPath)) {
						String distinctPath = projectPath.substring(lastPath
								.length());
						return wordMatches(distinctPath);
					} else {
						return wordMatches(projectPath);
					}
				}

				return false;
			}
		};
		filter.setIncludeLeadingWildcard(true);

		FilteredCheckboxTree filteredTree = new FilteredCheckboxTree(
				listComposite, null, SWT.NONE, filter);

		filteredTree.setInitialText(UIText.WizardProjectsImportPage_filterText);
		projectsList = filteredTree.getCheckboxTreeViewer();
		GridData listData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		projectsList.getControl().setLayoutData(listData);
		projectsList.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				ProjectRecord element = (ProjectRecord) event.getElement();
				if (isProjectInWorkspace(element.getProjectName())) {
					projectsList.setChecked(element, false);
				}
				enableSelectAllButtons();
			}
		});

		// a bug in the CachedCheckboxTreeView requires us to not return null
		final Object[] children = new Object[0];

		projectsList.setContentProvider(new ITreeContentProvider() {

			@Override
			public Object[] getChildren(Object parentElement) {
				return children;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return selectedProjects;
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
			public void dispose() {
				// ignore
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// ignore
			}

		});

		projectsList.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});

		projectsList.setLabelProvider(new ProjectLabelProvider());

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
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllNewProjects();
				enableSelectAllButtons();
				setPageComplete(true);
			}
		});
		Dialog.applyDialogFont(selectAll);
		setButtonLayoutData(selectAll);

		deselectAll = new Button(buttonsComposite, SWT.PUSH);
		deselectAll.setText(UIText.WizardProjectsImportPage_deselectAll);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (TreeItem item : projectsList.getTree().getItems())
					projectsList.setChecked(item.getData(), false);
				projectsList.setInput(this); // filter away selected projects
				enableSelectAllButtons();
				setPageComplete(false);
			}
		});
		Dialog.applyDialogFont(deselectAll);
		setButtonLayoutData(deselectAll);
	}

	private void createOptionsArea(Composite workArea) {
		Composite optionsGroup = new Composite(workArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginBottom = 2 * layout.marginHeight;
		layout.marginHeight = 0;
		optionsGroup.setLayout(layout);
		optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		nestedProjectsCheckbox = new Button(optionsGroup, SWT.CHECK);
		nestedProjectsCheckbox
				.setText(UIText.GitProjectsImportPage_SearchForNestedProjects);
		nestedProjectsCheckbox.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
		nestedProjectsCheckbox.setSelection(nestedProjects);
		nestedProjectsCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				nestedProjects = nestedProjectsCheckbox.getSelection();
				setProjectsList(lastPath);
			}
		});
	}

	private void selectAllNewProjects() {
		for (TreeItem item : projectsList.getTree().getItems()) {
			ProjectRecord record = (ProjectRecord) item.getData();
			if (!isProjectInWorkspace(record.getProjectName()))
				projectsList.setChecked(item.getData(), true);
		}
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
		if (path.equals(lastPath) && lastModified == modified
				&& lastNestedProjects == nestedProjects) {
			// since the file/folder was not modified and the path did not
			// change, no refreshing is required
			return;
		}

		setErrorMessage(null);

		lastPath = path;
		lastModified = modified;
		lastNestedProjects = nestedProjects;

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) {

					monitor.beginTask(
							UIText.WizardProjectsImportPage_SearchingMessage,
							100);
					selectedProjects = new ProjectRecord[0];
					Collection<File> files = new ArrayList<>();
					monitor.worked(10);
					if (directory.isDirectory()) {
						boolean searchNested = nestedProjects;

						boolean found = ProjectUtil.findProjectFiles(files,
								directory, searchNested, monitor);

						if (!found)
							return;

						ArrayList<ProjectRecord> result = new ArrayList<>();
						Iterator<File> filesIterator = files.iterator();
						monitor.worked(50);
						monitor
								.subTask(UIText.WizardProjectsImportPage_ProcessingMessage);
						while (filesIterator.hasNext()) {
							File file = filesIterator.next();
							if (isSelected(file)) {
								result.add(new ProjectRecord(file));
							}
						}
						selectedProjects = result
								.toArray(new ProjectRecord[result.size()]);

						if (selectedProjects.length == 0) {
							// run in UI thread
							Display.getDefault().syncExec(() -> setErrorMessage(
									UIText.GitProjectsImportPage_NoProjectsMessage));
						}
					} else {
						monitor.worked(60);
					}
					monitor.done();
				}

				private boolean isSelected(File pFile) {
					GitCreateProjectViaWizardWizard wizard = (GitCreateProjectViaWizardWizard) getWizard();
					return wizard.getFilter().isEmpty() || wizard.getFilter().contains(pFile.getParent());
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
		selectAllNewProjects();
		enableSelectAllButtons();
		checkPageComplete();
	}

	private void enableSelectAllButtons() {
		int itemCount = getValidProjects().length;
		int selectionCount = projectsList.getCheckedLeafCount();
		selectAll.setEnabled(itemCount > selectionCount && itemCount > 0);
		deselectAll.setEnabled(selectionCount > 0);
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
		List<ProjectRecord> validProjects = new ArrayList<>();
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
	public Set<ProjectRecord> getCheckedProjects() {
		HashSet<ProjectRecord> ret = new HashSet<>();
		for (Object selected : projectsList.getCheckedElements())
			ret.add((ProjectRecord) selected);

		return ret;
	}

	/**
	 * @return the selected working sets (may be empty)
	 */
	public IWorkingSet[] getSelectedWorkingSets() {
		return workingSetGroup.getSelectedWorkingSets();
	}

	private void checkPageComplete() {
		setPageComplete(!getCheckedProjects().isEmpty());
	}

	private void restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null && settings.get(STORE_NESTED_PROJECTS) != null) {
			nestedProjects = settings.getBoolean(STORE_NESTED_PROJECTS);
			nestedProjectsCheckbox.setSelection(nestedProjects);
			lastNestedProjects = nestedProjects;
		}
	}

	void saveWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null)
			settings.put(STORE_NESTED_PROJECTS,
					nestedProjectsCheckbox.getSelection());
	}
}
