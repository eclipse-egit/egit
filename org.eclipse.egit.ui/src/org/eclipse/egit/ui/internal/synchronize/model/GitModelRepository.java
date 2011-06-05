/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevFlagSet;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Representation of Git repository in Git ChangeSet model.
 */
public class GitModelRepository extends GitModelObject {

	private final Repository repo;

	private final RevCommit srcRev;

	private final RevCommit dstRev;

	private final Set<IProject> projects;

	private final boolean includeLocal;

	private GitModelObject[] childrens;

	private IPath location;

	/**
	 * @param data
	 *            synchronization data
	 * @throws IOException
	 * @throws MissingObjectException
	 */
	public GitModelRepository(GitSynchronizeData data)
			throws MissingObjectException, IOException {
		super(null);
		repo = data.getRepository();
		includeLocal = data.shouldIncludeLocal();
		projects = data.getProjects();

		srcRev = data.getSrcRevCommit();
		dstRev = data.getDstRevCommit();
	}

	@Override
	public GitModelObject[] getChildren() {
		if (childrens == null)
			getChildrenImpl();

		return childrens;
	}

	@Override
	public String getName() {
		return repo.getWorkTree().toString();
	}

	@Override
	public IProject[] getProjects() {
		return projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * @return repository
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return source {@link RevObject}
	 */
	public ObjectId getSrcRev() {
		return srcRev;
	}

	/**
	 * @return destination {@link RevObject}
	 */
	public ObjectId getDstRev() {
		return dstRev;
	}

	@Override
	public IPath getLocation() {
		if (location == null)
			location = new Path(repo.getWorkTree().toString());

		return location;
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelRepository) {
			File objWorkTree = ((GitModelRepository) obj).repo.getWorkTree();
			return objWorkTree.equals(repo.getWorkTree());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return repo.getWorkTree().hashCode();
	}

	@Override
	public String toString() {
		return "ModelRepository[" + repo.getWorkTree() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void getChildrenImpl() {
		List<GitModelObjectContainer> result = new ArrayList<GitModelObjectContainer>();
		if (srcRev != null && dstRev != null)
			result.addAll(getListOfCommit());
		else {
			GitModelWorkingTree changes = getLocaWorkingTreeChanges();
			if (changes != null)
				result.add(changes);
		}


		childrens = result.toArray(new GitModelObjectContainer[result.size()]);
	}

	private List<GitModelObjectContainer> getListOfCommit() {
		List<GitModelObjectContainer> result = new ArrayList<GitModelObjectContainer>();

		RevWalk rw = new RevWalk(repo);
		rw.setRetainBody(true);
		try {
			RevCommit srcCommit = rw.parseCommit(srcRev);

			if (includeLocal) {
				GitModelCache gitCache = new GitModelCache(this, srcCommit);
				int gitCacheLen = gitCache.getChildren().length;

				GitModelWorkingTree gitWorkingTree = getLocaWorkingTreeChanges();
				int gitWorkingTreeLen = gitWorkingTree != null ? gitWorkingTree
						.getChildren().length : 0;

				if (gitCacheLen > 0 || gitWorkingTreeLen > 0) {
					result.add(gitCache);
					result.add(gitWorkingTree);
				}
			}

			if (srcRev.equals(dstRev))
				return result;

			RevFlag localFlag = rw.newFlag("local"); //$NON-NLS-1$
			RevFlag remoteFlag = rw.newFlag("remote"); //$NON-NLS-1$
			RevFlagSet allFlags = new RevFlagSet();
			allFlags.add(localFlag);
			allFlags.add(remoteFlag);
			rw.carry(allFlags);

			srcCommit.add(localFlag);
			rw.markStart(srcCommit);

			RevCommit dstCommit = rw.parseCommit(dstRev);
			dstCommit.add(remoteFlag);
			rw.markStart(dstCommit);

			for (RevCommit nextCommit : rw) {
				if (nextCommit.hasAll(allFlags))
					break;

				if (nextCommit.has(localFlag))
					result.add(new GitModelCommit(this, nextCommit, RIGHT));
				else if (nextCommit.has(remoteFlag))
					result.add(new GitModelCommit(this, nextCommit, LEFT));
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result;
	}

	private GitModelWorkingTree getLocaWorkingTreeChanges() {
		try {
			return new GitModelWorkingTree(this);
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return null;
	}

}
