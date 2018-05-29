/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2016, Lars Vogel <Lars.Vogel@vogella.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.MoveProjectOperation;

/**
 * The dialog used for activating Team>Share, i.e. to create a new Git
 * repository or associate projects with one.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard,
		IConfigurationWizardExtension {
	IProject[] projects;

	private ExistingOrNewPage existingPage;

	/**
	 * Construct the Git Sharing Wizard for connecting Git project to Eclipse
	 */
	public SharingWizard() {
		setWindowTitle(UIText.SharingWizard_windowTitle);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IProject[] p) {
		this.projects = new IProject[p.length];
		System.arraycopy(p, 0, this.projects, 0, p.length);
	}

	@Override
	public void init(final IWorkbench workbench, final IProject p) {
		projects = new IProject[] { p };
	}

	@Override
	public void addPages() {
		existingPage = new ExistingOrNewPage(this);
		addPage(existingPage);
	}

	@Override
	public boolean performFinish() {
		final IWorkbenchPage activePage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		if (!existingPage.getInternalMode()) {
			try {
				final Map<IProject, File> projectsToMove = existingPage
						.getProjects(true);
				final Repository selectedRepository = existingPage
						.getSelectedRepository();
				getContainer().run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						SubMonitor progress = SubMonitor.convert(monitor,
								projectsToMove.size() * 2);
						for (Map.Entry<IProject, File> entry : projectsToMove
								.entrySet()) {
							closeOpenEditorsForProject(activePage,
									entry.getKey());
							IPath targetLocation = new Path(entry.getValue()
									.getPath());
							IPath currentLocation = entry.getKey()
									.getLocation();
							if (!targetLocation.equals(currentLocation)) {
								MoveProjectOperation op = new MoveProjectOperation(
										entry.getKey(),
										entry.getValue().toURI(),
										UIText.SharingWizard_MoveProjectActionLabel);
								try {
									IStatus result = op.execute(
											progress.newChild(1), null);
									if (!result.isOK())
										throw new RuntimeException();
								} catch (ExecutionException e) {
									if (e.getCause() != null)
										throw new InvocationTargetException(e
												.getCause());
									throw new InvocationTargetException(e);
								}
							} else {
								progress.worked(1);
							}
							try {
								new ConnectProviderOperation(entry.getKey(),
										selectedRepository.getDirectory())
												.execute(progress.newChild(1));
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}
					}
				});
			} catch (InvocationTargetException e) {
				Activator.handleError(UIText.SharingWizard_failed,
						e.getCause(), true);
				return false;
			} catch (InterruptedException e) {
				// ignore for the moment
			}
			return true;
		} else {
			final ConnectProviderOperation op = new ConnectProviderOperation(
					existingPage.getProjects(true));
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor)
							throws InvocationTargetException {
						try {
							op.execute(monitor);
							PlatformUI.getWorkbench().getDisplay()
									.syncExec(new Runnable() {
										@Override
										public void run() {
											Set<File> filesToAdd = new HashSet<>();
											// collect all files first
											for (Entry<IProject, File> entry : existingPage
													.getProjects(true)
													.entrySet())
												filesToAdd.add(entry.getValue());
											// add the files to the repository
											// view
											for (File file : filesToAdd)
												Activator
														.getDefault()
														.getRepositoryUtil()
														.addConfiguredRepository(
																file);
										}
									});
						} catch (CoreException ce) {
							throw new InvocationTargetException(ce);
						}
					}
				});
				return true;
			} catch (Throwable e) {
				if (e instanceof InvocationTargetException) {
					e = e.getCause();
				}
				if (e instanceof CoreException) {
					IStatus status = ((CoreException) e).getStatus();
					e = status.getException();
				}
				Activator.handleError(UIText.SharingWizard_failed, e, true);
				return false;
			}
		}
	}

	private void closeOpenEditorsForProject(final IWorkbenchPage activePage,
			IProject project) {
		final List<IEditorReference> editorRefsToClose = findEditorReferencesForProject(
				activePage, project);
		if (editorRefsToClose.isEmpty()) {
			return;
		}
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				activePage.closeEditors(
						editorRefsToClose.toArray(
								new IEditorReference[editorRefsToClose.size()]),
						true);
			}
		});
	}

	private List<IEditorReference> findEditorReferencesForProject(
			IWorkbenchPage activePage, IProject project) {
		List<IEditorReference> fileEditors = new ArrayList<>();
		for (IEditorReference editorReference : activePage
				.getEditorReferences()) {
			try {
				IEditorInput editorInput = editorReference.getEditorInput();
				if (editorInput instanceof IFileEditorInput) {
					IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
					IFile file = fileEditorInput.getFile();
					if (file.getProject().equals(project)) {
						fileEditors.add(editorReference);
					}
				}
			} catch (PartInitException e) {
				Activator.logError("PartInitException - should not happen", e); //$NON-NLS-1$
			}
		}
		return fileEditors;
	}

	@Override
	public boolean canFinish() {
		return existingPage.isPageComplete();
	}
}
