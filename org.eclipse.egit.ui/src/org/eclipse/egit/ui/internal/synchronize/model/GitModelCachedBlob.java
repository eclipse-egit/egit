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

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 *
 *
 */
public class GitModelCachedBlob extends GitModelBlob {

	private final String path;

	/**
	 * @param parent
	 * @param baseCommit
	 * @param repoId
	 * @param cached
	 * @param path
	 * @throws IOException
	 */
	protected GitModelCachedBlob(GitModelObjectContainer parent,
			RevCommit baseCommit, ObjectId repoId, ObjectId cached, String path)
			throws IOException {
		super(parent, baseCommit, repoId, repoId, cached, path);
		this.path = path;
	}

	@Override
	public String getName() {
		return path;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	@Override
	public boolean isContainer() {
		return false;
	}

}
