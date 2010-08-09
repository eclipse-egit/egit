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

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.Image;

/**
 * Git blob object representation in Git ChangeSet
 */
public class GitModelBlob extends GitModelCommit implements ICompareInput {

	private final String name;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final ObjectId ancestorId;

	private final IPath location;

	private final String gitPath;

	private int kind = -1;

	private static final GitModelObject[] empty = new GitModelObject[0];

	/**
	 *
	 * @param parent
	 *            parent of this object
	 * @param commit
	 *            remote commit
	 * @param ancestorId
	 *            common ancestor id
	 * @param baseId
	 *            id of base object variant
	 * @param remoteId
	 *            id of remote object variant
	 * @param name
	 *            human readable blob name (file name)
	 * @throws IOException
	 */
	public GitModelBlob(GitModelObject parent, RevCommit commit,
			ObjectId ancestorId, ObjectId baseId, ObjectId remoteId, String name)
			throws IOException {
		super(parent, commit);
		this.name = name;
		this.baseId = baseId;
		this.remoteId = remoteId;
		this.ancestorId = ancestorId;
		location = getParent().getLocation().append(name);
		gitPath = Repository.stripWorkDir(getRepository().getWorkTree(),
				getLocation().toFile());
	}

	@Override
	public GitModelObject[] getChildren() {
		return empty;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	@Override
	public IPath getLocation() {
		return location;
	}

	public Image getImage() {
		// currently itsn't used
		return null;
	}

	public int getKind() {
		if (kind == -1)
			calculateKind();

		return kind;
	}

	public ITypedElement getAncestor() {
		return CompareUtils.getFileRevisionTypedElement(gitPath,
				getAncestorCommit(), getRepository(), ancestorId);
	}

	public ITypedElement getLeft() {
		if (getBaseCommit() != null && baseId != null)
			return CompareUtils.getFileRevisionTypedElement(gitPath,
					getBaseCommit(), getRepository(), baseId);

		return null;
	}

	public ITypedElement getRight() {
		return CompareUtils.getFileRevisionTypedElement(gitPath,
				getRemoteCommit(), getRepository(), remoteId);
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

	private void calculateKind() {
		if (ancestorId.equals(baseId))
			kind = OUTGOING;
		else if (ancestorId.equals(remoteId))
			kind = INCOMING;
		else
			kind = CONFLICTING;

		if (baseId.equals(zeroId()))
			kind = kind | ADDITION;
		else if (remoteId.equals(zeroId()))
			kind = kind | DELETION;
		else
			kind = kind | CHANGE;
	}

}
