/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Utils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Blob Storage related to a RevCommit. Method <code>getFullPath</code> returns
 * a path of format <repository name>/<file path> <commit id> This results in a
 * useful tool tip on the editor title when viewing a revision and avoids the
 * issue that editors get dirty because Eclipse seems to share the document of
 * the workspace file if the remote file has the same full path.
 */
public class CommitBlobStorage extends BlobStorage {

	private final RevCommit commit;

	CommitBlobStorage(final Repository repository, final String fileName,
			final ObjectId blob, RevCommit commit) {
		super(repository, fileName, blob);
		this.commit = commit;
	}

	@Override
	public IPath getFullPath() {
		IPath repoPath = new Path(repositoryUtil.getRepositoryName(db));
		String pathString = super.getFullPath().toPortableString() + " " //$NON-NLS-1$
				+ Utils.getShortObjectId(commit.getId());
		return repoPath.append(Path.fromPortableString(pathString));
	}

}
