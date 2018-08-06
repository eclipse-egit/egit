/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.lib.Repository;

/**
 * For building trees of directory and file nodes out of a flat list of changes.
 */
class TreeBuilder {

	/**
	 * This interface enables creating the right instances of
	 * {@link GitModelBlob} for files.
	 */
	interface FileModelFactory {
		/**
		 * Creates proper instance of {@link GitModelBlob} for file nodes
		 *
		 * @param parent
		 *            parent object
		 * @param repo
		 *            repository associated with file that will be created
		 * @param change
		 *            change associated with file that will be created
		 * @param fullPath
		 *            absolute path
		 * @return instance of {@link GitModelBlob}
		 */
		GitModelBlob createFileModel(GitModelObjectContainer parent,
				Repository repo, Change change, IPath fullPath);

		/**
		 * Distinguish working tree from changed/staged tree
		 *
		 * @return {@code true} when this tree is working tree, {@code false}
		 *         when it is a cached tree
		 */
		boolean isWorkingTree();
	}

	/**
	 * Interface for creating the desired instances of {@link GitModelTree}.
	 */
	interface TreeModelFactory {
		GitModelTree createTreeModel(GitModelObjectContainer parent,
				IPath fullPath, int kind);
	}

	/**
	 *
	 * @param root
	 *            the root node of the tree to build, which will become the
	 *            parent of the first level of children
	 * @param repo
	 * @param changes
	 * @param fileFactory
	 * @param treeFactory
	 * @return the children of the root nodes
	 */
	public static GitModelObject[] build(final GitModelObjectContainer root,
			final Repository repo, final Map<String, Change> changes,
			final FileModelFactory fileFactory,
			final TreeModelFactory treeFactory) {

		if (changes == null || changes.isEmpty())
			return new GitModelObject[] {};

		final IPath rootPath = new Path(repo.getWorkTree()
				.getAbsolutePath());
		final List<GitModelObject> rootChildren = new ArrayList<>();

		final Map<IPath, Node> nodes = new HashMap<>();

		for (Map.Entry<String, Change> entry : changes.entrySet()) {
			String repoRelativePath = entry.getKey();
			Change change = entry.getValue();

			GitModelObjectContainer parent = root;
			List<GitModelObject> children = rootChildren;
			IPath path = rootPath;

			String[] segments = repoRelativePath.split("/"); //$NON-NLS-1$

			for (int i = 0; i < segments.length; i++) {
				path = path.append(segments[i]);

				// Changes represent files, so the last segment is the file name
				boolean fileNode = (i == segments.length - 1);
				if (!fileNode) {
					Node node = nodes.get(path);
					if (node == null) {
						GitModelTree tree = treeFactory.createTreeModel(parent,
								path, change.getKind());
						node = new Node(tree);
						nodes.put(path, node);
						children.add(tree);
					}
					parent = node.tree;
					children = node.children;
				} else {
					GitModelBlob file = fileFactory.createFileModel(parent,
							repo, change, path);
					children.add(file);
				}
			}
		}

		for (Node object : nodes.values()) {
			GitModelTree tree = object.tree;
			tree.setChildren(object.children);
		}

		return rootChildren.toArray(new GitModelObject[0]);
	}

	private static class Node {
		private final GitModelTree tree;

		private final List<GitModelObject> children = new ArrayList<>();

		public Node(GitModelTree tree) {
			this.tree = tree;
		}
	}
}
