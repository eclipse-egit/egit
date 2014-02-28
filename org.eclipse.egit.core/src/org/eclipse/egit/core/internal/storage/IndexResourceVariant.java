/*******************************************************************************
 * Copyright (C) 2014, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;

/**
 * Implementation of a resource variant populated through a Repository's
 * DirCache information.
 */
public class IndexResourceVariant extends AbstractGitResourceVariant {
	private IndexResourceVariant(Repository repository, String path, String fileName, boolean isContainer, ObjectId objectId, int rawMode) {
		super(repository, path, fileName, isContainer, objectId, rawMode);
	}

	/**
	 * Constructs a resource variant corresponding to the given DirCache entry.
	 *
	 * @param repository
	 *            Repository from which this DirCacheEntry was extracted.
	 * @param entry
	 *            The DirCacheEntry for which content we need an
	 *            IResourceVariant.
	 * @return The created variant.
	 */
	public static IndexResourceVariant create(Repository repository,
			DirCacheEntry entry) {
		final String path = entry.getPathString();
		final String fileName;
		int lastSeparator = path.lastIndexOf('/');
		if (lastSeparator > 0)
			fileName = path.substring(lastSeparator + 1);
		else
			fileName = path;

		final boolean isContainer = FileMode.TREE.equals(entry.getFileMode());
		final ObjectId objectId = entry.getObjectId();
		final int rawMode = entry.getRawMode();

		return new IndexResourceVariant(repository, path, fileName,
				isContainer, objectId, rawMode);
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new IndexBlobStorage(repository, path, objectId);
	}
}
