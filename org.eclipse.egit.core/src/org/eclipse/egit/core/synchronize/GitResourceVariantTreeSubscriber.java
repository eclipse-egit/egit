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

import static org.eclipse.jgit.lib.Repository.stripWorkDir;
import static org.eclipse.team.core.Team.isIgnoredHint;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;

/**
 *
 */
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {

	/**
	 * A resource variant tree of the remote branch(es).
	 */
	private IResourceVariantTree remoteTree;

	/**
	 * A resource variant tree against HEAD.
	 */
	private IResourceVariantTree baseTree;

	private GitSynchronizeDataSet gsds;

	/**
	 * @param data
	 */
	public GitResourceVariantTreeSubscriber(GitSynchronizeDataSet data) {
		this.gsds = data;
	}

	private IResource[] roots;

	@Override
	public boolean isSupervised(IResource res) throws TeamException {
		return IResource.FILE == res.getType()
				&& gsds.contains(res.getProject()) && !isIgnoredHint(res)
				&& shouldBeIncluded(res);
	}

	/**
	 * Returns all members of git repository (including those that are not
	 * imported into workspace)
	 *
	 * @param res
	 */
	@Override
	public IResource[] members(IResource res) throws TeamException {
		if(res.getType() == IResource.FILE || !shouldBeIncluded(res))
			return new IResource[0];

		GitSynchronizeData gsd = gsds.getData(res.getProject());
		Repository repo = gsd.getRepository();

		String path = stripWorkDir(repo.getWorkTree(), res.getLocation()
				.toFile());

		TreeWalk tw = new TreeWalk(repo);
		if (path.length() > 0)
			tw.setFilter(PathFilter.create(path));

		Set<IResource> gitMembers = new HashSet<IResource>();
		Map<String, IResource> allMembers = new HashMap<String, IResource>();
		try {
			tw.addTree(new FileTreeIterator(repo));
			enterFilteredPath(path, tw);
			for (IResource member : ((IContainer) res).members())
				allMembers.put(member.getName(), member);

			while (tw.next()) {
				if (tw.getTree(0, FileTreeIterator.class).isEntryIgnored())
					continue;

				IResource member = allMembers.get(tw.getNameString());
				if (member != null)
					gitMembers.add(member);
			}
		} catch (IOException e) {
			throw new TeamException(e.getMessage(), e);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}

		return gitMembers.toArray(new IResource[gitMembers.size()]);
	}

	@Override
	public IResource[] roots() {
		if (roots == null)
			roots = gsds.getAllProjects();
		IResource[] result = new IResource[roots.length];
		System.arraycopy(roots, 0, result, 0, roots.length);
		return result;
	}

	/**
	 * @param data
	 */
	public void reset(GitSynchronizeDataSet data) {
		gsds = data;

		roots = null;
		baseTree = null;
		remoteTree = null;
	}

	@Override
	public String getName() {
		return CoreText.GitBranchResourceVariantTreeSubscriber_gitRepository;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new GitResourceVariantComparator(gsds);
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		if (baseTree == null) {
			baseTree = new GitBaseResourceVariantTree(gsds);
		}
		return baseTree;
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		if (remoteTree == null) {
			remoteTree = new GitRemoteResourceVariantTree(gsds);
		}
		return remoteTree;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		GitSynchronizeData gsd = gsds.getData(local.getProject());

		SyncInfo info;
		if (gsd.shouldIncludeLocal())
			info = new SyncInfo(local, base, remote, getResourceComparator());
		else
			info = new GitSyncInfo(local, base, remote, getResourceComparator(), gsd);

		info.init();
		return info;
	}

	private void enterFilteredPath(String path, TreeWalk tw) throws IOException {
		int subtreesLen = path.split("/").length; //$NON-NLS-1$
		for (int i = 0; i < subtreesLen; i++) {
			tw.next();
			tw.enterSubtree();
		}
	}

	private boolean shouldBeIncluded(IResource res) {
		Set<IContainer> includedPaths = gsds.getData(res.getProject())
				.getIncludedPaths();
		if (includedPaths == null)
			return true;

		IPath path = res.getLocation();
		for (IContainer container : includedPaths)
			if (container.getLocation().isPrefixOf(path))
				return true;

		return false;
	}

}
