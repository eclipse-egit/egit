/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.eclipse.team.core.synchronize.SyncInfo.ADDITION;
import static org.eclipse.team.core.synchronize.SyncInfo.CHANGE;
import static org.eclipse.team.core.synchronize.SyncInfo.CONFLICTING;
import static org.eclipse.team.core.synchronize.SyncInfo.DELETION;
import static org.eclipse.team.core.synchronize.SyncInfo.INCOMING;
import static org.eclipse.team.core.synchronize.SyncInfo.OUTGOING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObject implements
		ISynchronizationCompareInput {

	private final RevCommit baseCommit;

	private final RevCommit remoteCommit;

	private final RevCommit ancestorCommit;

	private int kind = -1;

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
		if (obj instanceof GitModelCommit) {
			GitModelCommit objCommit = (GitModelCommit) obj;

			boolean equalsBaseCommit;
			RevCommit objBaseCommit = objCommit.getBaseCommit();
			if (objBaseCommit != null)
				equalsBaseCommit = objBaseCommit.equals(baseCommit);
			else
				equalsBaseCommit = baseCommit == null;

			// it is impossible that to have different common ancestor commit if
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
		final int prime = 31;
		int result = 1;
		result = prime * result + getLocation().hashCode();
		result = prime * result + remoteCommit.hashCode();
		if (baseCommit != null)
			result = prime * result + baseCommit.hashCode();

		return result;
	}

	public Image getImage() {
		// currently itsn't used
		return null;
	}

	public int getKind() {
		if (kind == -1)
			calculateKind(getAncestorSha1(), getBaseSha1(), getRemoteSha1());

		return kind;
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

	/**
	 * @return SHA1 of ancestor object
	 */
	protected String getAncestorSha1() {
		return ancestorCommit.getId().getName();
	}

	/**
	 * @return SHA1 of base object
	 */
	protected String getBaseSha1() {
		return baseCommit.getId().getName();
	}

	/**
	 * @return SHA1 of remote object
	 */
	protected String getRemoteSha1() {
		return remoteCommit.getId().getName();
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
			RevTree actualTree = remoteCommit.getTree();
			List<String> notIgnored = getNotIgnoredNodes(actualTree);

			int actualNth = tw.addTree(actualTree);
			int baseNth = -1;
			if (baseCommit != null)
				baseNth = tw.addTree(baseCommit.getTree());
			int ancestorNth = tw.addTree(ancestorCommit.getTree());

			while (tw.next()) {
				if (!notIgnored.contains(tw.getNameString()))
					continue;

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

	private void calculateKind(String ancestorSha1, String baseSha1,
			String remoteSha1) {
		if (ancestorSha1.equals(baseSha1))
			kind = INCOMING;
		else if (ancestorSha1.equals(baseSha1))
			kind = OUTGOING;
		else
			kind = CONFLICTING;

		if (baseSha1.equals(zeroId().getName()))
			kind = kind | ADDITION;
		else if (remoteSha1.equals(zeroId().getName()))
			kind = kind | DELETION;
		else
			kind = kind | CHANGE;
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

}
