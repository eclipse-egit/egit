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
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Blob Storage related to a file in the index. Method <code>getFullPath</code>
 * returns a path of format <repository name>/<file path> index
 *
 * @see CommitBlobStorage
 *
 */
public class IndexBlobStorage extends BlobStorage {

	IndexBlobStorage(final Repository repository, final String fileName,
			final ObjectId blob) {
		super(repository, fileName, blob);
	}

	@Override
	public IPath getFullPath() {
		IPath repoPath = new Path(Utils.getRepositoryName(db));
		String pathString = super.getFullPath().toPortableString() + " index"; //$NON-NLS-1$
		return repoPath.append(Path.fromPortableString(pathString));
	}

}
