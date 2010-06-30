/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.resource.GitContainer;
import org.eclipse.egit.core.resource.GitFile;
import org.eclipse.egit.core.resource.GitFolder;
import org.eclipse.egit.core.resource.GitProject;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantTree;

abstract class GitResourceVariantTree extends ResourceVariantTree {

	private final GitSynchronizeDataSet gsds;

	private final ResourceVariantByteStore store;

	GitResourceVariantTree(ResourceVariantByteStore store,
			GitSynchronizeDataSet data) {
		super(store);
		this.gsds = data;
		this.store = getByteStore();

		try {
			initialize();
		} catch (Exception e) {
			// we can live without initialization, it only makes synchronization
			// process a little bit faster
		}
	}

	public IResource[] roots() {
		Set<IResource> roots = new HashSet<IResource>();
		for (GitSynchronizeData gsd : gsds) {
			roots.addAll(gsd.getProjects());
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	public IResourceVariant getResourceVariant(IResource resource)
			throws TeamException {
		GitSynchronizeData gsd = gsds.getData(resource.getProject().getName());

		if (resource.getType() == IResource.FILE)
			return getBlobResourceVariant(resource, gsd);
		else
			return getFolderResourceVariant(resource, gsd);
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (variant == null || !variant.isContainer())
			return new IResourceVariant[0];

		IProgressMonitor monitor = SubMonitor.convert(progress);
		monitor.beginTask(NLS.bind(
				CoreText.GitResourceVariantTree_fetchingMembers, variant),
				IProgressMonitor.UNKNOWN);

		try {
			return fetchMembersImpl(variant, monitor);
		} finally {
			monitor.done();
		}
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(NLS.bind(
				CoreText.GitResourceVariantTree_fetchingVariant,
				resource.getName()), IProgressMonitor.UNKNOWN);

		try {
			return getResourceVariant(resource);
		} finally {
			subMonitor.done();
		}
	}

	protected abstract RevCommit getBaseRevCommit(GitSynchronizeData gsd)
			throws IOException;

	protected GitSynchronizeDataSet getSyncData() {
		return gsds;
	}

	private void initialize() throws IOException, TeamException {
		for (GitSynchronizeData gsd : gsds) {
			Repository repo = gsd.getRepository();
			RevCommit revCommit = getBaseRevCommit(gsd);
			Tree tree = repo.mapTree(revCommit.getTree().getId());

			readMembers(gsd, tree);
		}
	}

	private void readMembers(GitSynchronizeData gsd, Tree tree)
			throws IOException, TeamException {
		Tree projTree;
		GitProject parent;
		TreeEntry treeEntry;

		for (IProject project : gsd.getProjects()) {
			treeEntry = tree.findTreeMember(project.getName());

			if (treeEntry != null && treeEntry instanceof Tree) {
				projTree = (Tree) treeEntry;
				parent = new GitProject(project, projTree);
			} else {
				projTree = tree;
				parent = new GitProject(project, projTree);
			}

			readAllMembers(parent, projTree.members());
		}
	}

	private void readAllMembers(IContainer parent, TreeEntry[] tree)
			throws TeamException, IOException {
		for (int i = 0; i < tree.length; i++) {
			if (tree[i] instanceof Tree) {
				Tree nestedTree = (Tree) tree[i];
				GitFolder folder = new GitFolder(parent, nestedTree);
				TreeEntry[] nestedTreeEntrys = nestedTree.members();

				readAllMembers(folder, nestedTreeEntrys);
			} else if (tree[i] instanceof FileTreeEntry) {
				FileTreeEntry fileEntry = (FileTreeEntry) tree[i];
				GitFile file = new GitFile(parent, fileEntry);

				store.setBytes(file, fileEntry.openReader().getBytes());
			} // what we should do with GitLinkTreEntry and SymlinkTreeEntry?
		}
	}

	private IResourceVariant[] fetchMembersImpl(IResourceVariant variant,
			IProgressMonitor monitor) throws TeamException {
		IContainer container = getContainer(variant);
		Set<IResourceVariant> result = new HashSet<IResourceVariant>();
		GitSynchronizeData gsd = gsds.getData(container.getProject());
		Tree treeEntry = getTree(container, gsd);

		if (treeEntry == null)
			return new IResourceVariant[0];

		try {
			TreeEntry[] members = treeEntry.members();

			for (TreeEntry entry : members) {
				if (entry instanceof FileTreeEntry) {
					FileTreeEntry fileTreeEntry = (FileTreeEntry) entry;
					GitFile file = new GitFile(container, fileTreeEntry);
					result.add(new GitBlobResourceVariant(file, fileTreeEntry));
				} else if (entry instanceof Tree) {
					Tree tree = (Tree) entry;
					GitFolder gitFolder = new GitFolder(container, tree);
					result.add(new GitFolderResourceVariant(gitFolder));
				}
				monitor.worked(1);
			}
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.GitResourceVariantTree_couldNotFetchMembersOf,
					variant), e);
		}

		return result.toArray(new IResourceVariant[result.size()]);
	}

