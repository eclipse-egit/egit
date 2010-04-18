/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;

class GitBranchResourceVariantTree extends GitResourceVariantTree {

	/**
	 * A map of repositories to names of the branches that should be compared to
	 * by this tree.
	 */
	private Map<Repository, String> branches;

	GitBranchResourceVariantTree(Map<Repository, String> branches,
			IResource[] roots) {
		super(roots);
		this.branches = branches;
	}

	@Override
	String getRevString(IResource resource) {
		Repository repository = RepositoryMapping.getMapping(resource)
				.getRepository();
		return branches.get(repository);
	}

}
