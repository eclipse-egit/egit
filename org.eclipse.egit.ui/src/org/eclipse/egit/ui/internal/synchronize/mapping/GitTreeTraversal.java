/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

class GitTreeTraversal extends ResourceTraversal {

	public GitTreeTraversal(GitModelTree modelTree) {
		this(modelTree.getRepository(), modelTree.getBaseCommit(), modelTree
				.getLocation());
	}

	public GitTreeTraversal(Repository repo, RevCommit commit) {
		this(repo, commit, new Path(repo.getWorkTree().toString()));
	}

	private GitTreeTraversal(Repository repo, RevCommit revCommit, IPath path) {
		super(getResourcesImpl(repo, revCommit, path),
				IResource.DEPTH_INFINITE, IResource.NONE);
	}

	private static IResource[] getResourcesImpl(Repository repo,
			RevCommit revCommit, IPath path) {
		ObjectId remoteId;
		RevCommit[] parents = revCommit.getParents();
		if (parents.length > 0)
			remoteId = revCommit.getParent(0).getTree().getId();
		else
			return new IResource[0];

		TreeWalk tw = new TreeWalk(repo);
		ObjectId baseId = revCommit.getTree().getId();
		List<IResource> result = new ArrayList<IResource>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		tw.reset();
		tw.setRecursive(false);
		tw.setFilter(TreeFilter.ANY_DIFF);
		try {
			if (!baseId.equals(zeroId()))
				tw.addTree(baseId);
			int actualNth = tw.addTree(remoteId);

			while (tw.next()) {
				int objectType = tw.getFileMode(actualNth).getObjectType();
				IPath childPath = path.append(tw.getNameString());

				IResource resource = null;
				if (objectType == Constants.OBJ_BLOB)
					resource = root.getFileForLocation(childPath);
				else if (objectType == Constants.OBJ_TREE)
					resource = root.getContainerForLocation(childPath);

				if (resource != null)
					result.add(resource);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new IResource[result.size()]);
	}
}
