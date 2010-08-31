/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.BranchHierarchyNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.ErrorNode;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.SymbolicRefNode;
import org.eclipse.egit.ui.internal.repository.tree.SymbolicRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Content Provider for the Git Repositories View
 */
public class RepositoriesViewContentProvider implements ITreeContentProvider,
		IStateListener {

	private final RepositoryCache repositoryCache = org.eclipse.egit.core.Activator
			.getDefault().getRepositoryCache();

	private final State commandState;

	private boolean branchHierarchyMode = false;

	/**
	 * Constructs this instance
	 */
	public RepositoriesViewContentProvider() {
		ICommandService srv = (ICommandService) PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		commandState = srv.getCommand(
				"org.eclipse.egit.ui.RepositoriesToggleBranchHierarchy") //$NON-NLS-1$
				.getState("org.eclipse.ui.commands.toggleState"); //$NON-NLS-1$
		commandState.addListener(this);
		try {
			this.branchHierarchyMode = ((Boolean) commandState.getValue())
					.booleanValue();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}

	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {

		List<RepositoryTreeNode> nodes = new ArrayList<RepositoryTreeNode>();
		List<String> directories = new ArrayList<String>();
		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();

		if (inputElement instanceof Collection) {
			for (Iterator it = ((Collection) inputElement).iterator(); it
					.hasNext();) {
				Object next = it.next();
				if (next instanceof RepositoryTreeNode)
					nodes.add((RepositoryTreeNode) next);
				else if (next instanceof String)
					directories.add((String) next);
			}
		} else if (inputElement instanceof IWorkspaceRoot) {
			directories.addAll(repositoryUtil.getConfiguredRepositories());
		}

		for (String directory : directories) {
			try {
				File gitDir = new File(directory);
				if (gitDir.exists()) {
					RepositoryNode rNode = new RepositoryNode(null,
							repositoryCache.lookupRepository(gitDir));
					nodes.add(rNode);
				} else
					repositoryUtil.removeDir(gitDir);
			} catch (IOException e) {
				// ignore for now
			}
		}

		Collections.sort(nodes);
		return nodes.toArray();
	}

	public void dispose() {
		commandState.removeListener(this);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	public Object[] getChildren(Object parentElement) {

		RepositoryTreeNode node = (RepositoryTreeNode) parentElement;
		Repository repo = node.getRepository();

		switch (node.getType()) {

		case BRANCHES: {
			List<RepositoryTreeNode> nodes = new ArrayList<RepositoryTreeNode>();
			nodes.add(new LocalBranchesNode(node, repo));
			nodes.add(new RemoteBranchesNode(node, repo));
			return nodes.toArray();
		}

		case LOCALBRANCHES: {
			if (branchHierarchyMode) {
				BranchHierarchyNode hierNode = new BranchHierarchyNode(node,
						repo, new Path(Constants.R_HEADS));
				List<RepositoryTreeNode> children = new ArrayList<RepositoryTreeNode>();
				try {
					for (IPath path : hierNode.getChildPaths()) {
						children.add(new BranchHierarchyNode(node, node
								.getRepository(), path));
					}
					for (Ref ref : hierNode.getChildRefs()) {
						children.add(new RefNode(node, node.getRepository(),
								ref));
					}
				} catch (IOException e) {
					return handleException(e, node);
				}
				return children.toArray();
			} else {
				List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();
				try {
					for (Entry<String, Ref> refEntry : repo.getRefDatabase()
							.getRefs(Constants.R_HEADS).entrySet()) {
						if (!refEntry.getValue().isSymbolic())
							refs.add(new RefNode(node, repo, refEntry
									.getValue()));
					}
				} catch (IOException e) {
					return handleException(e, node);
				}
				return refs.toArray();
			}
		}

		case REMOTEBRANCHES: {
			if (branchHierarchyMode) {
				BranchHierarchyNode hierNode = new BranchHierarchyNode(node,
						repo, new Path(Constants.R_REMOTES));
				List<RepositoryTreeNode> children = new ArrayList<RepositoryTreeNode>();
				try {
					for (IPath path : hierNode.getChildPaths()) {
						children.add(new BranchHierarchyNode(node, node
								.getRepository(), path));
					}
					for (Ref ref : hierNode.getChildRefs()) {
						children.add(new RefNode(node, node.getRepository(),
								ref));
					}
				} catch (IOException e) {
					return handleException(e, node);
				}
				return children.toArray();
			} else {
				List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();
				try {
					for (Entry<String, Ref> refEntry : repo.getRefDatabase()
							.getRefs(Constants.R_REMOTES).entrySet()) {
						if (!refEntry.getValue().isSymbolic())
							refs.add(new RefNode(node, repo, refEntry
									.getValue()));
					}
				} catch (IOException e) {
					return handleException(e, node);
				}

				return refs.toArray();
			}
		}

		case BRANCHHIERARCHY: {
			BranchHierarchyNode hierNode = (BranchHierarchyNode) node;
			List<RepositoryTreeNode> children = new ArrayList<RepositoryTreeNode>();
			try {
				for (IPath path : hierNode.getChildPaths()) {
					children.add(new BranchHierarchyNode(node, node
							.getRepository(), path));
				}
				for (Ref ref : hierNode.getChildRefs()) {
					children.add(new RefNode(node, node.getRepository(), ref));
				}
			} catch (IOException e) {
				return handleException(e, node);
			}
			return children.toArray();
		}

		case TAGS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(Constants.R_TAGS).entrySet()) {
					refs.add(new TagNode(node, repo, refEntry.getValue()));
				}
			} catch (IOException e) {
				return handleException(e, node);
			}

			return refs.toArray();
		}

		case SYMBOLICREFS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(RefDatabase.ALL).entrySet()) {
					if (refEntry.getValue().isSymbolic())
						refs.add(new SymbolicRefNode(node, repo, refEntry
								.getValue()));
				}
			} catch (IOException e) {
				return handleException(e, node);
			}

			return refs.toArray();
		}

		case REMOTES: {
			List<RepositoryTreeNode<String>> remotes = new ArrayList<RepositoryTreeNode<String>>();

			Repository rep = node.getRepository();

			Set<String> configNames = rep.getConfig().getSubsections(
					RepositoriesView.REMOTE);

			for (String configName : configNames) {
				remotes.add(new RemoteNode(node, repo, configName));
			}

			return remotes.toArray();
		}

		case REPO: {

			List<RepositoryTreeNode<? extends Object>> nodeList = new ArrayList<RepositoryTreeNode<? extends Object>>();

			nodeList.add(new BranchesNode(node, repo));
			nodeList.add(new TagsNode(node, repo));
			nodeList.add(new SymbolicRefsNode(node, repo));
			nodeList.add(new WorkingDirNode(node, repo));
			nodeList.add(new RemotesNode(node, repo));

			return nodeList.toArray();
		}

		case WORKINGDIR: {
			List<RepositoryTreeNode<File>> children = new ArrayList<RepositoryTreeNode<File>>();

			if (node.getRepository().isBare())
				return children.toArray();
			File workingDir = repo.getWorkTree();
			if (workingDir == null || !workingDir.exists())
				return children.toArray();

			File[] childFiles = workingDir.listFiles();
			Arrays.sort(childFiles, new Comparator<File>() {
				public int compare(File o1, File o2) {
					if (o1.isDirectory()) {
						if (o2.isDirectory()) {
							return o1.compareTo(o2);
						}
						return -1;
					} else if (o2.isDirectory()) {
						return 1;
					}
					return o1.compareTo(o2);
				}
			});
			for (File file : childFiles) {
				if (file.isDirectory()) {
					children.add(new FolderNode(node, repo, file));
				} else {
					children.add(new FileNode(node, repo, file));
				}
			}

			return children.toArray();
		}

		case FOLDER: {
			List<RepositoryTreeNode<File>> children = new ArrayList<RepositoryTreeNode<File>>();

			File parent = ((File) node.getObject());

			File[] childFiles = parent.listFiles();
			Arrays.sort(childFiles, new Comparator<File>() {
				public int compare(File o1, File o2) {
					if (o1.isDirectory()) {
						if (o2.isDirectory()) {
							return o1.compareTo(o2);
						}
						return -1;
					} else if (o2.isDirectory()) {
						return 1;
					}
					return o1.compareTo(o2);
				}
			});
			for (File file : childFiles) {
				if (file.isDirectory()) {
					children.add(new FolderNode(node, repo, file));
				} else {
					children.add(new FileNode(node, repo, file));
				}
			}

			return children.toArray();
		}

		case REMOTE: {

			List<RepositoryTreeNode<String>> children = new ArrayList<RepositoryTreeNode<String>>();

			String remoteName = (String) node.getObject();
			RemoteConfig rc;
			try {
				rc = new RemoteConfig(node.getRepository().getConfig(),
						remoteName);
			} catch (URISyntaxException e) {
				return handleException(e, node);
			}

			if (!rc.getURIs().isEmpty())
				children.add(new FetchNode(node, node.getRepository(), rc
						.getURIs().get(0).toPrivateString()));

			int uriCount = rc.getPushURIs().size();
			if (uriCount == 0 && !rc.getURIs().isEmpty())
				uriCount++;

			// show push if either a fetch or push URI is specified and
			// at least one push specification
			if (uriCount > 0 && !rc.getPushRefSpecs().isEmpty()) {
				URIish firstUri;
				if (!rc.getPushURIs().isEmpty())
					firstUri = rc.getPushURIs().get(0);
				else
					firstUri = rc.getURIs().get(0);

				if (uriCount == 1)
					children.add(new PushNode(node, node.getRepository(),
							firstUri.toPrivateString()));
				else
					children.add(new PushNode(node, node.getRepository(),
							firstUri.toPrivateString() + "...")); //$NON-NLS-1$
			}
			return children.toArray();

		}

		case FILE:
			// fall through
		case REF:
			// fall through
		case PUSH:
			// fall through
		case TAG:
			// fall through
		case FETCH:
			// fall through
		case ERROR:
			// fall through
		case SYMBOLICREF:
			return null;

		}

		return null;

	}

	private Object[] handleException(Exception e, RepositoryTreeNode parentNode) {
		Activator.handleError(e.getMessage(), e, false);
		// add a node indicating that there was an Exception
		String message = e.getMessage();
		if (message == null)
			return new Object[] { new ErrorNode(parentNode, parentNode
					.getRepository(),
					UIText.RepositoriesViewContentProvider_ExceptionNodeText) };
		else
			return new Object[] { new ErrorNode(parentNode, parentNode
					.getRepository(), message) };
	}

	public Object getParent(Object element) {
		if (element instanceof RepositoryTreeNode)
			return ((RepositoryTreeNode) element).getParent();
		return null;
	}

	public boolean hasChildren(Object element) {
		// for some of the nodes we can optimize this call
		RepositoryTreeNode node = (RepositoryTreeNode) element;
		Repository repo = node.getRepository();
		switch (node.getType()) {
		case BRANCHES:
			return true;
		case REPO:
			return true;
		case SYMBOLICREFS:
			try {
				for (Ref refEntry : repo.getRefDatabase().getRefs(
						RefDatabase.ALL).values()) {
					if (refEntry.isSymbolic())
						return true;
				}
			} catch (IOException e) {
				// true so that the node can be opened
				return true;
			}
			return false;
		case TAGS:
			try {
				return !repo.getRefDatabase().getRefs(Constants.R_TAGS)
						.isEmpty();
			} catch (IOException e) {
				return true;
			}
		case WORKINGDIR:
			if (node.getRepository().isBare())
				return false;
			File workingDir = repo.getWorkTree();
			if (workingDir == null || !workingDir.exists())
				return false;
			return workingDir.listFiles().length > 0;
		default:
			Object[] children = getChildren(element);
			return children != null && children.length > 0;
		}
	}

	public void handleStateChange(State state, Object oldValue) {
		try {
			this.branchHierarchyMode = ((Boolean) state.getValue())
					.booleanValue();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}
}
