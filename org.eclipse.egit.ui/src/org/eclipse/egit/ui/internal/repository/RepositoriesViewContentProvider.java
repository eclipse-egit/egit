/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Laurent Goubet <laurent.goubet@obeo.fr - 404121
 *    Alexander Nittka <alex@nittka.de> - 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.ToggleCommand;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchHierarchyNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
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
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.StashNode;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.SubmodulesNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
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
import org.eclipse.ui.handlers.RegistryToggleState;

/**
 * Content Provider for the Git Repositories View
 */
public class RepositoriesViewContentProvider implements ITreeContentProvider {

	private static final Object[] NO_CHILDREN = new Object[0];

	private final RepositoryCache repositoryCache = org.eclipse.egit.core.Activator
			.getDefault().getRepositoryCache();

	private final State branchHierarchy;

	private boolean showUnbornHead = false;

	private boolean showRepositoryGroups = false;

	private RefCache.Cache refCache = RefCache.get();

	/**
	 * Constructs a new {@link RepositoriesViewContentProvider} that doesn't
	 * show an unborn branch as HEAD.
	 */
	public RepositoriesViewContentProvider() {
		this(false);
	}

	/**
	 * Constructs a new {@link RepositoriesViewContentProvider}.
	 *
	 * @param showUnbornHead
	 *            whether to show HEAD even if it is an unborn branch
	 */
	public RepositoriesViewContentProvider(boolean showUnbornHead) {
		super();
		this.showUnbornHead = showUnbornHead;
		ICommandService srv = PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		branchHierarchy = srv.getCommand(ToggleCommand.BRANCH_HIERARCHY_ID)
				.getState(RegistryToggleState.STATE_ID);
	}

