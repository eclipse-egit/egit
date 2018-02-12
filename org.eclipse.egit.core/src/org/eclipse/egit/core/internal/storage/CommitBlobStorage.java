/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2014, Obeo
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.storage.GitBlobStorage;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
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
public class CommitBlobStorage extends GitBlobStorage {

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
	 * @param metadata
	 *            Smudge filters and EOL stream type to apply when the content
	 *            is to be gotten.
	 */
	public CommitBlobStorage(final Repository repository, final String fileName,
			final ObjectId blob, RevCommit commit, CheckoutMetadata metadata) {
		super(repository, fileName, blob, metadata);
		this.commit = commit;
	}

	@Override
	public IPath getFullPath() {
		final RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		IPath repoPath = new Path(repositoryUtil.getRepositoryName(db));
		String pathString = super.getFullPath().toPortableString() + " " //$NON-NLS-1$
				+ Utils.getShortObjectId(commit.getId());
		return repoPath.append(Path.fromPortableString(pathString));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime * super.hashCode()
				+ ((commit == null) ? 0 : commit.hashCode());
	}

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
