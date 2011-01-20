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

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObjectContainer implements
		ISynchronizationCompareInput {

	private RevCommit ancestorCommit;

	/**
	 * @param parent
	 *            instance of repository model object that is parent for this
	 *            commit
	 * @param commit
	 *            instance of commit that will be associated with this model
	 *            object
	 * @param direction
	 * @throws IOException
	 */
	public GitModelCommit(GitModelRepository parent, RevCommit commit,
			int direction) throws IOException {
		super(parent, commit, direction);

		this.ancestorCommit = calculateAncestor(commit);
	}

	/**
	 * Constructor for child classes.
	 *
	 * @param parent
	 *            instance of repository model object that is parent for this
	 *            commit
	 * @param commit
	 *            instance of commit that will be associated with this model
	 *            object
	 * @param direction
	 *            use {@link Differencer#LEFT} and {@link Differencer#RIGHT} to
	 *            determinate commit direction (is it incoming or outgoing)
	 * @throws IOException
	 */
	protected GitModelCommit(GitModelObject parent, RevCommit commit,
			int direction) throws IOException {
		super(parent, commit, direction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelCommit) {
			GitModelCommit objCommit = (GitModelCommit) obj;

			boolean equalsBaseCommit;
			RevCommit objBaseCommit = objCommit.getBaseCommit();
			if (objBaseCommit != null)
				equalsBaseCommit = objBaseCommit.equals(baseCommit);
			else
				equalsBaseCommit = baseCommit == null;

			// it is impossible to have different common ancestor commit if
			// remote and base commit are equal, therefore we don't compare
			// common ancestor's

			return equalsBaseCommit
					&& objCommit.getRemoteCommit().equals(remoteCommit)
					&& objCommit.getLocation().equals(getLocation());
		}

		return false;
	}

	@Override
	public int hashCode() {
		int result = getLocation().hashCode() ^ baseCommit.hashCode();
		if (remoteCommit != null)
			result ^= remoteCommit.hashCode();

		return result;
	}

	@Override
	protected GitModelObject[] getChildrenImpl() {
		TreeWalk tw = createTreeWalk();
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			RevTree actualTree = baseCommit.getTree();

			int actualNth = tw.addTree(actualTree);
			int baseNth = -1;
			if (remoteCommit != null)
				baseNth = tw.addTree(remoteCommit.getTree());
			int ancestorNth = tw.addTree(ancestorCommit.getTree());

			while (tw.next()) {
				GitModelObject obj = getModelObject(tw, ancestorNth, baseNth,
						actualNth);
				if (obj != null)
					result.add(obj);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new GitModelObject[result.size()]);
	}

	/**
	 * @return ancestor commit for this model node
	 */
	protected RevCommit getAncestorCommit() {
		return ancestorCommit;
	}


	private RevCommit calculateAncestor(RevCommit actual) throws IOException {
		RevWalk rw = new RevWalk(getRepository());
		rw.setRevFilter(RevFilter.MERGE_BASE);

		for (RevCommit parent : actual.getParents()) {
			RevCommit parentCommit = rw.parseCommit(parent.getId());
			rw.markStart(parentCommit);
		}

		rw.markStart(rw.parseCommit(actual.getId()));

		RevCommit result = rw.next();
		return result != null ? result : rw.parseCommit(ObjectId.zeroId());
	}

}