	private IContainer getContainer(IResourceVariant variant) {
		GitFolderResourceVariant folder = (GitFolderResourceVariant) variant;
		IContainer container = folder.getContainer();
		return container;
	}

	private IResourceVariant getBlobResourceVariant(IResource resource,
			GitSynchronizeData gsd) throws TeamException {
		FileTreeEntry fileEntry = getBlob(resource, gsd);
		if (fileEntry == null)
			return null;

		byte[] content;
		GitFile file = new GitFile(resource.getParent(), fileEntry);
		try {
			content = fileEntry.openReader().getBytes();
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.GitResourceVariantTree_couldNotOpenReaderFor,
					resource), e);
		}

		return new GitBlobResourceVariant(file, content);
	}

	private IResourceVariant getFolderResourceVariant(IResource resource,
			GitSynchronizeData gsd) throws TeamException {
		Tree folderEntry = getTree((IContainer) resource, gsd);
		GitContainer folder;

		if (resource.getType() == IResource.PROJECT)
			folder = new GitProject((IProject) resource, folderEntry);
		else
			folder = new GitFolder(resource.getParent(), folderEntry);

		return new GitFolderResourceVariant(folder);
	}

	private String getResourcePath(IResource resource, Repository repo) {
		String repoWorkDir = repo.getWorkDir().toString();
		String resourcePath = resource.getLocation().toString();

		if (!repoWorkDir.equals(resourcePath)
				&& resourcePath.startsWith(repoWorkDir))
			resourcePath = resourcePath.substring(repoWorkDir.length() + 1);

		return resourcePath;
	}

	private Tree getTree(IContainer container, GitSynchronizeData gsd)
			throws TeamException {
		Tree treeEntry;
		Repository repo = gsd.getRepository();

		try {
			RevCommit baseRevCommit = getBaseRevCommit(gsd);
			Tree tree = repo.mapTree(baseRevCommit.getTree().getId());
			String repoWorkDir = repo.getWorkDir().toString();
			String resourcePath = getResourcePath(container, repo);

			if (resourcePath.equals(repoWorkDir))
				treeEntry = repo.mapTree(baseRevCommit.getTree().getId());
			else
				treeEntry = (Tree) tree.findTreeMember(resourcePath);
		} catch (IOException e) {
			throw new TeamException(
					NLS.bind(CoreText.GitResourceVariantTree_couldNotFindTree,
							container), e);
		}

		return treeEntry;
	}

	private FileTreeEntry getBlob(IResource resource, GitSynchronizeData gsd)
			throws TeamException {
		FileTreeEntry fileEntry;
		Repository repo = gsd.getRepository();

		try {
			Tree tree = repo.mapTree(getBaseRevCommit(gsd).getTree().getId());
			String resourcePath = getResourcePath(resource, repo);

			TreeEntry treeEntry = tree.findBlobMember(resourcePath);
			fileEntry = (FileTreeEntry) treeEntry;
		} catch (IOException e) {
			throw new TeamException(
					NLS.bind(CoreText.GitResourceVariantTree_couldNotFindBlob,
							resource), e);
		}

		return fileEntry;
	}

}
