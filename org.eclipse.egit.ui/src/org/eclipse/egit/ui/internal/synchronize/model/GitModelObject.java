/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.treewalk.filter.TreeFilter.ANY_DIFF;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Abstract representation of all object that are included in Git ChangeSet
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
	 * @return child's of particular model object
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
	 * @return list of projects that are connected with particular object model
	 */
	public abstract IProject[] getProjects();

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
	 * Refresh child of this item
	 */
	public abstract void refresh();

	/**
	 *
	 * @param parent
	 *            of particular model object
	 */
	protected GitModelObject(GitModelObject parent) {
		this.parent = parent;
	}

	/**
	 * @return parent of particular model object, or <code>null</code> if object
	 *         is root node
	 */
	public GitModelObject getParent() {
		return parent;
	}

	/**
	 * @return repository associated with particular model object
	 */
	public Repository getRepository() {
		return parent.getRepository();
	}

	/**
	 * Returns preinitialized instance of {@link TreeWalk}. Set of
	 * initialization call's is common for all model object's.
	 *
	 * @return tree walk
	 */
	protected TreeWalk createTreeWalk() {
		Repository repo = getRepository();
		TreeWalk tw = new TreeWalk(repo);

		tw.reset();
		tw.setRecursive(false);

		tw.setFilter(ANY_DIFF);
		return tw;
	}

}
