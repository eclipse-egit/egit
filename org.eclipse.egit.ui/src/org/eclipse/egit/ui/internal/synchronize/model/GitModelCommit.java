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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObjectContainer implements
		ISynchronizationCompareInput {

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
		int result = getLocation().hashCode() ^ remoteCommit.hashCode();
		if (baseCommit != null)
			result ^= baseCommit.hashCode();

		return result;
	}

	@Override
	protected GitModelObject[] getChildrenImpl() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			TreeWalk tw = createTreeWalk();
			RevTree actualTree = remoteCommit.getTree();

			int actualNth = tw.addTree(actualTree);
			int baseNth = -1;
			if (baseCommit != null)
				baseNth = tw.addTree(baseCommit.getTree());
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

}
