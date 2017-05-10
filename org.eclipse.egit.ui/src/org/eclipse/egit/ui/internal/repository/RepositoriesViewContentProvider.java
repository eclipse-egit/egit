/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Laurent Goubet <laurent.goubet@obeo.fr - 404121
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchHierarchyNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.ErrorNode;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteTrackingNode;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.StashNode;
import org.eclipse.egit.ui.internal.repository.tree.SubmodulesNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.internal.repository.tree.command.ToggleBranchHierarchyCommand;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
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

	private Map<Repository, Map<String, Ref>> branchRefs = new WeakHashMap<>();

	private Map<Repository, ListenerHandle> refsChangedListeners = new WeakHashMap<>();

	/**
	 * Constructs this instance
	 */
	public RepositoriesViewContentProvider() {
		ICommandService srv = CommonUtils.getService(PlatformUI.getWorkbench(), ICommandService.class);
		commandState = srv.getCommand(
				ToggleBranchHierarchyCommand.ID)
				.getState(ToggleBranchHierarchyCommand.TOGGLE_STATE);
		commandState.addListener(this);
		try {
			this.branchHierarchyMode = ((Boolean) commandState.getValue())
					.booleanValue();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {

		List<RepositoryTreeNode> nodes = new ArrayList<>();
		List<String> directories = new ArrayList<>();
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

	@Override
	public void dispose() {
		commandState.removeListener(this);
		for (ListenerHandle handle : refsChangedListeners.values())
			handle.remove();
		refsChangedListeners.clear();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	@Override
	public Object[] getChildren(Object parentElement) {

		RepositoryTreeNode node = (RepositoryTreeNode) parentElement;
		Repository repo = node.getRepository();

		switch (node.getType()) {

		case BRANCHES: {
			List<RepositoryTreeNode> nodes = new ArrayList<>();
			nodes.add(new LocalNode(node, repo));
			nodes.add(new RemoteTrackingNode(node, repo));
			return nodes.toArray();
		}

		case LOCAL: {
			if (branchHierarchyMode) {
				BranchHierarchyNode hierNode = new BranchHierarchyNode(node,
						repo, new Path(Constants.R_HEADS));
				List<RepositoryTreeNode> children = new ArrayList<>();
				try {
					for (IPath path : hierNode.getChildPaths()) {
						children.add(new BranchHierarchyNode(node, node
								.getRepository(), path));
					}
					for (Ref ref : hierNode.getChildRefs()) {
						children.add(new RefNode(node, node.getRepository(),
								ref));
					}
				} catch (Exception e) {
					return handleException(e, node);
				}
				return children.toArray();
			} else {
				List<RepositoryTreeNode<Ref>> refs = new ArrayList<>();
				try {
					for (Entry<String, Ref> refEntry : getRefs(repo, Constants.R_HEADS).entrySet()) {
						if (!refEntry.getValue().isSymbolic())
							refs.add(new RefNode(node, repo, refEntry
									.getValue()));
					}
				} catch (Exception e) {
					return handleException(e, node);
				}
				return refs.toArray();
			}
		}

		case REMOTETRACKING: {
			if (branchHierarchyMode) {
				BranchHierarchyNode hierNode = new BranchHierarchyNode(node,
						repo, new Path(Constants.R_REMOTES));
				List<RepositoryTreeNode> children = new ArrayList<>();
				try {
					for (IPath path : hierNode.getChildPaths()) {
						children.add(new BranchHierarchyNode(node, node
								.getRepository(), path));
					}
					for (Ref ref : hierNode.getChildRefs()) {
						children.add(new RefNode(node, node.getRepository(),
								ref));
					}
				} catch (Exception e) {
					return handleException(e, node);
				}
				return children.toArray();
			} else {
				List<RepositoryTreeNode<Ref>> refs = new ArrayList<>();
				try {
					for (Entry<String, Ref> refEntry : getRefs(repo, Constants.R_REMOTES).entrySet()) {
						if (!refEntry.getValue().isSymbolic())
							refs.add(new RefNode(node, repo, refEntry
									.getValue()));
					}
				} catch (Exception e) {
					return handleException(e, node);
				}

				return refs.toArray();
			}
		}

		case BRANCHHIERARCHY: {
			BranchHierarchyNode hierNode = (BranchHierarchyNode) node;
			List<RepositoryTreeNode> children = new ArrayList<>();
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
			return getTagsChildren(node, repo);
		}

		case ADDITIONALREFS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<>();
			try {
				for (Entry<String, Ref> refEntry : getRefs(repo, RefDatabase.ALL).entrySet()) {
					String name=refEntry.getKey();
					if (!(name.startsWith(Constants.R_HEADS) || name.startsWith(Constants.R_TAGS)|| name.startsWith(Constants.R_REMOTES)))
						refs.add(new AdditionalRefNode(node, repo, refEntry
								.getValue()));
				}
				for (Ref r : repo.getRefDatabase().getAdditionalRefs())
					refs.add(new AdditionalRefNode(node, repo, r));
			} catch (Exception e) {
				return handleException(e, node);
			}
			return refs.toArray();
		}

		case REMOTES: {
			List<RepositoryTreeNode<String>> remotes = new ArrayList<>();

			Repository rep = node.getRepository();

			Set<String> configNames = rep.getConfig().getSubsections(
					RepositoriesView.REMOTE);

			for (String configName : configNames) {
				remotes.add(new RemoteNode(node, repo, configName));
			}

			return remotes.toArray();
		}

		case REPO: {

			List<RepositoryTreeNode<? extends Object>> nodeList = new ArrayList<>();
			nodeList.add(new BranchesNode(node, repo));
			nodeList.add(new TagsNode(node, repo));
			nodeList.add(new AdditionalRefsNode(node, repo));
			final boolean bare = repo.isBare();
			if (!bare)
				nodeList.add(new WorkingDirNode(node, repo));
			nodeList.add(new RemotesNode(node, repo));
			if(!bare && hasStashedCommits(repo))
				nodeList.add(new StashNode(node, repo));
			if (!bare && hasConfiguredSubmodules(repo))
				nodeList.add(new SubmodulesNode(node, repo));

			return nodeList.toArray();
		}

		case WORKINGDIR: {
			List<RepositoryTreeNode<File>> children = new ArrayList<>();

			if (node.getRepository().isBare())
				return children.toArray();
			File workingDir = repo.getWorkTree();
			if (!workingDir.exists())
				return children.toArray();

			File[] childFiles = workingDir.listFiles();
			Arrays.sort(childFiles, new Comparator<File>() {
				@Override
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
			List<RepositoryTreeNode<File>> children = new ArrayList<>();

			File parent = ((File) node.getObject());

			File[] childFiles = parent.listFiles();
			if (childFiles == null)
				return children.toArray();

			Arrays.sort(childFiles, new Comparator<File>() {
				@Override
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

			List<RepositoryTreeNode<String>> children = new ArrayList<>();

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
			if (uriCount > 0) {
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

		case SUBMODULES:
			List<RepositoryNode> children = new ArrayList<>();
			try {
				SubmoduleWalk walk = SubmoduleWalk.forIndex(node
						.getRepository());
				while (walk.next()) {
					Repository subRepo = walk.getRepository();
					if (subRepo != null) {
						Repository cachedRepo = null;
						try {
							cachedRepo = repositoryCache
								.lookupRepository(subRepo.getDirectory());
						} finally {
							subRepo.close();
						}
						if (cachedRepo != null)
							children.add(new RepositoryNode(node, cachedRepo));
					}
				}
			} catch (IOException e) {
				handleException(e, node);
			}
			return children.toArray();
		case STASH:
			List<StashedCommitNode> stashNodes = new ArrayList<>();
			int index = 0;
			try {
				for (RevCommit commit : Git.wrap(repo).stashList().call())
					stashNodes.add(new StashedCommitNode(node, repo, index++,
							commit));
			} catch (Exception e) {
				handleException(e, node);
			}
			return stashNodes.toArray();
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
		case STASHED_COMMIT:
			// fall through
		case ADDITIONALREF:
			return null;

		}

		return null;

	}

	private Object[] getTagsChildren(RepositoryTreeNode parentNode,
			Repository repo) {
		List<RepositoryTreeNode<Ref>> nodes = new ArrayList<>();

		try (RevWalk walk = new RevWalk(repo)) {
			walk.setRetainBody(true);
			Map<String, Ref> tagRefs = getRefs(repo, Constants.R_TAGS);
			for (Ref tagRef : tagRefs.values()) {
				ObjectId objectId = tagRef.getLeaf().getObjectId();
				RevObject revObject = walk.parseAny(objectId);
				RevObject peeledObject = walk.peel(revObject);
				TagNode tagNode = createTagNode(parentNode, repo, tagRef,
						revObject, peeledObject);
				nodes.add(tagNode);
			}
		} catch (IOException e) {
			return handleException(e, parentNode);
		}

		return nodes.toArray();
	}

	private TagNode createTagNode(RepositoryTreeNode parentNode,
			Repository repo, Ref ref, RevObject revObject,
			RevObject peeledObject) {
		boolean annotated = (revObject instanceof RevTag);
		if (peeledObject instanceof RevCommit) {
			RevCommit commit = (RevCommit) peeledObject;
			String id = commit.getId().name();
			String message = commit.getShortMessage();
			return new TagNode(parentNode, repo, ref, annotated, id, message);
		} else {
			return new TagNode(parentNode, repo, ref, annotated, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
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

	@Override
	public Object getParent(Object element) {
		if (element instanceof RepositoryTreeNode)
			return ((RepositoryTreeNode) element).getParent();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// for some of the nodes we can optimize this call
		RepositoryTreeNode node = (RepositoryTreeNode) element;
		Repository repo = node.getRepository();
		switch (node.getType()) {
		case BRANCHES:
			return true;
		case REPO:
			return true;
		case ADDITIONALREFS:
			return true;
		case SUBMODULES:
			return true;
		case TAGS:
			try {
				return !getRefs(repo, Constants.R_TAGS).isEmpty();
			} catch (IOException e) {
				return true;
			}
		case WORKINGDIR:
			if (node.getRepository().isBare())
				return false;
			File workingDir = repo.getWorkTree();
			if (!workingDir.exists())
				return false;
			return workingDir.listFiles().length > 0;
		default:
			Object[] children = getChildren(element);
			return children != null && children.length > 0;
		}
	}

	@Override
	public void handleStateChange(State state, Object oldValue) {
		try {
			this.branchHierarchyMode = ((Boolean) state.getValue())
					.booleanValue();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}

	private synchronized Map<String, Ref> getRefs(final Repository repo, final String prefix) throws IOException {
		Map<String, Ref> allRefs = branchRefs.get(repo);
		if (allRefs == null) {
			allRefs = repo.getRefDatabase().getRefs(RefDatabase.ALL);
			branchRefs.put(repo, allRefs);
			if (refsChangedListeners.get(repo) == null) {
				RefsChangedListener listener = new RefsChangedListener() {
					@Override
					public void onRefsChanged(RefsChangedEvent event) {
						synchronized (RepositoriesViewContentProvider.this) {
							branchRefs.remove(repo);
						}
					}
				};
				refsChangedListeners.put(repo, repo.getListenerList()
						.addRefsChangedListener(listener));
			}
		}
		if (prefix.equals(RefDatabase.ALL))
			return allRefs;

		Map<String, Ref> filtered = new HashMap<>();
		for (Map.Entry<String, Ref> entry : allRefs.entrySet()) {
			if (entry.getKey().startsWith(prefix))
				filtered.put(entry.getKey(), entry.getValue());
		}
		return filtered;
	}

	/**
	 * Does the repository have any submodule configurations?
	 * <p>
	 * This method checks for a '.gitmodules' file at the root of the working
	 * directory or any 'submodule' sections in the repository's config file
	 *
	 * @param repository
	 * @return true if submodules, false otherwise
	 */
	private boolean hasConfiguredSubmodules(final Repository repository) {
		if (new File(repository.getWorkTree(), Constants.DOT_GIT_MODULES)
				.isFile())
			return true;
		return !repository.getConfig()
				.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION)
				.isEmpty();
	}

	/**
	 * Does the repository have any stashed commits?
	 * <p>
	 * This method checks for a {@link Constants#R_STASH} ref in the given
	 * repository
	 *
	 * @param repository
	 * @return true if stashed commits, false otherwise
	 */
	private boolean hasStashedCommits(final Repository repository) {
		try {
			return repository.exactRef(Constants.R_STASH) != null;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Tells whether this content provider is using a hierarchical branch
	 * layout.
	 *
	 * @return {@code true} if this content provider uses a hierarchical branch
	 *         layout; {@code false} otherwise
	 */
	public boolean isHierarchical() {
		return branchHierarchyMode;
	}
}
