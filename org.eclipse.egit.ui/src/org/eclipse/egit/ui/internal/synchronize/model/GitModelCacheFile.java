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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

class GitModelCacheFile extends GitModelBlob {

	public GitModelCacheFile(GitModelObjectContainer parent, RevCommit commit,
			ObjectId repoId, ObjectId cacheId, String name) throws IOException {
		super(parent, commit, repoId, repoId, cacheId, name);
	}

	@Override
	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		super.prepareInput(configuration, monitor);
		configuration.setLeftLabel(NLS.bind(
				UIText.GitModelWorkingFile_cachedVersion, getLeft().getName()));
	}

}
