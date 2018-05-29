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
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.egit.ui.internal.CompareUtils.getIndexTypedElement;
import static org.eclipse.egit.ui.internal.CompareUtils.getFileRevisionTypedElement;

import java.io.IOException;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Specific implementation of {@link ISynchronizationCompareInput} for showing
 * cached files
 */
public class GitCacheCompareInput extends GitCompareInput {

	private final IFile baseFile;

	/**
	 * Creates {@link GitCacheCompareInput}
	 *
	 * @param repo
	 *            repository that is connected with this object
	 * @param baseFile
	 *            base file
	 * @param ancestorDataSource
	 *            data that should be use to obtain common ancestor object data
	 * @param baseDataSource
	 *            data that should be use to obtain base object data
	 * @param remoteDataSource
	 *            data that should be used to obtain remote object data
	 * @param gitPath
	 *            repository relative path of object
	 */
	public GitCacheCompareInput(Repository repo,
			IFile baseFile, ComparisonDataSource ancestorDataSource,
			ComparisonDataSource baseDataSource,
			ComparisonDataSource remoteDataSource, String gitPath) {
		super(repo, ancestorDataSource, baseDataSource, remoteDataSource,
				gitPath);
		this.baseFile = baseFile;
	}

	@Override
	public ITypedElement getLeft() {
		try {
			return getIndexTypedElement(baseFile);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public ITypedElement getRight() {
		return getFileRevisionTypedElement(gitPath, baseCommit, repo);
	}

	@Override
	public ITypedElement getAncestor() {
		return getRight();
	}

}
