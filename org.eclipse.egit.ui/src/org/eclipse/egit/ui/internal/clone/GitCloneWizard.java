/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.RepositoryUtil;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends Wizard {
	private RepositorySelectionPage cloneSource;

	private SourceBranchPage validSource;

	private CloneDestinationPage cloneDestination;

	String alreadyClonedInto;

	/**
	 * The default constructor
	 */
	public GitCloneWizard() {
		setWindowTitle(UIText.GitCloneWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
		cloneSource = new RepositorySelectionPage(true, null);
		validSource = new SourceBranchPage() {

			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(cloneSource.getSelection());
				super.setVisible(visible);
			}

		};
		cloneDestination = new CloneDestinationPage() {
			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(cloneSource.getSelection(), validSource
							.getAvailableBranches(), validSource
							.getSelectedBranches(), validSource.getHEAD());
				super.setVisible(visible);
			}
		};
	}

	@Override
	public boolean performCancel() {
		if (alreadyClonedInto != null) {
			File test = new File(alreadyClonedInto);
			if (test.exists()
					&& MessageDialog.openQuestion(getShell(),
							UIText.GitCloneWizard_abortingCloneTitle,
							UIText.GitCloneWizard_abortingCloneMsg)) {
				deleteRecursively(new File(alreadyClonedInto));
			}
		}
		return true;
	}

	private void deleteRecursively(File f) {
		File[] children = f.listFiles();
		if (children != null)
			for (File i : children) {
				if (i.isDirectory()) {
					deleteRecursively(i);
				} else {
					if (!i.delete()) {
						i.deleteOnExit();
					}
				}
			}
		if (!f.delete())
			f.deleteOnExit();
	}

	@Override
	public void addPages() {
		addPage(cloneSource);
		addPage(validSource);
		addPage(cloneDestination);
	}

	@Override
	public boolean canFinish() {
		return cloneDestination.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			return performClone(false);
		} finally {
			setWindowTitle(UIText.GitCloneWizard_title);
		}
	}

	boolean performClone(boolean background) {
		final URIish uri = cloneSource.getSelection().getURI();
		setWindowTitle(NLS.bind(UIText.GitCloneWizard_jobName, uri.toString()));
		final boolean allSelected;
		final Collection<Ref> selectedBranches;
		if (validSource.isSourceRepoEmpty()) {
			// fetch all branches of empty repo
			allSelected = true;
			selectedBranches = Collections.emptyList();
		} else {
			allSelected = validSource.isAllSelected();
			selectedBranches = validSource.getSelectedBranches();
		}
		final File workdir = cloneDestination.getDestinationFile();
		final String branch = cloneDestination.getInitialBranch();
		final String remoteName = cloneDestination.getRemote();

		workdir.mkdirs();

		if (!workdir.isDirectory()) {
			final String errorMessage = NLS.bind(
					UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
			ErrorDialog.openError(getShell(), getWindowTitle(),
					UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
							Activator.getPluginId(), 0, errorMessage, null));
			// let's give user a chance to fix this minor problem
			return false;
		}

		final CloneOperation op = new CloneOperation(uri, allSelected,
				selectedBranches, workdir, branch, remoteName);

		alreadyClonedInto = workdir.getPath();

		final RepositoryUtil config = Activator.getDefault()
				.getRepositoryUtil();

		if (background) {
			final Job job = new Job(NLS.bind(UIText.GitCloneWizard_jobName, uri
					.toString())) {
				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					try {
						op.run(monitor);
						cloneSource.saveUriInPrefs();
						config.addConfiguredRepository(op.getGitDir());
						return Status.OK_STATUS;
					} catch (InterruptedException e) {
						return Status.CANCEL_STATUS;
					} catch (InvocationTargetException e) {
						Throwable thr = e.getCause();
						return new Status(IStatus.ERROR, Activator
								.getPluginId(), 0, thr.getMessage(), thr);
					}
				}

			};
			job.setUser(true);
			job.schedule();
			return true;
		} else {
			try {
				// Perform clone in ModalContext thread with progress
				// reporting on the wizard.
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						op.run(monitor);
						if (monitor.isCanceled())
							throw new InterruptedException();
					}
				});

				cloneSource.saveUriInPrefs();
				config.addConfiguredRepository(op.getGitDir());
				return true;
			} catch (InterruptedException e) {
				MessageDialog.openInformation(getShell(),
						UIText.GitCloneWizard_CloneFailedHeading,
						UIText.GitCloneWizard_CloneCanceledMessage);
				return false;
			} catch (InvocationTargetException e) {
				Activator.handleError(UIText.GitCloneWizard_CloneFailedHeading,
						e.getTargetException(), true);
				return false;
			} catch (Exception e) {
				Activator.handleError(UIText.GitCloneWizard_CloneFailedHeading,
						e, true);
				return false;
			}
		}
	}
}
