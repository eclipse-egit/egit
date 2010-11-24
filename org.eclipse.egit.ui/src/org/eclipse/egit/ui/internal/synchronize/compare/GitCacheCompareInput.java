/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.egit.ui.internal.CompareUtils.getFileCachedRevisionTypedElement;
import static org.eclipse.egit.ui.internal.CompareUtils.getFileRevisionTypedElement;

import org.eclipse.compare.ITypedElement;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Specific implementation of {@link ISynchronizationCompareInput} for showing
 * cached files
 */
public class GitCacheCompareInput extends GitCompareInput {

	/**
	 * Creates {@link GitCacheCompareInput}
	 *
	 * @param repo
	 *            repository that is connected with this object
	 * @param ancestroDataSource
	 *            data that should be use to obtain common ancestor object data
	 * @param baseDataSource
	 *            data that should be use to obtain base object data
	 * @param remoteDataSource
	 *            data that should be used to obtain remote object data
	 * @param gitPath
	 *            repository relative path of object
	 */
	public GitCacheCompareInput(Repository repo,
			ComparisonDataSource ancestroDataSource,
			ComparisonDataSource baseDataSource,
			ComparisonDataSource remoteDataSource, String gitPath) {
		super(repo, ancestroDataSource, baseDataSource, remoteDataSource,
				gitPath);
	}

	public ITypedElement getLeft() {
		return getFileCachedRevisionTypedElement(gitPath, repo);
	}

	public ITypedElement getRight() {
		return getFileRevisionTypedElement(gitPath, baseCommit, repo);
	}

}
