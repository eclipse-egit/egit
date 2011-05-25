/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;

/**
 * A content provider for existing (non-bare) Repositories known to the
 * Repositories View
 */
public class RepoComboContentProvider implements IStructuredContentProvider {
	private final RepositoryUtil util = Activator.getDefault()
			.getRepositoryUtil();

	private final RepositoryCache cache = org.eclipse.egit.core.Activator
			.getDefault().getRepositoryCache();

	public void dispose() {
		// nothing
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	public Object[] getElements(Object inputElement) {
		List<Repository> nonBareRepos = new ArrayList<Repository>();
		for (String dir : util.getConfiguredRepositories()) {
			Repository repo;
			try {
				repo = cache.lookupRepository(new File(dir));
			} catch (IOException e1) {
				continue;
			}
			if (repo.isBare())
				continue;
			nonBareRepos.add(repo);
		}
		return nonBareRepos.toArray();
	}
}
