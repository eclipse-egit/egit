/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code extracted from org.eclipse.egit.ui.internal.actions.DiscardChangesAction
 * and reworked.
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;

/**
 * The operation discards changes on a set of resources. In case of a folder
 * resource all file resources in the sub tree are processed.
 */
public class ReplaceFromHEADOperation extends DiscardChangesOperation {

	/**
	 * @param files
	 */
	public ReplaceFromHEADOperation(IResource[] files) {
		super(files);
	}

	protected void discardChange(IResource res, Repository repository)
			throws IOException {

		String resRelPath = RepositoryMapping.getMapping(res).getRepoRelativePath(res);
		File f = new File(res.getLocationURI());

		RevCommit commit = new RevWalk(repository).parseCommit(repository.getRef(Constants.HEAD).getObjectId());

		TreeWalk w = TreeWalk.forPath(repository, resRelPath, commit.getTree());
		if (w == null)
			return;		// git doesn't know such resource path

		ObjectLoader ol = repository.open(w.getObjectId(0));

		File parentDir = f.getParentFile();
		File tmpFile = File.createTempFile("._" + f.getName(), null, parentDir); //$NON-NLS-1$
		FileOutputStream channel = new FileOutputStream(tmpFile);
		try {
			ol.copyTo(channel);
		} finally {
			channel.close();
		}

		if (!tmpFile.renameTo(f)) {
			// tried to rename which failed.
			// Let' delete the target file and try again
			FileUtils.delete(f);
			if (!tmpFile.renameTo(f))
				throw new IOException("Can't write to file " + f.getPath()); //$NON-NLS-1$
		}
	}

}
