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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObject {

	private final RevCommit baseCommit;

	private final RevCommit remoteCommit;

	private final RevCommit ancestorCommit;

	private String name;

	private GitModelObject[] children;

	/**
	 * @param parent
	 *            instance of repository model object that is parent for this
	 *            commit
	 * @param commit
	 *            instance of commit that will be associated with this model
	 *            object
	 * @throws IOException
	 */
	public GitModelCommit(GitModelRepository parent, RevCommit commit)
			throws IOException {
		super(parent);
		remoteCommit = commit;
		ancestorCommit = calculateAncestor(remoteCommit);

		RevCommit[] parents = remoteCommit.getParents();
		if (parents != null && parents.length > 0)
			baseCommit = remoteCommit.getParent(0);
		else {
			baseCommit = null;
		}
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
	 * @throws IOException
	 */
	protected GitModelCommit(GitModelObject parent, RevCommit commit)
			throws IOException {
		super(parent);
		remoteCommit = commit;
		ancestorCommit = calculateAncestor(remoteCommit);

		RevCommit[] parents = remoteCommit.getParents();
		if (parents != null && parents.length > 0)
			baseCommit = remoteCommit.getParent(0);
		else {
			baseCommit = null;
		}
	}

	@Override
	public GitModelObject[] getChildren() {
		if (children == null)
			getChildrenImpl();

		return children;

	}

	@Override
	public String getName() {
		if (name == null)
			name = remoteCommit.getName().substring(0, 6)
					+ ": " + remoteCommit.getShortMessage();//$NON-NLS-1$

		return name;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	/**
	 * Returns common ancestor for this commit and all it parent's commits.
	 *
	 * @return common ancestor commit
	 */
	public RevCommit getAncestorCommit() {
		return ancestorCommit;
	}

	/**
	 * Returns instance of commit that is parent for one that is associated with
	 * this model object.
	 *
	 * @return base commit
	 */
	public RevCommit getBaseCommit() {
		return baseCommit;
	}

	/**
	 * Resurns instance of commit that is associated with this model object.
	 *
	 * @return rev commit
	 */
	public RevCommit getRemoteCommit() {
		return remoteCommit;
	}

	@Override
	public IPath getLocation() {
		return getParent().getLocation();
	}

	@Override
	public boolean equals(Object obj) {
		return remoteCommit.equals(obj);
	}

	@Override
	public int hashCode() {
		return remoteCommit.hashCode();
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

	private void getChildrenImpl() {
		TreeWalk tw = createTreeWalk();
		List<GitModelObject> result = new ArrayList<GitModelObject>();
		try {
			int ancestorNth = tw.addTree(ancestorCommit.getTree());
			int baseNth = -1;
			if (baseCommit != null)
				baseNth = tw.addTree(baseCommit.getTree());
			int actualNth = tw.addTree(remoteCommit.getTree());

			while (tw.next()) {
				GitModelObject obj = getModelObject(tw, ancestorNth, baseNth,
						actualNth);
				if (obj != null)
					result.add(obj);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		children = result.toArray(new GitModelObject[result.size()]);
	}

	private GitModelObject getModelObject(TreeWalk tw, int ancestorNth,
			int baseNth, int actualNth) throws IOException {
		String objName = tw.getNameString();

		ObjectId objBaseId;
		if (baseNth > -1)
			objBaseId = tw.getObjectId(baseNth);
		else
			objBaseId = ObjectId.zeroId();

		ObjectId objRemoteId = tw.getObjectId(actualNth);
		ObjectId objAncestorId = tw.getObjectId(ancestorNth);
		int objectType = tw.getFileMode(actualNth).getObjectType();

		if (objectType == Constants.OBJ_BLOB)
			return new GitModelBlob(this, remoteCommit, objAncestorId,
					objBaseId, objRemoteId, objName);
		else if (objectType == Constants.OBJ_TREE)
			return new GitModelTree(this, remoteCommit, objAncestorId,
					objBaseId, objRemoteId, objName);

		return null;
	}

}
