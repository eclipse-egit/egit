/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.egit.core.Utils.copyOf;
import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;

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

	private final IProject[] projects;

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
		Set<IProject> projectSet = data.getProjects();
		projects = projectSet.toArray(new IProject[projectSet.size()]);

		srcRev = data.getSrcRevCommit();
		dstRev = data.getDstRevCommit();
	}

	@Override
	public GitModelObject[] getChildren() {
		if (childrens == null)
			getChildrenImpl();

		return copyOf(childrens);
	}

	@Override
	public String getName() {
		return repo.getWorkTree().toString();
	}

	@Override
	public IProject[] getProjects() {
		return copyOf(projects);
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

	private void getChildrenImpl() {
		RevWalk rw = new RevWalk(repo);
		RevFlag localFlag = rw.newFlag("local"); //$NON-NLS-1$
		RevFlag remoteFlag = rw.newFlag("remote"); //$NON-NLS-1$
		RevFlagSet allFlags = new RevFlagSet();
		allFlags.add(localFlag);
		allFlags.add(remoteFlag);
		rw.carry(allFlags);
		List<GitModelObjectContainer> result = new ArrayList<GitModelObjectContainer>();

		rw.setRetainBody(true);
		try {
			RevCommit srcCommit = rw.parseCommit(srcRev);
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

			if (includeLocal) {
				result.add(new GitModelCache(this, dstCommit));
				result.add(new GitModelWorkingTree(this, dstCommit));
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}


		childrens = result.toArray(new GitModelObjectContainer[result.size()]);
	}

}
