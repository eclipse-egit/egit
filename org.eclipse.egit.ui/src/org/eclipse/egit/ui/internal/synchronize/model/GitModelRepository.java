/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Representation of Git repository in Git ChangeSet model.
 */
public class GitModelRepository extends GitModelObject {

	private final Repository repo;

	private final ObjectId srcRev;

	private final ObjectId dstRev;

	private final IProject[] projects;

	private GitModelObject[] childrens;

	private IPath location;

	/**
	 * @param data synchronization data
	 * @throws IOException
	 * @throws MissingObjectException
	 */
	public GitModelRepository(GitSynchronizeData data)
			throws MissingObjectException, IOException {
		super(null);
		repo = data.getRepository();
		Set<IProject> projectSet = data.getProjects();
		projects = projectSet.toArray(new IProject[projectSet.size()]);
		srcRev = data.getSrcRev().getObjectId();
		dstRev = data.getDstRev().getObjectId();
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
		return projects;
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

	private void getChildrenImpl() {
		ObjectWalk ow = new ObjectWalk(repo);
		List<GitModelCommit> result = new ArrayList<GitModelCommit>();

		try {
			ow.markStart(ow.parseAny(srcRev));
			RevCommit srcCommit = ow.next();

			ow.reset();
			ow.markStart(ow.parseAny(dstRev));
			RevCommit dstCommit = ow.next();

			if (srcCommit != null && dstCommit != null) {
				ow.reset();
				ow.setRevFilter(getRevFilter(srcCommit, dstCommit));
				ow.markStart(srcCommit);
				ow.markStart(dstCommit);

				RevCommit commit;
				while ((commit = ow.next()) != null)
					result.add(new GitModelCommit(this, commit));
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		childrens = result.toArray(new GitModelCommit[result.size()]);
	}

	private RevFilter getRevFilter(RevCommit srcCommit, RevCommit dstCommit) {
		Date since, until;

		if (srcCommit.getCommitTime() < dstCommit.getCommitTime()) {
			since = new Date(srcCommit.getCommitTime() * 1000L);
			until = new Date(dstCommit.getCommitTime() * 1000L);
		} else {
			until = new Date(srcCommit.getCommitTime() * 1000L);
			since = new Date(dstCommit.getCommitTime() * 1000L);
		}

		return CommitTimeRevFilter.between(since, until);
	}

}
