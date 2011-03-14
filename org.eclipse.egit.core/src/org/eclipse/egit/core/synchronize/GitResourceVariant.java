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
import org.eclipse.team.core.variants.IResourceVariant;

abstract class GitResourceVariant implements IResourceVariant {

	private final String path;

	private final Repository repo;

	private final ObjectId objectId;

	private final RevCommit revCommit;

	private String name;

	private IPath location;

	private static final IWorkspaceRoot workspaceRoot = ResourcesPlugin
			.getWorkspace().getRoot();

	/**
	 * Construct Git representation of {@link IResourceVariant}.
	 *
	 * @param repo
	 * @param revCommit
	 *            associated with this resource varinat
	 * @param objectId
	 * @param path
	 *            should be repository relative
	 * @throws IOException
	 */
	GitResourceVariant(Repository repo, RevCommit revCommit, ObjectId objectId,
			String path) throws IOException {
		this.path = path;
		this.repo = repo;
		this.objectId = objectId;
		this.revCommit = revCommit;
	}

	public String getContentIdentifier() {
		return revCommit.abbreviate(7).name() + "..."; //$NON-NLS-1$
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

	public RevCommit getRevCommit() {
		return revCommit;
	}

	@Override
	public int hashCode() {
		return objectId.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitResourceVariant)
			return objectId.equals(((GitResourceVariant) obj).getObjectId());

		return false;
	}

	public byte[] asBytes() {
		return getObjectId().getName().getBytes();
	}

	@Override
	public String toString() {
		return path + "(" + objectId.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected ObjectId getObjectId() {
		return objectId;
	}

	protected Repository getRepository() {
		return repo;
	}

	protected String getPath() {
		return path;
	}

	public Object getLocation() {
		if (location == null) {
			IResource resource;
			IPath absolutePath = new Path(repo.getWorkTree() + File.separator
					+ path);

			if (isContainer())
				resource = workspaceRoot.getContainerForLocation(absolutePath);
			else
				resource = workspaceRoot.getFileForLocation(absolutePath);

			if (resource != null && resource.exists())
				location = resource.getLocation();
			else
				location = new Path(path);
		}

		return location;
	}


	public boolean exists() {
		return true;
	}

}