	/**
	 * Fluent API for configuring the content provider to show repository groups
	 * or not.
	 *
	 * @param showGroups
	 *            whether to show repository groups
	 * @return the content provider itself
	 */
	public RepositoriesViewContentProvider showingRepositoryGroups(
			boolean showGroups) {
		this.showRepositoryGroups = showGroups;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {

		List<RepositoryTreeNode> nodes = new ArrayList<>();
		List<File> directories = new ArrayList<>();
		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		RepositoryGroups groupsUtil = RepositoryGroups.getInstance();

		if (inputElement instanceof Collection) {
			for (Object next : ((Collection) inputElement)) {
				if (next instanceof RepositoryTreeNode) {
					nodes.add((RepositoryTreeNode) next);
				} else if (next instanceof String) {
					directories.add(new File((String) next));
				}
			}
		} else if (inputElement instanceof IWorkspaceRoot) {
			directories.addAll(repositoryUtil.getConfiguredRepositories()
					.stream().map(File::new).collect(Collectors.toList()));
		}

		nodes.addAll(
				getRepositoryNodes(repositoryUtil, groupsUtil, null,
						directories));
		if (showRepositoryGroups) {
			for (RepositoryGroup group : groupsUtil.getGroups()) {
				nodes.add(new RepositoryGroupNode(group));
			}
		}

		Collections.sort(nodes);
		return nodes.toArray();
	}

	@Override
	public void dispose() {
		refCache.dispose();
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

		case LOCAL:
			return getBranchChildren(node, repo, Constants.R_HEADS);

		case REMOTETRACKING:
			return getBranchChildren(node, repo, Constants.R_REMOTES);

		case BRANCHHIERARCHY:
			return getBranchHierarchyChildren(node, repo,
					((BranchHierarchyNode) node).getObject()
							.toPortableString());

		case TAGS:
			return getTagsChildren(node, repo);

		case ADDITIONALREFS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<>();
			try {
				for (Entry<String, Ref> refEntry : getRefs(repo, RefDatabase.ALL).entrySet()) {
					String name=refEntry.getKey();
					if (!name.startsWith(Constants.R_HEADS) && !name.startsWith(Constants.R_TAGS) && !name.startsWith(Constants.R_REMOTES))
						refs.add(new AdditionalRefNode(node, repo, refEntry
								.getValue()));
				}
				for (Ref r : refCache.additional(repo)) {
					refs.add(new AdditionalRefNode(node, repo, r));
				}
				if (showUnbornHead) {
					Ref head = repo.exactRef(Constants.HEAD);
					if (head != null && head.isSymbolic()
							&& head.getObjectId() == null) {
						refs.add(new AdditionalRefNode(node, repo, head));
					}
				}
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

		case REPOGROUP: {
			List<File> repoDirs = ((RepositoryGroupNode) node).getObject()
					.getRepositoryDirectories();
			return getRepositoryNodes(
					Activator.getDefault().getRepositoryUtil(), null, node,
					repoDirs).toArray();
		}

		case WORKINGDIR:
			if (repo.isBare()) {
				return NO_CHILDREN;
			}
			return getDirectoryChildren(node, repo.getWorkTree());

		case FOLDER:
			return getDirectoryChildren(node, (File) node.getObject());

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

		case SUBMODULES: {
			List<RepositoryNode> children = new ArrayList<>();
			Repository repository = node.getRepository();
			try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repository)) {
				walk.setBuilderFactory(
						() -> repositoryCache.getBuilder(false, false));
				while (walk.next()) {
					Repository submodule = walk.getRepository();
					if (submodule != null) {
						children.add(new RepositoryNode(node, submodule));
					}
				}
			} catch (IOException e) {
				handleException(e, node);
			}
			return children.toArray();
		}

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

	private List<RepositoryNode> getRepositoryNodes(RepositoryUtil util,
			RepositoryGroups groupsUtil, RepositoryTreeNode<?> parent,
			List<File> directories) {
		List<RepositoryNode> result = new ArrayList<>();
		for (File gitDir : directories) {
			try {
				if (gitDir.exists()) {
					boolean addRepo = (groupsUtil == null
							|| !showRepositoryGroups
							|| !groupsUtil.belongsToGroup(gitDir));
					if (addRepo) {
						RepositoryNode rNode = new RepositoryNode(parent,
								repositoryCache.lookupRepository(gitDir));
						result.add(rNode);
					}
				} else {
					util.removeDir(gitDir);
				}
			} catch (IOException e) {
				// ignore for now
			}
		}
		return result;
	}

	private Object[] getBranchChildren(RepositoryTreeNode node, Repository repo,
			String prefix) {
		if (isHierarchical()) {
			return getBranchHierarchyChildren(node, repo, prefix);
		} else {
			try {
				return getRefs(repo, prefix).values().stream()
						.filter(ref -> !ref.isSymbolic())
						.map(ref -> new RefNode(node, repo, ref)).toArray();
			} catch (IOException e) {
				return handleException(e, node);
			}
		}
	}

	private Object[] getBranchHierarchyChildren(RepositoryTreeNode node,
			Repository repo, String prefix) {
		try {
			Set<String> folderChildren = new HashSet<>();
			return getRefs(repo, prefix).entrySet().stream()
					.filter(e -> !e.getValue().isSymbolic()).map(e -> {
						int i = e.getKey().indexOf('/', prefix.length());
						if (i < 0) {
							return new RefNode(node, repo, e.getValue());
						} else {
							String name = e.getKey().substring(prefix.length(),
									i);
							if (folderChildren.add(name)) {
								return new BranchHierarchyNode(node, repo,
										Path.fromPortableString(prefix + name));
							}
							return null;
						}
					}).filter(Objects::nonNull).toArray();
		} catch (IOException e) {
			return handleException(e, node);
		}
	}

	private Object[] getDirectoryChildren(RepositoryTreeNode parentNode,
			File parent) {
		Repository repo = parentNode.getRepository();
		List<RepositoryTreeNode<File>> children = new ArrayList<>();

		try {
			Files.walkFileTree(parent.toPath(),
					EnumSet.noneOf(FileVisitOption.class), 1,
					new SimpleFileVisitor<java.nio.file.Path>() {
						@Override
						public FileVisitResult visitFile(
								java.nio.file.Path file,
								BasicFileAttributes attrs) throws IOException {
							if (attrs.isDirectory()) {
								children.add(new FolderNode(parentNode, repo,
										file.toFile()));
							} else {
								children.add(new FileNode(parentNode, repo,
										file.toFile()));
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(
								java.nio.file.Path file, IOException exc)
								throws IOException {
							// Just ignore it
							return FileVisitResult.CONTINUE;
						}
					});
		} catch (IOException e) {
			// Ignore
		}

		return children.toArray();
	}

	private Object[] getTagsChildren(RepositoryTreeNode parentNode,
			Repository repo) {
		List<RepositoryTreeNode<Ref>> nodes = new ArrayList<>();

		try (RevWalk walk = new RevWalk(repo)) {
			walk.setRetainBody(true);
			for (Ref tagRef : getRefs(repo, Constants.R_TAGS).values()) {
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
		case REPOGROUP:
			return ((RepositoryGroupNode) element).hasChildren();
		case BRANCHES:
		case REPO:
		case ADDITIONALREFS:
		case SUBMODULES:
			return true;
		case TAGS:
			return hasTagsChildren(repo);
		case WORKINGDIR:
			return !repo.isBare() && hasDirectoryChildren(repo.getWorkTree());
		case FOLDER:
			return !repo.isBare()
					&& hasDirectoryChildren((File) node.getObject());
		case FILE:
			return false;
		default:
			Object[] children = getChildren(element);
			return children != null && children.length > 0;
		}
	}

	private boolean hasDirectoryChildren(File file) {
		try (DirectoryStream<java.nio.file.Path> dir = Files
				.newDirectoryStream(file.toPath())) {
			return dir.iterator().hasNext();
		} catch (DirectoryIteratorException | IOException e) {
			return false;
		}
	}

	/**
	 * As long as the ref database has not been read, assume there are tags, and
	 * start reading the database in the background. This should avoid long
	 * blocking during startup.
	 *
	 * @param repo
	 * @return whether the tags node has children.
	 */
	private boolean hasTagsChildren(Repository repo) {
		try {
			if (!refCache.isLoaded(repo)) {
				WorkspaceJob job = new WorkspaceJob(
						UIText.RepositoriesViewContentProvider_ReadReferencesJob) {

					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor)
							throws CoreException {
						try {
							// trigger reading the reference database
							getRefs(repo, Constants.R_TAGS);
						} catch (IOException e) {
							return Status.CANCEL_STATUS;
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
				return true;
			}
			return !getRefs(repo, Constants.R_TAGS).isEmpty();
		} catch (IOException e) {
			return true;
		}
	}

	private Map<String, Ref> getRefs(final Repository repo, final String prefix)
			throws IOException {
		return refCache.byPrefix(repo, prefix);
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
		return ((Boolean) branchHierarchy.getValue()).booleanValue();
	}
}
