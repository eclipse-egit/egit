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
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.JGitInternalException;
import org.eclipse.jgit.api.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
import org.eclipse.jgit.revwalk.RevObject;

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
	 * @param data
	 *            synchronization data
	 * @throws IOException
	 * @throws MissingObjectException
	 */
	public GitModelRepository(GitSynchronizeData data)
			throws MissingObjectException, IOException {
		super(null);
		repo = data.getRepository();
		Set<IProject> projectSet = data.getProjects();
		projects = projectSet.toArray(new IProject[projectSet.size()]);

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

	@Override
	public boolean isContainer() {
		return true;
	}


	private void getChildrenImpl() {
		List<GitModelCommit> result = new ArrayList<GitModelCommit>();

		try {
			RevCommit ancestorCommit = RevUtils.getCommonAncestor(repo, srcRev,
					dstRev);
			RevCommitList<RevCommit> commits = getRevCommits(ancestorCommit, dstRev);
			commits.addAll(getRevCommits(ancestorCommit, srcRev));

			for (RevCommit commit : commits)
				result.add(new GitModelCommit(this, commit));
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		childrens = result.toArray(new GitModelCommit[result.size()]);
	}

	private RevCommitList<RevCommit> getRevCommits(AnyObjectId since, AnyObjectId until) {
		Git git = new Git(repo);
		RevCommitList<RevCommit> result = new RevCommitList<RevCommit>();
		try {
			Iterable<RevCommit> call = git.log().addRange(since, until).call();

			for (RevCommit commit : call)
				result.add(commit);

		} catch (NoHeadException e) {
			Activator.logError(e.getMessage(), e);
		} catch (JGitInternalException e) {
			Activator.logError(e.getMessage(), e);
		} catch (MissingObjectException e) {
			Activator.logError(e.getMessage(), e);
		} catch (IncorrectObjectTypeException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result;
	}

}
