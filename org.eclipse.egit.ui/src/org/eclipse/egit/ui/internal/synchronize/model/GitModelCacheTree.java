/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.CHANGE;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.synchronize.model.TreeBuilder.FileModelFactory;
import org.eclipse.jgit.lib.Repository;

/**
 * Because Git cache holds changes on file level (SHA-1 of trees are same as in
 * repository) we must emulate tree representation of staged files.
 */
public class GitModelCacheTree extends GitModelTree {

	private final FileModelFactory factory;

	/**
	 * @param parent
	 *            parent object
	 * @param repo
	 *            repository associated with this object parent object
	 * @param fullPath
	 *            absolute path of object
	 * @param factory
	 */
	public GitModelCacheTree(GitModelObjectContainer parent, Repository repo,
			IPath fullPath, FileModelFactory factory) {
		super(parent, fullPath, RIGHT | CHANGE);
		this.factory = factory;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (obj.getClass() != getClass())
			return false;

		GitModelCacheTree objTree = (GitModelCacheTree) obj;
		return path.equals(objTree.path)
				&& factory.isWorkingTree() == objTree.factory.isWorkingTree();
	}

	@Override
	public int hashCode() {
		return path.hashCode() + (factory.isWorkingTree() ? 31 : 41);
	}

	@Override
	public String toString() {
		return "GitModelCacheTree[" + getLocation() + ", isWorkingTree:" + factory.isWorkingTree() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Distinguish working tree from cached/staged tree
	 *
	 * @return {@code true} when this tree is working tree, {@code false}
	 *         when it is a cached tree
	 */
	public boolean isWorkingTree() {
		return factory.isWorkingTree();
	}

}
