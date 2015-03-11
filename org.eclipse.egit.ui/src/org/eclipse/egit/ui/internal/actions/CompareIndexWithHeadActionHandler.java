/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compares the index content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareIndexWithHeadActionHandler extends RepositoryActionHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final Repository repository = getRepository(true, event);
		// assert all resources map to the same repository
		if (repository == null) {
			return null;
		}
		final Object fileOrPath = getSingleSelectedObject(event);
		if (fileOrPath == null) {
			return null;
		}
		Job job = new Job(UIText.CompareUtils_jobName) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				IPath location = (IPath) (fileOrPath instanceof IPath ? fileOrPath
						: ((IResource) fileOrPath).getLocation());
				if (!isStaged(repository, location)) {
					showNoStagedFileInfo(location);
					return Status.CANCEL_STATUS;
				}
				try {
					runCompare(event, repository);
				} catch (Exception e) {
					return Activator.createErrorStatus(
							UIText.CompareWithRefAction_errorOnSynchronize, e);
				}
				return Status.OK_STATUS;
			}

		};
		job.setUser(true);
		job.schedule();

		return null;
	}

	private static void showNoStagedFileInfo(IPath location) {
		final String title = UIText.CompareIndexWithHeadActionHandler_nothingToDoTitle;
		final String message = NLS.bind(
				UIText.CompareIndexWithHeadActionHandler_fileNotStaged,
				location.toOSString());
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(null, title, message);

			}
		});
	}

	private void runCompare(ExecutionEvent event, final Repository repository)
			throws Exception {
		IWorkbenchPage workBenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		IResource[] resources = getSelectedResources();

		if (resources.length > 0) {
			CompareUtils.compare(resources, repository, GitFileRevision.INDEX,
					Constants.HEAD, false, workBenchPage);
		} else {
			IPath[] locations = getSelectedLocations(event);
			if (locations.length > 0)
				CompareUtils.compare(locations[0], repository,
						GitFileRevision.INDEX, Constants.HEAD, false,
						workBenchPage);
		}
	}

	private Object getSingleSelectedObject(ExecutionEvent event)
			throws ExecutionException {
		IResource[] resources = getSelectedResources();
		if (resources.length == 1) {
			return resources[0];
		} else {
			IPath[] locations = getSelectedLocations(event);
			if (locations.length == 1) {
				return locations[0];
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		if (selection.size() != 1) {
			return false;
		}

		Repository repository = getRepository();
		if (repository == null) {
			return false;
		}

		Object selected = selection.getFirstElement();
		IResource resource = AdapterUtils.adapt(selected, IResource.class);
		if (resource instanceof IFile) {
			return isStaged(repository, resource.getLocation());
		} else if (resource == null) {
			IPath location = AdapterUtils.adapt(selected, IPath.class);
			return isStaged(repository, location);
		}

		return false;
	}


	private boolean isStaged(Repository repository, IPath location) {
		if (location == null || location.toFile().isDirectory()) {
			return false;
		}
		String resRelPath = RepositoryMapping.getMapping(location).getRepoRelativePath(location);

		// This action at the moment only works for files anyway
		if (resRelPath == null || resRelPath.length() == 0) {
			return false;
		}

		IndexDiffData indexDiff = getIndexDiff(repository);
		return indexDiff.getAdded().contains(resRelPath)
				|| indexDiff.getChanged().contains(resRelPath)
				|| indexDiff.getRemoved().contains(resRelPath);
	}
}
