/*******************************************************************************
 * Copyright (C) 2010,2011 Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCacheCompareInput;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.jgit.lib.Repository;

/**
 * Representation of staged file in Git Change Set model
 */
public class GitModelCacheFile extends GitModelBlob {

	GitModelCacheFile(GitModelObjectContainer parent, Repository repo,
			Change change, IPath path) {
		super(parent, repo, change, path);
	}

	@Override
	protected GitCompareInput getCompareInput(ComparisonDataSource baseData,
			ComparisonDataSource remoteData, ComparisonDataSource ancestorData) {
		String gitPath = Repository.stripWorkDir(repo.getWorkTree(), path.toFile());

		return new GitCacheCompareInput(repo, (IFile) getResource(),
				ancestorData, baseData, remoteData, gitPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (obj.getClass() != getClass())
			return false;

		GitModelCacheFile objBlob = (GitModelCacheFile) obj;

		return hashCode() == objBlob.hashCode();
	}

	@Override
	public int hashCode() {
		return super.hashCode() + 31;
	}

}
