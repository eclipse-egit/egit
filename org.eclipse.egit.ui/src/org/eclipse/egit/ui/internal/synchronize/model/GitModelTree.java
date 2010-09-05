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
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Representation of Git tree's in Git ChangeSet model
 */
public class GitModelTree extends GitModelCommit {

	private final String name;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final ObjectId ancestorId;

	private GitModelObject[] children;

	private IPath location;

	/**
	 * @param parent
	 *            parent of this tree
	 * @param commit
	 *            commit associated with this tree
	 * @param ancestorId
	 *            id of common ancestor tree for this on
	 * @param baseId
	 *            id of base tree
	 * @param remoteId
	 *            this tree id
	 * @param name
	 *            name resource associated with this tree
	 * @throws IOException
	 */
	public GitModelTree(GitModelCommit parent, RevCommit commit,
			ObjectId ancestorId, ObjectId baseId, ObjectId remoteId, String name)
			throws IOException {
		// only direction is important for us, therefore we mask rest of bits in kind
		super(parent, commit, parent.getKind() & (LEFT | RIGHT));
		this.name = name;
		this.baseId = baseId;
		this.remoteId = remoteId;
		this.ancestorId = ancestorId;
	}

	@Override
	public GitModelObject[] getChildren() {
		if (children == null)
			getChildrenImpl();

		return children;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	/**
	 * Return id of tree that should be used as a base variant in
	 * three-way-compare.
	 *
	 * @return base id
	 */
	public ObjectId getBaseId() {
		return baseId;
	}

	/**
	 * Returns id of tree that should be used as a remote in three-way-compare
	 *
	 * @return object id
	 */
	public ObjectId getRemoteId() {
		return remoteId;
	}

	@Override
	public IPath getLocation() {
		if (location == null)
			location = getParent().getLocation().append(name);

		return location;
	}

	@Override
	protected ObjectId getBaseObjectId() {
		return baseId;
	}

	@Override
	protected ObjectId getRemoteObjectId() {
		return remoteId;
	}

	private void getChildrenImpl() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			TreeWalk tw = createTreeWalk();
			int remoteNth = tw.addTree(remoteId);

			int baseNth = -1;
			if (!baseId.equals(zeroId()))
				baseNth = tw.addTree(baseId);

			int ancestorNth = -1;
			if (!ancestorId.equals(zeroId()))
				ancestorNth = tw.addTree(ancestorId);

			while (tw.next()) {
				GitModelObject obj = createChildren(tw, ancestorNth, baseNth,
						remoteNth);
				if (obj != null)
					result.add(obj);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		children = result.toArray(new GitModelObject[result.size()]);
	}

	private GitModelObject createChildren(TreeWalk tw, int ancestorNth,
			int baseNth, int remoteNth) throws IOException {
		ObjectId objRemoteId = tw.getObjectId(remoteNth);
		if (objRemoteId.equals(zeroId()))
			return null;

		String objName = tw.getNameString();
		ObjectId objBaseId = baseNth != -1 ? tw.getObjectId(baseNth) : zeroId();
		ObjectId objAncestorId = ancestorNth != -1 ? tw
				.getObjectId(ancestorNth) : zeroId();
		int objectType = tw.getFileMode(remoteNth).getObjectType();

		if (objectType == Constants.OBJ_BLOB)
			return new GitModelBlob(this, getRemoteCommit(), objAncestorId,
					objBaseId, objRemoteId, objName);
		else if (objectType == Constants.OBJ_TREE)
			return new GitModelTree(this, getRemoteCommit(), objAncestorId,
					objBaseId, objRemoteId, objName);

		return null;
	}

}
