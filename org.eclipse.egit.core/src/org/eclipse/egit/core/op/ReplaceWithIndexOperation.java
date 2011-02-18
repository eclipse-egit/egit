/*******************************************************************************
 * Copyright (C) 2011, Ilya Ivanov <ilya.ivanov@intland.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Repository;

/**
 * The operation replaces a set of resources with staged versions.
 * In case of a folder resource children are processed recursively.
 */
public class ReplaceWithIndexOperation extends DiscardChangesOperation {

	/**
	 * @param files
	 */
	public ReplaceWithIndexOperation(IResource[] files) {
		super(files);
	}

	protected void discardChange(IResource res, Repository repository)
			throws IOException {
		String resRelPath = RepositoryMapping.getMapping(res)
				.getRepoRelativePath(res);
		DirCache dc = repository.lockDirCache();
		try {
			DirCacheEntry entry = dc.getEntry(resRelPath);
			if (entry != null) {
				File file = new File(res.getLocationURI());
				DirCacheCheckout.checkoutEntry(repository, file, entry);
			}
		} finally {
			dc.unlock();
		}
	}

}
