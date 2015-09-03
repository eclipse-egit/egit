/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

//CHECKSTYLE:OFF
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.storage.GitBlobStorage;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * A {@link GitBlobStorage} that is also aware of its workspace-relative path.
 *
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 * @author <a href="mailto:laurent.delaigue@obeo.fr">Laurent Delaigue</a>
 */
public class WorkspaceGitBlobStorage extends GitBlobStorage {

	/** Workspace-relative path of the underlying object. */
	private final IPath workspacePath;

	/**
	 * @param repository
	 *            The repository containing this object.
	 * @param path
	 *            Repository-relative path of the underlying object. This path is not validated by this class,
	 *            i.e. it's returned as is by {@code #getAbsolutePath()} and {@code #getFullPath()} without
	 *            validating if the blob is reachable using this path.
	 * @param workspacePath
	 *            Workspace-relative path of the underlying object. This path is not validated by this class,
	 *            it's returned as is without validating if the blob is reachable using this path.
	 * @param blob
	 *            Id of this object in its repository.
	 */
	public WorkspaceGitBlobStorage(Repository repository, String path, IPath workspacePath, ObjectId blob) {
		super(repository, path, blob);
		this.workspacePath = workspacePath;
	}

	/**
	 * Returns a workspace-relative path of the underlying object.
	 *
	 * @return a workspace-relative path of the underlying object.
	 */
	public IPath getWorkspacePath() {
		return workspacePath;
	}
}
// CHECKSTYLE:ON
