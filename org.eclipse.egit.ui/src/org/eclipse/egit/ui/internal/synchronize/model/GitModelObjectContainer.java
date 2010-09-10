/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.ADDITION;
import static org.eclipse.compare.structuremergeviewer.Differencer.CHANGE;
import static org.eclipse.compare.structuremergeviewer.Differencer.DELETION;
import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

abstract class GitModelObjectContainer extends GitModelObject implements
		ISynchronizationCompareInput {

	private int kind = -1;

	private String name;

	private GitModelObject[] children;

	protected final RevCommit baseCommit;

	protected final RevCommit remoteCommit;

	protected final RevCommit ancestorCommit;

	protected GitModelObjectContainer(GitModelObject parent, RevCommit commit,
			int direction) throws IOException {
		super(parent);
		kind = direction;
		remoteCommit = commit;
		ancestorCommit = calculateAncestor(remoteCommit);

		RevCommit[] parents = remoteCommit.getParents();
		if (parents != null && parents.length > 0)
			baseCommit = remoteCommit.getParent(0);
		else {
			baseCommit = null;
		}
	}

	public Image getImage() {
		// currently itsn't used
		return null;
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

	public int getKind() {
		if (kind == -1 || kind == LEFT || kind == RIGHT)
			calculateKind();

		return kind;
	}

	@Override
	public GitModelObject[] getChildren() {
		if (children == null)
			children = getChildrenImpl();

		return children;

	}

	@Override
	public String getName() {
		if (name == null)
			name = remoteCommit.getShortMessage();

		return name;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	@Override
	public IPath getLocation() {
		return getParent().getLocation();
	}

	public ITypedElement getAncestor() {
		return null;
	}

	public ITypedElement getLeft() {
		return null;
	}

	public ITypedElement getRight() {
		return null;
	}

	public void addCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		// data in commit will never change, therefore change listeners are
		// useless
	}

	public void removeCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		// data in commit will never change, therefore change listeners are
		// useless
	}

	public void copy(boolean leftToRight) {
		// do nothing, we should disallow coping content between commits
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	public SaveableComparison getSaveable() {
		// TODO Auto-generated method stub
		return null;
	}

	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		// there is no needed configuration for commit object
	}

	public String getFullPath() {
		return getLocation().toPortableString();
	}

	public boolean isCompareInputFor(Object object) {
		// TODO Auto-generated method stub
		return false;
	}

	protected abstract GitModelObject[] getChildrenImpl();

	protected GitModelObject getModelObject(TreeWalk tw, int ancestorNth,
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
			return new GitModelBlob(this, getRemoteCommit(), objAncestorId,
					objBaseId, objRemoteId, objName);
		else if (objectType == Constants.OBJ_TREE)
			return new GitModelTree(this, getRemoteCommit(), objAncestorId,
					objBaseId, objRemoteId, objName);

		return null;
	}

	private void calculateKind() {
		ObjectId base = baseCommit != null ? baseCommit.getId() : zeroId();
		if (base.equals(zeroId()))
			kind = kind | ADDITION;
		else if (remoteCommit.equals(zeroId()))
			kind = kind | DELETION;
		else
			kind = kind | CHANGE;
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
