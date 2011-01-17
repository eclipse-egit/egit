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

import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Git blob object representation in Git ChangeSet
 */
public class GitModelBlob extends GitModelCommit {

	private final String name;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final ObjectId ancestorId;

	private static final GitModelObject[] empty = new GitModelObject[0];

	private GitCompareInput compareInput;

	/**
	 * Git repository relative path of file associated with this
	 * {@link GitModelBlob}
	 */
	protected final String gitPath;

	/**
	 *
	 * @param parent
	 *            parent of this object
	 * @param location
	 *            blob location
	 * @param commit
	 *            blob location remote commit
	 * @param ancestorId
	 *            common ancestor id
	 * @param baseId
	 *            id of base object variant
	 * @param remoteId
	 *            id of remote object variants
	 * @throws IOException
	 */
	public GitModelBlob(GitModelObjectContainer parent, IPath location, RevCommit commit,
			ObjectId ancestorId, ObjectId baseId, ObjectId remoteId)
			throws IOException {
		// only direction is important for us, therefore we mask rest of bits in kind
		super(parent, location, commit, parent.getKind() & (LEFT | RIGHT));
		this.name = location.lastSegment();
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
	public boolean isContainer() {
		return false;
	}

	@Override
	public ITypedElement getAncestor() {
		createCompareInput();
		return compareInput.getAncestor();
	}

	@Override
	public ITypedElement getLeft() {
		createCompareInput();
		return compareInput.getLeft();
	}

	@Override
	public ITypedElement getRight() {
		createCompareInput();
		return compareInput.getRight();
	}

	@Override
	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		createCompareInput();
		compareInput.prepareInput(configuration, monitor);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelBlob) {
			GitModelBlob objBlob = (GitModelBlob) obj;

			boolean equalsRemoteId;
			ObjectId objRemoteId = objBlob.remoteId;
			if (objRemoteId != null)
				equalsRemoteId = objRemoteId.equals(remoteId);
			else
				equalsRemoteId = baseCommit == null;

			return objBlob.baseId.equals(baseId) && equalsRemoteId;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int result = baseId.hashCode();
		if (remoteId != null)
			result ^= remoteId.hashCode();

		return result;
	}

	private void createCompareInput() {
		if (compareInput == null) {
			ComparisonDataSource baseData = new ComparisonDataSource(
					baseCommit, baseId);
			ComparisonDataSource remoteData = new ComparisonDataSource(
					remoteCommit, remoteId);
			ComparisonDataSource ancestorData = new ComparisonDataSource(
					ancestorCommit, ancestorId);
			compareInput = getCompareInput(baseData, remoteData, ancestorData);
		}
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
		return new GitCompareInput(getRepository(), ancestorData, baseData,
				remoteData, gitPath);
	}

}
