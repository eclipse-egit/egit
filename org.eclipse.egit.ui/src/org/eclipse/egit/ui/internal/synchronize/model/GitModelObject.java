/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.errors.MissingObjectException;

/**
 * Abstract representation of all objects that are included in Git ChangeSet
 * model.
 */
public abstract class GitModelObject extends PlatformObject {

	private final GitModelObject parent;

	/**
	 * Creates root of Git ChangeSet model.
	 *
	 * @param data
	 *            synchronization data
	 * @return Git ChangeSet model root
	 * @throws IOException
	 * @throws MissingObjectException
	 */
	public static GitModelObject createRoot(GitSynchronizeData data)
			throws MissingObjectException, IOException {
		return new GitModelRepository(data);
	}

	/**
	 * @param parent
	 */
	public GitModelObject(GitModelObject parent) {
		this.parent = parent;
	}

	/**
	 * @return parent
	 */
	public GitModelObject getParent() {
		return parent;
	}

	/**
	 * @return repository hash code
	 */
	public abstract int repositoryHashCode();

	/**
	 * @return children of particular model object
	 */
	public abstract GitModelObject[] getChildren();

	/**
	 * Returns name of model object, in case of:
	 * <ul>
	 * <li>root node it will return repository path
	 * <li>commit node it will return first 6 characters of commit's SHA-1
	 * connected with short commit message
	 * <li>tree node it will return folder name
	 * <li>blob node it will return file name
	 * </ul>
	 *
	 * @return name of model object
	 */
	public abstract String getName();

	/**
	 *
	 * @return change kind
	 */
	public abstract int getKind();

	/**
	 * @return location of resource associated with particular model object
	 */
	public abstract IPath getLocation();

	/**
	 * Answers if model object may have children.
	 *
	 * @return <code>true</code> if the model object may have children and
	 *         <code>false</code> otherwise.
	 */
	public abstract boolean isContainer();

	/**
	 * Disposed all nested resources
	 */
	public abstract void dispose();

}
