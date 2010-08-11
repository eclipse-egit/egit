/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

class GitRepositoryMapping extends GitObjectMapping {

	private final GitModelRepository gitRepo;

	protected GitRepositoryMapping(GitModelRepository gitRepo) {
		super(gitRepo);
		this.gitRepo = gitRepo;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		Repository repo = gitRepo.getRepository();
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();

		for (GitModelObject obj : gitRepo.getChildren())
			if (obj instanceof GitModelCommit) {
				RevCommit revCommit = ((GitModelCommit) obj).getRemoteCommit();
				result.add(new GitTreeTraversal(repo, revCommit));
			}

		return result.toArray(new ResourceTraversal[result.size()]);
	}

}
