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
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Displays a list of projects with their containing Git repository and check
 * box for sharing these projects with the Git Team Provider
 * <p>
 * TODO add selectAll/unselectAll/toggleSelection?
 */
public class GitShareProjectsPage extends WizardPage {

	CheckboxTableViewer tv;

	private final FilenameFilter myFilenameFilter = new FilenameFilter() {

		public boolean accept(File dir, String name) {
			return name.equals(Constants.DOT_GIT);
		}
	};

	/**
	 * Default constructor
	 */
	public GitShareProjectsPage() {
		super(GitShareProjectsPage.class.getName());
		setTitle(UIText.GitShareProjectsPage_PageTitle);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		tv = CheckboxTableViewer.newCheckList(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTable());

		TableColumn name = new TableColumn(tv.getTable(), SWT.NONE);
		name.setWidth(200);
		name.setText(UIText.GitShareProjectsPage_ProjectNameLabel);

		TableColumn repo = new TableColumn(tv.getTable(), SWT.NONE);
		repo.setWidth(400);
		repo.setText(UIText.GitShareProjectsPage_RepositoryLabel);

		tv.getTable().setHeaderVisible(true);

		tv.setContentProvider(new IStructuredContentProvider() {

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// nothing
			}

			public void dispose() {
				// nothing
			}

			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
		});

		tv.setLabelProvider(new ITableLabelProvider() {

			public void removeListener(ILabelProviderListener listener) {
				// ignore
			}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			public void dispose() {
				// nothing
			}

			public void addListener(ILabelProviderListener listener) {
				// ignore
			}

			public String getColumnText(Object element, int columnIndex) {
				switch (columnIndex) {
				case 0:
					return ((IProject) element).getName();
				case 1:
					String actRepo = getRepository((IProject) element);
					if (actRepo == null)
						return UIText.GitShareProjectsPage_NoRepoFoundMessage;
					return actRepo;
				default:
					return null;
				}
			}

			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});

		tv.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				checkPage();
			}
		});

		setControl(main);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			// when this becomes visible, we have to ask the wizard to import
			// the projects
			final ProjectCreator wiz = (ProjectCreator) getWizard();
			// TODO scheduling rule
			try {
				getContainer().run(false, true, new IRunnableWithProgress() {

					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						wiz.importProjects();

					}

				});
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getCause().getMessage(), e.getCause(),
						true);
			} catch (InterruptedException e) {
				Activator.handleError(
						UIText.GitShareProjectsPage_AbortedMessage, e, true);
			}

			setProjects(wiz.getAddedProjects());

		}
		super.setVisible(visible);
	}

	/**
	 * @param projects
	 */
	public void setProjects(IProject[] projects) {
		tv.setInput(projects);
		tv.setAllChecked(true);
		checkPage();
	}

	/**
	 * @return the selected projects
	 */
	public IProject[] getSelectedProjects() {
		List<IProject> prj = new ArrayList<IProject>();
		for (Object o : tv.getCheckedElements()) {
			prj.add((IProject) o);
		}
		return prj.toArray(new IProject[0]);

	}

	private String getRepository(IProject element) {
		File locationFile = new File(element.getLocationURI());
		return checkFileRecursive(locationFile);
	}

	private String checkFileRecursive(File locationFile) {
		if (locationFile == null)
			return null;
		if (locationFile.list(myFilenameFilter).length > 0)
			return locationFile.getPath();
		return checkFileRecursive(locationFile.getParentFile());
	}

	private void checkPage() {
		setErrorMessage(null);
		try {
			// of course we need at least one project
			IProject[] projects = (IProject[]) tv.getInput();
			if (projects == null || projects.length == 0) {
				setErrorMessage(UIText.GitShareProjectsPage_NoNewProjectMessage);
				return;
			}

			Object[] selected = tv.getCheckedElements();
			if (selected.length == 0) {
				setErrorMessage(UIText.GitShareProjectsPage_NothingSelectedMessage);
				return;
			}
			// not all selected projects may be share-able here
			for (Object obj : selected) {
				IProject prj = (IProject) obj;
				if (getRepository(prj) == null) {
					setErrorMessage(NLS
							.bind(
									UIText.GitShareProjectsPage_NoRepoForProjectMessage,
									prj.getName()));
					return;
				}
				if (RepositoryProvider.getProvider(prj) != null)
					setErrorMessage(NLS
							.bind(
									UIText.GitShareProjectsPage_ProjectAlreadySharedMessage,
									prj.getName()));
			}

		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

}
