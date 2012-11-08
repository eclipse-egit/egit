/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents a File in the working directory tree
 */
public class FileNode extends RepositoryTreeNode<File> {

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param file
	 *            the file
	 */
	public FileNode(RepositoryTreeNode parent, Repository repository, File file) {
		super(parent, RepositoryTreeNodeType.FILE, repository, file);
	}

	@Override
	public IPath getPath() {
		return new Path(getObject().getAbsolutePath());
	}
}
