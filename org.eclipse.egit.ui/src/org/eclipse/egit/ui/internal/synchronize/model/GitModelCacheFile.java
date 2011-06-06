/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCacheCompareInput;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Representation of staged file in Git Change Set model
 */
public class GitModelCacheFile extends GitModelBlob {

	GitModelCacheFile(GitModelObjectContainer parent, RevCommit commit,
			ObjectId repoId, ObjectId cacheId, IPath location) throws IOException {
		super(parent, commit, null, repoId, cacheId, repoId, location);
	}

	@Override
	protected GitCompareInput getCompareInput(ComparisonDataSource baseData,
			ComparisonDataSource remoteData, ComparisonDataSource ancestorData) {
		return new GitCacheCompareInput(getRepository(), ancestorData,
				baseData, remoteData, gitPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelCacheFile) {
			GitModelCacheFile objBlob = (GitModelCacheFile) obj;

			return objBlob.baseId.equals(baseId)
					&& objBlob.remoteId.equals(remoteId)
					&& objBlob.getLocation().equals(getLocation());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return baseId.hashCode() ^ remoteId.hashCode()
				^ getLocation().hashCode();
	}

	@Override
	public String toString() {
		return "ModelCacheFile[repoId=" + baseId + ". cacheId=" + remoteId + ", location=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ getLocation() + "]"; //$NON-NLS-1$
	}

}
