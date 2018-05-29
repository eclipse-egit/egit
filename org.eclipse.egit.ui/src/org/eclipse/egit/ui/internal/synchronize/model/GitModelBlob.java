/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput.getFileRevisionLabel;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * Git blob object representation in Git ChangeSet
 */
public class GitModelBlob extends GitModelObject implements
		ISynchronizationCompareInput, IResourceProvider {

	private static final GitModelObject[] empty = new GitModelObject[0];

	private final Change change;

	private ITypedElement ancestorElement;

	private ITypedElement leftElement;

	private ITypedElement rightElement;

	/**
	 * Absolute path to changed object
	 */
	protected final IPath path;

	/**
	 * {@link Repository} associated with this object
	 */
	protected final Repository repo;

	/**
	 * @param parent
	 *            parent object
	 * @param repo
	 *            repository associated with this object
	 * @param change
	 *            change associated with this object
	 * @param path
	 *            absolute path of change
	 */
	public GitModelBlob(GitModelObjectContainer parent, Repository repo,
			Change change, IPath path) {
		super(parent);
		this.repo = repo;
		this.path = path;
		this.change = change;
	}

	@Override
	public GitModelObject[] getChildren() {
		return empty;
	}

	@Override
	public String getName() {
		return path.lastSegment();
	}

	@Override
	public IPath getLocation() {
		return path;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public int getKind() {
		return change.getKind();
	}

	/**
	 * @return abbreviated object id of base commit
	 */
	public AbbreviatedObjectId getBaseCommitId() {
		return change.getCommitId();
	}

	/**
	 * @return abbreviated object id of remote commit
	 */
	public AbbreviatedObjectId getRemoteCommitId() {
		return change.getRemoteCommitId();
	}

	@Override
	public int repositoryHashCode() {
		return repo.getWorkTree().hashCode();
	}

	@Override
	public void dispose() {
		// there is nothing to dispose
	}

	@Override
	public String toString() {
		return "ModelBlob[objectId=" + change.getObjectId() + ", location=" + getLocation() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public Image getImage() {
		// not used
		return null;
	}

	@Override
	public ITypedElement getAncestor() {
		prepareTypedElements();
		return ancestorElement;
	}

	@Override
	public ITypedElement getLeft() {
		prepareTypedElements();
		return leftElement;
	}

	@Override
	public ITypedElement getRight() {
		prepareTypedElements();
		return rightElement;
	}

	@Override
	public void addCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		// data in commit will never change, therefore change listeners are
		// useless
	}

	@Override
	public void removeCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		// data in commit will never change, therefore change listeners are
		// useless
	}

	@Override
	public void copy(boolean leftToRight) {
		// do nothing, we should disallow coping content between commits
	}

	@Override
	public SaveableComparison getSaveable() {
		// unused
		return null;
	}

	@Override
	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		configuration.setLeftLabel(getFileRevisionLabel(getLeft()));
		configuration.setRightLabel(getFileRevisionLabel(getRight()));
	}

	@Override
	public String getFullPath() {
		return path.toOSString();
	}

	@Override
	public boolean isCompareInputFor(Object object) {
		// not used
		return false;
	}

	@Override
	public int hashCode() {
		int baseHash = 1;
		if (change != null)
			baseHash = change.getObjectId() != null ? change.getObjectId()
				.hashCode() : 31;
		int remoteHash = 11;
		if (change != null)
			remoteHash = change.getRemoteObjectId() != null ? change
				.getRemoteObjectId().hashCode() : 41;

		return baseHash ^ remoteHash ^ path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitModelBlob other = (GitModelBlob) obj;
		if (change == null) {
			if (other.change != null)
				return false;
		} else if (!change.equals(other.change))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;

		return true;
	}

	private void prepareTypedElements() {
		if (ancestorElement != null) // other elements should also not be null
			return;

		ComparisonDataSource baseData;
		ComparisonDataSource remoteData;

		RevCommit baseCommit = null;
		RevCommit remoteCommit = null;
		try (RevWalk rw = new RevWalk(repo)) {
			rw.setRetainBody(true);
			if (change.getCommitId() != null)
				baseCommit = rw.parseCommit(change.getCommitId().toObjectId());
			if (change.getRemoteCommitId() != null)
				remoteCommit = rw.parseCommit(change.getRemoteCommitId()
						.toObjectId());
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}
		if (baseCommit == null && remoteCommit != null)
			baseCommit = remoteCommit; // prevent from NPE for deleted files

		ObjectId localId = extractObjectId(change.getObjectId());
		ObjectId remoteId = extractObjectId(change.getRemoteObjectId());

		if ((getKind() & RIGHT) == RIGHT) {
			baseData = new ComparisonDataSource(baseCommit, localId);
			remoteData = new ComparisonDataSource(remoteCommit, remoteId);
		} else /* getKind() == LEFT */{
			baseData = new ComparisonDataSource(remoteCommit, remoteId);
			remoteData = new ComparisonDataSource(baseCommit, localId);
		}

		GitCompareInput compareInput = getCompareInput(baseData, remoteData, remoteData);

		ancestorElement = compareInput.getAncestor();
		leftElement = compareInput.getLeft();
		rightElement = compareInput.getRight();
	}

	@Override
	public IResource getResource() {
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFileForLocation(path);

		return file;
	}

	/**
	 * Returns specific instance of {@link GitCompareInput} for particular
	 * compare input.
	 *
	 * @param baseData
	 * @param remoteData
	 * @param ancestorData
	 * @return Git specific {@link ICompareInput}
	 */
	protected GitCompareInput getCompareInput(ComparisonDataSource baseData,
			ComparisonDataSource remoteData, ComparisonDataSource ancestorData) {
		String gitPath = Repository.stripWorkDir(repo.getWorkTree(), path.toFile());

		return new GitCompareInput(repo, ancestorData, baseData, remoteData,
				gitPath);
	}

	private ObjectId extractObjectId(AbbreviatedObjectId objectId) {
		if (objectId != null)
			return objectId.toObjectId();
		else
			return zeroId();
	}

}
