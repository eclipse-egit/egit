/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

class GitCommitMapping extends GitObjectMapping {

	private final GitModelCommit gitCommit;

	public GitCommitMapping(GitModelCommit gitCommit) {
		super(gitCommit);
		this.gitCommit = gitCommit;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		Repository repo = gitCommit.getRepository();
		TreeWalk tw = new TreeWalk(repo);
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		tw.reset();
		tw.setRecursive(false);
		tw.setFilter(TreeFilter.ANY_DIFF);
		try {
			RevCommit commit = gitCommit.getRemoteCommit();
			tw.addTree(commit.getParent(0).getTree());
			int nth = tw.addTree(commit.getTree());

			while (tw.next()) {
				ResourceTraversal traversal = getTraversal(tw, workspaceRoot,
						nth);

				if (traversal != null)
					result.add(traversal);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new ResourceTraversal[result.size()]);
	}

	private ResourceTraversal getTraversal(TreeWalk tw,
			IWorkspaceRoot workspaceRoot, int nth) {
		IPath path = gitCommit.getLocation().append(tw.getPathString());
		int objectType = tw.getFileMode(nth).getObjectType();

		ResourceTraversal traversal = null;
		if (objectType == Constants.OBJ_BLOB) {
			IFile file = workspaceRoot.getFileForLocation(path);
			if (file != null)
				traversal = new ResourceTraversal(new IResource[] { file },
						IResource.DEPTH_ZERO, IResource.ALLOW_MISSING_LOCAL);
		} else if (objectType == Constants.OBJ_TREE) {
			IContainer folder = workspaceRoot.getContainerForLocation(path);
			if (folder != null)
				traversal = new ResourceTraversal(new IResource[] { folder },
						IResource.DEPTH_INFINITE, IResource.ALLOW_MISSING_LOCAL);
		}

		return traversal;
	}

}
