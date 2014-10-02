/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens Baumgart (SAP AG) - initial implementation
 *    Dariusz Luksza - expose public constructor
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.Utils;
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

	/**
	 *
	 * @param repository
	 *            from with blob version should be taken
	 * @param fileName
	 *            name of blob file
	 * @param blob
	 *            blob id
	 * @param commit
	 *            from with blob version should be taken
	 */
	public CommitBlobStorage(final Repository repository, final String fileName,
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

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		return prime * super.hashCode()
				+ ((commit == null) ? 0 : commit.hashCode());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommitBlobStorage other = (CommitBlobStorage) obj;
		if (commit == null) {
			if (other.commit != null)
				return false;
		} else if (!commit.equals(other.commit))
			return false;
		return true;
	}


}
