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

import org.eclipse.compare.IResourceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Git blob object representation in Git ChangeSet
 */
public class GitModelBlob extends GitModelCommit implements IResourceProvider {

	private final IPath location;

	/** {@link ObjectId} of base variant */
	protected final ObjectId baseId;

	/** {@link ObjectId} of remove variant */
	protected final ObjectId remoteId;

	private static final GitModelObject[] empty = new GitModelObject[0];

	/**
	 * Git repository relative path of file associated with this
	 * {@link GitModelBlob}
	 */
	protected final String gitPath;

	/**
	 *
	 * @param parent
	 *            parent of this object
	 * @param commit
	 *            remote commit
	 * @param ancestorCommit TODO
	 * @param baseId
	 *            id of base object variant
	 * @param remoteId
	 *            id of remote object variants
	 * @param location
	 *            absolute blob location
	 * @throws IOException
	 */
	public GitModelBlob(GitModelObjectContainer parent, RevCommit commit,
			RevCommit ancestorCommit, ObjectId baseId, ObjectId remoteId, IPath location)
			throws IOException {
		// only direction is important for us, therefore we mask rest of bits in
		// kind
		super(parent, commit, ancestorCommit, parent.getKind() & (LEFT | RIGHT));
		this.baseId = baseId;
		this.remoteId = remoteId;
		this.location = location;
		gitPath = Repository.stripWorkDir(getRepository().getWorkTree(),
				getLocation().toFile());
	}

	/**
	 * @return git path
	 */
	public String getGitPath() {
		return gitPath;
	}

	/**
	 * @return base object id
	 */
	public ObjectId getBaseId() {
		return baseId;
	}

	/**
	 * @return remote object id
	 */
	public ObjectId getRemoteId() {
		return remoteId;
	}

	@Override
	public GitModelObject[] getChildren() {
		return empty;
	}

	@Override
	public String getName() {
		return location.lastSegment();
	}

	@Override
	public IPath getLocation() {
		return location;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public int getKind() {
		if (kind != LEFT && kind != RIGHT)
			return kind;

		int changeKind;
		if (zeroId().equals(baseId))
			changeKind = DELETION;
		else if (zeroId().equals(remoteId) || remoteId == null)
			changeKind = ADDITION;
		else
			changeKind = CHANGE;

		kind |= changeKind;

		return kind;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (obj.getClass() != getClass())
			return false;

		GitModelBlob objBlob = (GitModelBlob) obj;

		boolean equalsRemoteId;
		ObjectId objRemoteId = objBlob.remoteId;
		if (objRemoteId != null)
			equalsRemoteId = objRemoteId.equals(remoteId);
		else
			equalsRemoteId = remoteId == null;

		return objBlob.baseId.equals(baseId) && equalsRemoteId
				&& objBlob.location.equals(location);
	}

	@Override
	public int hashCode() {
		int result = baseId.hashCode() ^ location.hashCode();
		if (remoteId != null)
			result ^= remoteId.hashCode();

		return result;
	}

	@Override
	public String toString() {
		return "ModelBlob[objectId=" + baseId + ", location=" + getLocation() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IResource getResource() {
		String absoluteFilePath = getRepository().getWorkTree()
				.getAbsolutePath() + "/" + gitPath; //$NON-NLS-1$
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFileForLocation(new Path(absoluteFilePath));
		return file;
	}

}
