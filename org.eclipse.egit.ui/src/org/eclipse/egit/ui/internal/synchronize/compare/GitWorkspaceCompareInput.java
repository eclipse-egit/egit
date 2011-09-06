/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.compare.structuremergeviewer.Differencer.CHANGE;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.LocalResourceTypedElement;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * Git specific implementation of {@link ISynchronizationCompareInput} for
 * Workspace presentation model
 */
public class GitWorkspaceCompareInput implements ISynchronizationCompareInput {

	private final String gitPath;

	private final Repository repo;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final RevCommit baseCommit;

	private final RevCommit remoteCommit;

	/**
	 * @param repo
	 * @param baseCommit
	 * @param baseId
	 * @param remoteCommit
	 * @param remoteId
	 * @param gitPath
	 */
	public GitWorkspaceCompareInput(Repository repo, RevCommit baseCommit, ObjectId baseId, RevCommit remoteCommit, ObjectId remoteId, String gitPath) {
		this.repo = repo;
		this.baseId = baseId;
		this.gitPath = gitPath;
		this.remoteId = remoteId;
		this.baseCommit = baseCommit;
		this.remoteCommit = remoteCommit;
	}

	public String getName() {
		int lastSeparator = gitPath.indexOf("/"); //$NON-NLS-1$
		if (lastSeparator > -1)
			return gitPath.substring(lastSeparator + 1, gitPath.length());
		else
			return gitPath;
	}

	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getKind() {
		return CHANGE;
	}

	public ITypedElement getAncestor() {
		return getRight();
	}

	public ITypedElement getLeft() {
		return CompareUtils.getFileRevisionTypedElement(gitPath, baseCommit,
				repo, baseId);
	}

	public ITypedElement getRight() {
		return CompareUtils.getFileRevisionTypedElement(gitPath, remoteCommit,
				repo, remoteId);
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

	public SaveableComparison getSaveable() {
		// not used
		return null;
	}

	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		configuration.setLeftLabel(getFileRevisionLabel(getLeft()));
		configuration.setRightLabel(getFileRevisionLabel(getRight()));

		// disable editing on both sides, since we can't edit checked in blobs
		configuration.setLeftEditable(true);
		configuration.setRightEditable(false);
	}

	public String getFullPath() {
		return gitPath;
	}

	public boolean isCompareInputFor(Object obj) {
		// not used
		return false;
	}

	private String getFileRevisionLabel(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement castElement = (FileRevisionTypedElement) element;
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
