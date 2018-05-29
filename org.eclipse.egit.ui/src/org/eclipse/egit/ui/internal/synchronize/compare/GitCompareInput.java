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
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.egit.core.internal.storage.GitFileRevision.INDEX;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * Git specific implementation of {@link ISynchronizationCompareInput}
 */
public class GitCompareInput implements ISynchronizationCompareInput {

	private final String name;

	private final ObjectId ancestorId;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final RevCommit ancestorCommit;

	private final RevCommit remoteCommit;

	/**
	 * {@link RevCommit} instance of base commit associated with this compare
	 * input
	 */
	protected final RevCommit baseCommit;

	/**
	 * {@link Repository} associated with this compare compare input
	 */
	protected final Repository repo;

	/**
	 * Git repository relative path of file associated with this compare input
	 */
	protected final String gitPath;

	/**
	 * Creates {@link GitCompareInput}
	 *
	 * @param repo
	 *            repository that is connected with this object
	 * @param ancestorDataSource
	 *            data that should be use to obtain common ancestor object data
	 * @param baseDataSource
	 *            data that should be use to obtain base object data
	 * @param remoteDataSource
	 *            data that should be used to obtain remote object data
	 * @param gitPath
	 *            repository relative path of object
	 */
	public GitCompareInput(Repository repo,
			ComparisonDataSource ancestorDataSource,
			ComparisonDataSource baseDataSource,
			ComparisonDataSource remoteDataSource, String gitPath) {
		this.repo = repo;
		this.gitPath = gitPath;
		this.baseId = baseDataSource.getObjectId();
		this.remoteId = remoteDataSource.getObjectId();
		this.baseCommit = baseDataSource.getRevCommit();
		this.ancestorId = ancestorDataSource.getObjectId();
		this.remoteCommit = remoteDataSource.getRevCommit();
		this.ancestorCommit = ancestorDataSource.getRevCommit();
		this.name = gitPath.lastIndexOf('/') < 0 ? gitPath : gitPath
				.substring(gitPath.lastIndexOf('/') + 1);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getKind() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ITypedElement getAncestor() {
		if (objectExist(ancestorCommit, ancestorId))
			return CompareUtils.getFileRevisionTypedElement(gitPath,
					ancestorCommit, repo, ancestorId);

		return null;
	}

	@Override
	public ITypedElement getLeft() {
		return CompareUtils.getFileRevisionTypedElement(gitPath, baseCommit,
				repo, baseId);
	}

	@Override
	public ITypedElement getRight() {
		return CompareUtils.getFileRevisionTypedElement(gitPath, remoteCommit,
				repo, remoteId);
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

	private boolean objectExist(RevCommit commit, ObjectId id) {
		return commit != null && id != null && !id.equals(zeroId());
	}

	@Override
	public SaveableComparison getSaveable() {
		// not used
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
		return gitPath;
	}

	@Override
	public boolean isCompareInputFor(Object object) {
		// not used
		return false;
	}

	/**
	 * @param element
	 * @return compare editor description
	 */
	public static String getFileRevisionLabel(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement castElement = (FileRevisionTypedElement) element;
			if (INDEX.equals(castElement.getContentIdentifier()))
				return NLS.bind(
						UIText.GitCompareFileRevisionEditorInput_StagedVersion,
						element.getName());
			else
				return NLS.bind(
						UIText.GitCompareFileRevisionEditorInput_RevisionLabel,
						new Object[] {
								element.getName(),
								CompareUtils.truncatedRevision(castElement
										.getContentIdentifier()),
								castElement.getAuthor() });

		} else if (element instanceof LocalResourceTypedElement)
			return NLS.bind(
					UIText.GitCompareFileRevisionEditorInput_LocalVersion,
					element.getName());
		else
			return element.getName();
	}

}
