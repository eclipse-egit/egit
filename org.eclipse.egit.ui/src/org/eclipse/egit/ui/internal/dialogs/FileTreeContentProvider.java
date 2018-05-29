/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - improve UI responsiveness
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * Builds the nodes for a file tree
 *
 */
public class FileTreeContentProvider implements ITreeContentProvider {
	private final Repository repository;

	private final List<Node> rootNodes = new ArrayList<>();

	private List<String> input;

	private Mode mode = Mode.REPO_RELATIVE_PATHS;

	private List<IPath> projectPaths;

	private Image folderImage = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);

	private Image fileImage = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE);

	private Image projectImage = PlatformUI.getWorkbench().getSharedImages()
			.getImage(SharedImages.IMG_OBJ_PROJECT);

	/**
	 * Describe how to show the paths
	 *
	 */
	public enum Mode {
		/**
		 * The paths are shown relative to the repository (default)
		 */
		REPO_RELATIVE_PATHS,
		/**
		 * The paths are shown as full file system paths
		 */
		FULL_PATHS,
		/**
		 * The paths are shown as resource paths (relative to the workspace)
		 */
		RESOURCE_PATHS;
	}

	/**
	 * The node
	 */
	public abstract class Node {
		private final String myName;

		private final Node myParent;

		/**
		 * Child list
		 */
		protected final List<Node> myChildren = new ArrayList<>();

		Node(Node parent, String name) {
			myParent = parent;
			myName = name;
			if (parent != null)
				parent.addChild(this);
		}

		/**
		 * @return a boolean
		 */
		public abstract boolean hasChildren();

		/**
		 * @return an image
		 */
		public abstract Image getImage();

		/**
		 * @return the name
		 */
		public String getName() {
			return myName;
		}

		/**
		 * @return the parent
		 */
		public Node getParent() {
			return myParent;
		}

		private void addChild(Node child) {
			myChildren.add(child);
		}

		/**
		 * @return the children or null
		 */
		public List<Node> getChildren() {
			if (myChildren != null)
				return myChildren;
			return null;
		}

		@Override
		public String toString() {
			return myName;
		}
	}

	private class FolderNode extends Node {
		FolderNode(Node parent, String name) {
			super(parent, name);
		}

		@Override
		public boolean hasChildren() {
			return true;
		}

		@Override
		public Image getImage() {
			if (mode == Mode.RESOURCE_PATHS
					&& getParent() instanceof VirtualNode)
				return projectImage;
			return folderImage;
		}

	}

	private class NoResourceNode extends Node {
		NoResourceNode(Node parent, String name) {
			super(parent, name);
		}

		@Override
		public boolean hasChildren() {
			return true;
		}

		@Override
		public Image getImage() {
			return folderImage;
		}

	}

	class FileNode extends Node {
		FileNode(Node parent, String name) {
			super(parent, name);
		}

		@Override
		public boolean hasChildren() {
			return false;
		}

		@Override
		public Image getImage() {
			return fileImage;
		}
	}

	private class VirtualNode extends FolderNode {
		VirtualNode() {
			super(null, ""); //$NON-NLS-1$
		}

		@Override
		public boolean hasChildren() {
			return !myChildren.isEmpty();
		}
	}

	/**
	 * @param repository
	 */
	public FileTreeContentProvider(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @param mode
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public Object[] getChildren(Object parent) {
		return ((Node) parent).getChildren().toArray();
	}

	@Override
	public Object getParent(Object child) {
		return ((Node) child).getParent();
	}

	@Override
	public boolean hasChildren(Object parent) {
		return ((Node) parent).hasChildren();
	}

	@Override
	public Object[] getElements(Object arg0) {
		return rootNodes.toArray();
	}

	@Override
	public void dispose() {
		// nothing to dispose
	}

	@Override
	@SuppressWarnings({ "unchecked", "unused" })
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		rootNodes.clear();

		input = (List<String>) newInput;
		if (input == null)
			return;

		VirtualNode virtualNode = new VirtualNode();
		Node parentNode = virtualNode;
		NoResourceNode noResourceNode = null;

		IPath repositoryRoot = new Path(repository.getWorkTree()
				.getAbsolutePath());

		for (String pathString : input) {
			IPath path;
			if (mode == Mode.FULL_PATHS)
				path = repositoryRoot.append(pathString);
			else if (mode == Mode.RESOURCE_PATHS)
				path = resolveResourcePath(repositoryRoot.append(pathString));
			else
				path = new Path(pathString);
			if (path == null) {
				path = repositoryRoot.append(pathString);
				if (noResourceNode == null) {
					noResourceNode = new NoResourceNode(
							virtualNode,
							UIText.FileTreeContentProvider_NonWorkspaceResourcesNode);
				}
				parentNode = noResourceNode;
			} else
				parentNode = virtualNode;
			for (int i = 0; i < path.segmentCount(); i++) {
				String segment = path.segment(i);
				Node foundNode = null;
				for (Node node : parentNode.getChildren()) {
					if (node.getName().equals(segment)
							&& !(node instanceof FileNode)) {
						foundNode = node;
						break;
					}
				}
				if (foundNode == null) {
					if (i < path.segmentCount() - 1)
						parentNode = new FolderNode(parentNode, segment);
					else
						new FileNode(parentNode, segment);
				} else {
					parentNode = foundNode;
				}
			}
		}

		rootNodes.addAll(virtualNode.getChildren());
	}

	private IPath resolveResourcePath(IPath fullPath) {
		for (IPath projectPath : getProjectPaths()) {
			if (projectPath.isPrefixOf(fullPath)) {
				return fullPath
						.removeFirstSegments(projectPath.segmentCount() - 1);
			}
		}
		return null;
	}

	private List<IPath> getProjectPaths() {
		if (projectPaths == null) {
			projectPaths = new ArrayList<>();
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
					.getProjects()) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(project);
				if (mapping != null
						&& mapping.getRepository().equals(repository)) {
					projectPaths.add(project.getLocation());
				}
			}
		}
		return projectPaths;
	}
}
