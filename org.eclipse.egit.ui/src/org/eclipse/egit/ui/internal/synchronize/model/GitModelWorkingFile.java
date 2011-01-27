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
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.egit.ui.internal.synchronize.compare.GitLocalCompareInput;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

class GitModelWorkingFile extends GitModelBlob {

	public GitModelWorkingFile(GitModelObjectContainer parent,
			RevCommit commit, ObjectId repoId, IPath location) throws IOException {
		super(parent, commit, null, repoId, repoId, null, location);
	}

	@Override
	protected GitCompareInput getCompareInput(ComparisonDataSource baseData,
			ComparisonDataSource remoteData, ComparisonDataSource ancestorData) {
		return new GitLocalCompareInput(getRepository(), ancestorData,
				baseData, remoteData, gitPath);
	}

}
