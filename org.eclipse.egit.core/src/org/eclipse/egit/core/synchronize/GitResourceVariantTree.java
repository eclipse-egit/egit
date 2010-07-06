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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.AbstractIndexTreeVisitor;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexTreeWalker;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.AbstractResourceVariantTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

abstract class GitResourceVariantTree extends AbstractResourceVariantTree {

	/**
	 * A map of a given resource's trail of commits.
	 */
	private Map<String, RevCommitList<RevCommit>> dates = new HashMap<String, RevCommitList<RevCommit>>();

	/**
	 * A map of a given resource to its latest blob within the branch.
	 */
	private Map<String, ObjectId> updated = new HashMap<String, ObjectId>();

	/**
	 * A map of repositories to their trees.
	 */
	private Map<Repository, Tree> trees = new HashMap<Repository, Tree>();

	private GitSynchronizeDataSet gsdData;

	private final ResourceVariantByteStore store;

	GitResourceVariantTree(GitSynchronizeDataSet data,
			ResourceVariantByteStore store) {
		this.store = store;
		this.gsdData = data;
	}

	public IResource[] roots() {
		Set<IResource> roots = new HashSet<IResource>();
		for (GitSynchronizeData gsd : gsdData) {
			roots.addAll(gsd.getProjects());
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	public IResource[] members(IResource resource) throws TeamException {
		if (resource.exists() && resource instanceof IContainer) {
			GitSynchronizeData gsd = getSyncData().getData(
					resource.getProject());
			if (gsd.shouldIncludeLocal()) {
				try {
					return ((IContainer) resource).members();
				} catch (CoreException e) {
					throw new TeamException(e.getStatus());
				}
			} else {
				return getMembersAndStore(resource, gsd);
			}
		}
		return new IResource[0];
	}

	/**
	 * Returns whether this file is of interest to this resource variant tree.
	 * Due to the fact that a repository may have many, many files, we only want
	 * to retrieve and store information about files that the user is actually
	 * interested in. That is, if they only wish to synchronize on one project,
	 * then there is no reason for this tree to be storing information about
	 * other projects that are contained within the repository.
	 *
	 * @param file
	 *            the file to check
	 * @return <code>true</code> if the blob information about this file is of
	 *         interest to this tree, <code>false</code> otherwise
	 */
	private boolean contains(File file) {
		for (GitSynchronizeData gsd : gsdData) {
			if (gsd.contains(file)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Retrieves the name of the branch that this variant tree should be
	 * compared against for the given resource.
	 *
	 * @param resource
	 *            the resource that is being compared for
	 * @return the name of the target comparison branch
	 * @throws IOException
	 */
	abstract Tree getRevTree(IResource resource) throws IOException;

	abstract ObjectId getRevObjId(IResource resource) throws IOException;

	/**
	 * Initializes the repository information for the specified resource.
	 *
	 * @param resource
	 *            the resource that needs to have its repository information
	 *            initialized for
	 * @throws IOException
	 *             if an error occurs while walking the branch
	 */
	private synchronized void initialize(IResource resource) throws IOException {
		IProject project = resource.getProject();
		if (!gsdData.contains(project)) {
			return;
		}

		Repository db = gsdData.getData(project).getRepository();
		if (!trees.containsKey(db)) {
			Tree tree = getRevTree(resource);
			ObjectId objId = getRevObjId(resource);

			if (objId != null && tree != null) {
				trees.put(db, tree);
				// walk the tree to retrieve information
				walk(db, objId, tree);
			}
		}
	}

	private void walk(final Repository db, final ObjectId objId, Tree merge)
			throws IOException {
		IndexTreeWalker walker = new IndexTreeWalker(db.getIndex(), merge, db
				.getWorkTree(), new AbstractIndexTreeVisitor() {
			public void visitEntry(TreeEntry treeEntry, Entry indexEntry,
					File file) throws IOException {
				if (treeEntry != null && contains(file)) {
					store(db, objId, treeEntry);
				}
			}
		});
		walker.walk();
	}

	private void store(Repository db, ObjectId objId, TreeEntry treeEntry)
			throws IOException {
		String entry = treeEntry.getFullName();
		RevWalk walk = new RevWalk(db);
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.sort(RevSort.BOUNDARY, true);
		walk.markStart(walk.parseCommit(objId));
		walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
				.createFromStrings(Collections.singleton(entry)),
				TreeFilter.ANY_DIFF));

		RevCommitList<RevCommit> list = new RevCommitList<RevCommit>();
		list.source(walk);

		int lastSize = 0;
		do {
			lastSize = list.size();
			list.fillTo(Integer.MAX_VALUE);
		} while (lastSize != list.size());

		dates.put(entry, list);
		updated.put(entry, treeEntry.getId());
	}

	public IResourceVariant getResourceVariant(IResource resource)
			throws TeamException {
		return fetchVariant(resource, 0, new NullProgressMonitor());
	}

	private IResourceVariant findFolderVariant(IResource resource,
			Repository repository) {
		File workDir = repository.getWorkTree();
		if (resource.getLocation() == null)
			return null;
		File resourceLocation = resource.getLocation().toFile();
		String resLocationAbsolutePath = resourceLocation.getAbsolutePath();

		for (Map.Entry<String, ObjectId> entry : updated.entrySet()) {
			String entryName = entry.getKey();
			File file = new File(workDir, entryName);

			if (file.getAbsolutePath().startsWith(resLocationAbsolutePath))
				return new GitFolderResourceVariant(resource);
		}

		return null;
	}

	private IResourceVariant findFileVariant(IResource resource,
			Repository repository) throws TeamException {
		RepositoryMapping repoMapping = RepositoryMapping.getMapping(resource);
		if (repoMapping == null)
			return null;

		String gitPath = repoMapping.getRepoRelativePath(resource);
		ObjectId objectId = updated.get(gitPath);
		if (objectId != null) {
			File root = repository.getWorkTree();
			File file = new File(root, gitPath);

			if (resource.getLocation().toFile().equals(file)) {
				try {
					Tree merge = trees.get(repository);
					TreeEntry tree = merge.findBlobMember(gitPath);
					GitBlobResourceVariant variant = new GitBlobResourceVariant(
							resource, repository, tree.getId(), dates
									.get(gitPath));
					return variant;
				} catch (IOException e) {
					throw new TeamException(new Status(IStatus.ERROR, Activator
							.getPluginId(), NLS.bind(
							CoreText.GitResourceVariantTree_couldNotFindBlob,
							gitPath), e));
				}
			}
		}
		return null;
	}

	public boolean hasResourceVariant(IResource resource) throws TeamException {
		return getResourceVariant(resource) != null;
	}

	public void flushVariants(IResource resource, int depth)
			throws TeamException {
		if (!gsdData.getData(resource.getProject()).shouldIncludeLocal())
			store.flushBytes(resource, depth);
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (!variant.isContainer()) {
			return new IResourceVariant[0];
		}

		IProgressMonitor monitor = SubMonitor.convert(progress);

		Set<IResourceVariant> members = new HashSet<IResourceVariant>();
		try {
			GitFolderResourceVariant folderVariant = (GitFolderResourceVariant) variant;
			IContainer container = folderVariant.getContainer();
			File resourceLocation = container.getLocation().toFile();
			IProject project = container.getProject();

			Repository repository = gsdData.getData(project).getRepository();

			monitor.beginTask(NLS.bind(
					CoreText.GitResourceVariantTree_fetchingMembers, container
							.getLocation()), updated.size());
			File root = repository.getWorkTree();

			for (Map.Entry<String, ObjectId> entry : updated.entrySet()) {
				String entryName = entry.getKey();
				File file = new File(root, entryName);

				if (file.getAbsolutePath().startsWith(
						resourceLocation.getAbsolutePath())) {
					members.add(getMember(container, repository, entryName));
				}

				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return members.toArray(new IResourceVariant[members.size()]);
	}

	private IResourceVariant getMember(IContainer container,
			Repository repository, String entryName) throws TeamException {
		String gitPath = RepositoryMapping.getMapping(container)
				.getRepoRelativePath(container);
		Tree merge = trees.get(repository);
		try {
			TreeEntry tree = merge.findBlobMember(entryName);
			GitBlobResourceVariant blobVariant = new GitBlobResourceVariant(
					container.getFile(new Path(entryName)), repository, tree
							.getId(), dates.get(entryName));
			return blobVariant;
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, Activator
					.getPluginId(), NLS.bind(
					CoreText.GitResourceVariantTree_couldNotFindBlob, gitPath),
					e));
		}
	}

	private IResourceVariant fetchVariant(IResource resource,
			IProgressMonitor monitor) throws TeamException {
		try {
			monitor.beginTask(NLS.bind(
					CoreText.GitResourceVariantTree_fetchingVariant, resource
							.getLocation()), 5);
			initialize(resource);
			monitor.worked(4);
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, Activator
					.getPluginId(), NLS.bind(
					CoreText.GitResourceVariantTree_unableToReadRepository,
					resource.getName()), e));
		}

		Repository repository = gsdData.getData(resource.getProject())
				.getRepository();

		if (resource instanceof IProject) {
			return new GitFolderResourceVariant(resource);
		} else if (resource instanceof IFolder) {
			return findFolderVariant(resource, repository);
		}

		return findFileVariant(resource, repository);
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		try {
			if (resource != null)
				return fetchVariant(resource, monitor);
			else
				return null;
		} finally {
			monitor.done();
		}
	}

	@Override
	protected boolean setVariant(IResource local, IResourceVariant remote)
			throws TeamException {
		return true;
	}

	protected GitSynchronizeDataSet getSyncData() {
		return gsdData;
	}

	private IResource[] getMembersAndStore(IResource resource,
			GitSynchronizeData gsd) throws TeamException {
		Repository repo = gsd.getRepository();
		try {
			Tree tree = gsd.mapSrcTree();

			if (tree == null)
				return new IResource[0];

			IResource[] members = ((IContainer) resource).members();
			Set<IResource> membersSet = getAllMembers(repo, tree, members);

			return membersSet.toArray(new IResource[membersSet.size()]);
		} catch (IOException e) {
			throw new TeamException(e.getMessage(), e);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	private Set<IResource> getAllMembers(Repository repo, Tree tree,
			IResource[] members) throws IOException, TeamException {
		Set<IResource> membersSet = new HashSet<IResource>();

		for (IResource member : members) {
			String memberRelPath = getMemberRelPath(repo, member);

			// check if this file exists in repository
			if (tree.existsBlob(memberRelPath)) {
				// read file content and add it into store
				TreeEntry entry = tree.findBlobMember(memberRelPath);
				store.setBytes(member, repo.open(entry.getId(),
						Constants.OBJ_BLOB).getCachedBytes());
				membersSet.add(member);
			} else if (tree.existsTree(memberRelPath)) {
				// add to members if folder exists in repository
				membersSet.add(member);
			}
		}
		return membersSet;
	}

	private String getMemberRelPath(Repository repo, IResource member) {
		String repoWorkDir = repo.getWorkTree().toString();
		if (!"/".equals(File.separator)) { //$NON-NLS-1$
			// fix file separator issue on windows
			repoWorkDir = repoWorkDir.replace(File.separatorChar, '/');
		}

		String memberRelPath = member.getLocation().toString();
		memberRelPath = memberRelPath.replace(repoWorkDir, ""); //$NON-NLS-1$
		if (memberRelPath.startsWith("/"))//$NON-NLS-1$
			memberRelPath = memberRelPath.substring(1);

		return memberRelPath;
	}

}
