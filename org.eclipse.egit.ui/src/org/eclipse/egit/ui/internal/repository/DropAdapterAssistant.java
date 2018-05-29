/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

/**
 * Drop Adapter Assistant for the Repositories View
 */
public class DropAdapterAssistant extends CommonDropAdapterAssistant {
	/**
	 * Default constructor
	 */
	public DropAdapterAssistant() {
		// nothing
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter,
			DropTargetEvent aDropTargetEvent, Object aTarget) {
		String[] data = (String[]) aDropTargetEvent.data;
		for (String folder : data) {
			File repoFile = new File(folder);
			if (FileKey.isGitRepository(repoFile, FS.DETECTED))
				Activator.getDefault().getRepositoryUtil()
						.addConfiguredRepository(repoFile);
			// also a direct parent of a .git dir is allowed
			else if (!repoFile.getName().equals(Constants.DOT_GIT)) {
				File dotgitfile = new File(repoFile, Constants.DOT_GIT);
				if (FileKey.isGitRepository(dotgitfile, FS.DETECTED))
					Activator.getDefault().getRepositoryUtil()
							.addConfiguredRepository(dotgitfile);
			}
		}
		// the returned Status is not consumed anyway
		return Status.OK_STATUS;
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferData) {
		// check that all paths are valid repository paths
		String[] folders = (String[]) FileTransfer.getInstance().nativeToJava(
				transferData);
		if (folders == null)
			return Status.CANCEL_STATUS;
		for (String folder : folders) {
			File repoFile = new File(folder);
			if (FileKey.isGitRepository(repoFile, FS.DETECTED)) {
				continue;
			}
			// convenience: also allow the direct parent of .git
			if (!repoFile.getName().equals(Constants.DOT_GIT)) {
				File dotgitfile = new File(repoFile, Constants.DOT_GIT);
				if (FileKey.isGitRepository(dotgitfile, FS.DETECTED))
					continue;
			}
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return FileTransfer.getInstance().isSupportedType(aTransferType);
	}
}
