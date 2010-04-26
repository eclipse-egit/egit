/*******************************************************************************
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.GitIndex.Entry;

/**
 * Checkout all selected dirty files.
 */
public class DiscardChangesAction extends RepositoryAction{

	@Override
	public void execute(IAction action) {

		boolean performAction = MessageDialog.openConfirm(getShell(),
				UIText.DiscardChangesAction_confirmActionTitle,
				UIText.DiscardChangesAction_confirmActionMessage);
		if (performAction) {
			performDiscardChanges();
		}
	}

	private void performDiscardChanges() {
		ArrayList<IResource> allFiles = new ArrayList<IResource>();

		// find all files
		for (IResource res : getSelectedResources()) {
			allFiles.addAll(getAllMembers(res));
		}

		for (IResource res : allFiles) {
			try {
				discardChange(res);

			} catch (IOException e1) {
				Activator.logError(UIText.DiscardChangesAction_unexpectedErrorMessage, e1);
				MessageDialog.openError(getShell(),
						UIText.DiscardChangesAction_unexpectedErrorTitle,
						UIText.DiscardChangesAction_unexpectedErrorMessage);
			}catch (RuntimeException e2) {
				Activator.logError(UIText.DiscardChangesAction_unexpectedIndexErrorMessage, e2);
				MessageDialog.openError(getShell(),
						UIText.DiscardChangesAction_unexpectedErrorTitle,
						UIText.DiscardChangesAction_unexpectedIndexErrorMessage);
			}
		}

	}

	private void discardChange(IResource res) throws IOException {
		IProject[] proj = new IProject[] { res.getProject() };
		Repository repository = getRepositoriesFor(proj)[0];

		String resRelPath = RepositoryMapping.getMapping(res).getRepoRelativePath(res);
		Entry e = repository.getIndex().getEntry(resRelPath);

		// resource must exist in the index and be dirty
		if (e != null && e.getStage() == 0 && e.isModified(repository.getWorkDir())) {
			repository.getIndex().checkoutEntry(repository.getWorkDir(), e);

			try {
				res.refreshLocal(0, new NullProgressMonitor());
			} catch (CoreException e1) {
				Activator.logError(UIText.DiscardChangesAction_refreshErrorMessage, e1);
				MessageDialog.openError(getShell(),
						UIText.DiscardChangesAction_refreshErrorTitle,
						UIText.DiscardChangesAction_refreshErrorMessage);
			}

			repository.getIndex().write();
		}
	}

	@Override
	public boolean isEnabled() {
		for (IResource res : getSelectedResources()) {
			IProject[] proj = new IProject[] { res.getProject() };
			Repository repository = getRepositoriesFor(proj)[0];
			if (! repository.getRepositoryState().equals(RepositoryState.SAFE)){
				return false;
			}
		}
		return true;
	}

	/**
	 * @param res an IResource
	 * @return An ArrayList with all members of this IResource
	 * of arbitrary depth. This will return just the argument
	 * res if it is a file.
	 */
	private ArrayList<IResource> getAllMembers(IResource res) {
		ArrayList<IResource> ret = new ArrayList<IResource>();
		if (res.getLocation().toFile().isFile()) {
			ret.add(res);
		} else {
			getAllMembersHelper(res, ret);
		}
		return ret;
	}


	private void getAllMembersHelper(IResource res, ArrayList<IResource> ret) {
		ArrayList<IResource> tmp = new ArrayList<IResource> ();
		if (res instanceof IContainer) {
			IContainer cont = (IContainer) res;
			try {
				for (IResource r : cont.members()) {
					if (r.getLocation().toFile().isFile()) {
						tmp.add(r);
					} else {
						getAllMembersHelper(r, tmp);
					}
				}
			} catch (CoreException e) {
				// thrown by members()
				// ignore children in case parent resource no longer accessible
				return;
			}

			ret.addAll(tmp);
		}
	}

}
