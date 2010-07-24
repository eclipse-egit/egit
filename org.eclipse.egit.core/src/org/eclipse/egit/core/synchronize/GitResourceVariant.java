/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.variants.IResourceVariant;

abstract class GitResourceVariant implements IResourceVariant {

	private final String path;

	private final Repository repo;

	private final ObjectId objectId;

	private final RevCommit revCommit;

	private String name;

	private IPath fullPath;

	private static final IWorkspaceRoot workspaceRoot = ResourcesPlugin
			.getWorkspace().getRoot();

	/**
	 * Construct Git representation of {@link IResourceVariant}.
	 *
	 * @param repo
	 * @param revCommit
	 * @param path
	 *            should be repository relative
	 * @throws IOException
	 */
	GitResourceVariant(Repository repo, RevCommit revCommit, String path)
			throws IOException {
		this.path = path;
		this.repo = repo;
		this.revCommit = revCommit;
		TreeWalk tw = getTreeWalk(repo, revCommit.getTree(), path);
		if (tw == null)
			objectId = null;
		else
			objectId = tw.getObjectId(0);
	}

	public String getContentIdentifier() {
		return objectId.getName();
	}

	public String getName() {
		if (name == null && path != null) {
			int lastSeparator = path.lastIndexOf('/');
			if (lastSeparator > -1)
				name = path.substring(lastSeparator + 1);
			else
				name = path;
		}

		return name;
	}

	@Override
	public int hashCode() {
		return objectId.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GitResourceVariant)
			return objectId.equals(((GitResourceVariant) obj).getObjectId());

		return false;
	}

	@Override
	public String toString() {
		return path + "(" + objectId.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 *
	 * @param repo
	 * @param revTree
	 *            base commit
	 * @param path
	 *            to resource variant
	 * @return new tree walk positioned on given object or <code>null</code>
	 *         when given path was not found in repository
	 * @throws IOException
	 *             when something goes wrong during tree walk initialization
	 */
	protected abstract TreeWalk getTreeWalk(Repository repo, RevTree revTree,
			String path) throws IOException;

	protected ObjectId getObjectId() {
		return objectId;
	}

	protected Repository getRepository() {
		return repo;
	}

	protected RevCommit getRevCommit() {
		return revCommit;
	}

	protected String getPath() {
		return path;
	}

	protected IPath getFullPath() {
		if (fullPath == null) {
			IResource resource;
			IPath location = new Path(repo.getWorkTree() + File.separator
					+ path);

			if (isContainer())
				resource = workspaceRoot.getContainerForLocation(location);
			else
				resource = workspaceRoot.getFileForLocation(location);

			if (resource != null)
				fullPath = resource.getFullPath();
			else
				fullPath = new Path(path);
		}

		return fullPath;
	}

	public boolean exists() {
		return objectId != null && !objectId.equals(ObjectId.zeroId());
	}

}
