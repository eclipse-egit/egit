/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ConfigurationChecker;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * The dialog used for activating Team>Share, i.e. to create a new
 * Git repository or associate projects with one.
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
		ConfigurationChecker.checkConfiguration();
	}

	public void init(IWorkbench workbench, IProject[] p) {
		this.projects = new IProject[p.length];
		System.arraycopy(p, 0, this.projects, 0, p.length);
	}

	public void init(final IWorkbench workbench, final IProject p) {
		projects = new IProject[] { p };
	}

	public void addPages() {
		existingPage = new ExistingOrNewPage(this);
		addPage(existingPage);
	}

	public boolean performFinish() {
		final ConnectProviderOperation op = new ConnectProviderOperation(
				existingPage.getProjects(true));
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException {
					try {
						op.execute(monitor);
						PlatformUI.getWorkbench().getDisplay()
								.syncExec(new Runnable() {
									public void run() {
										Set<File> filesToAdd = new HashSet<File>();
										// collect all files first
										for (Entry<IProject, File> entry : existingPage
												.getProjects(true).entrySet())
											filesToAdd.add(entry.getValue());
										// add the files to the repository view
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

	@Override
	public boolean canFinish() {
		return existingPage.isPageComplete();
	}
}
