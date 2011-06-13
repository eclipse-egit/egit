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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObjectContainer implements
		ISynchronizationCompareInput {

	/**
	 * Common ancestor commit for wrapped commit object
	 */
	protected final RevCommit ancestorCommit;
	private final TreeFilter pathFilter;

	/**
	 * @param parent
	 *            instance of repository model object that is parent for this
	 *            commit
	 * @param commit
	 *            instance of commit that will be associated with this model
	 *            object
	 * @param direction
	 * @param pathFilter
	 * @throws IOException
	 */
	public GitModelCommit(GitModelRepository parent, RevCommit commit,
			int direction, TreeFilter pathFilter) throws IOException {
		super(parent, commit, direction);
		this.pathFilter = pathFilter;

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
	 * @param ancestorCommit
	 *            common ancestor commit for object that is wrapped
	 * @param direction
	 *            use {@link Differencer#LEFT} and {@link Differencer#RIGHT} to
	 *            determinate commit direction (is it incoming or outgoing)
	 * @throws IOException
	 */
	protected GitModelCommit(GitModelObject parent, RevCommit commit,
			RevCommit ancestorCommit, int direction) throws IOException {
		super(parent, commit, direction);

		pathFilter = null;
		this.ancestorCommit = ancestorCommit;
	}

	/**
	 * Constructor used by JUnits
	 *
	 * @param parent
	 * @param commit
	 * @param direction
	 * @throws IOException
	 */
	GitModelCommit(GitModelRepository parent, RevCommit commit,
			int direction) throws IOException {
		this(parent, commit, direction, null);
	}

	@Override
	public IPath getLocation() {
		return new Path(getRepository().getWorkTree().toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelCommit && !(obj instanceof GitModelTree)
				&& !(obj instanceof GitModelBlob)) {
			GitModelCommit objCommit = (GitModelCommit) obj;

			return objCommit.getBaseCommit().equals(baseCommit)
					&& objCommit.getParent().equals(getParent());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return baseCommit.hashCode() ^ getParent().hashCode();
	}

	@Override
	public String toString() {
		return "ModelCommit[" + baseCommit.getId() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	protected GitModelObject[] getChildrenImpl() {
		TreeWalk tw = createTreeWalk();
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			RevTree actualTree = baseCommit.getTree();

			int baseNth = tw.addTree(actualTree);
			int remoteNth = -1;
			if (remoteCommit != null)
				remoteNth = tw.addTree(remoteCommit.getTree());
			int ancestorNth = tw.addTree(ancestorCommit.getTree());

			while (tw.next()) {
				GitModelObject obj = getModelObject(tw, ancestorCommit, ancestorNth,
						remoteNth, baseNth);
				if (obj != null)
					result.add(obj);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new GitModelObject[result.size()]);
	}

	private RevCommit calculateAncestor(RevCommit actual) throws IOException {
		RevWalk rw = new RevWalk(getRepository());
		rw.setRevFilter(RevFilter.MERGE_BASE);

		if (pathFilter != null)
			rw.setTreeFilter(pathFilter);

		for (RevCommit parent : actual.getParents()) {
			RevCommit parentCommit = rw.parseCommit(parent.getId());
			rw.markStart(parentCommit);
		}

		rw.markStart(rw.parseCommit(actual.getId()));

		RevCommit result = rw.next();
		return result != null ? result : rw.parseCommit(ObjectId.zeroId());
	}

}
