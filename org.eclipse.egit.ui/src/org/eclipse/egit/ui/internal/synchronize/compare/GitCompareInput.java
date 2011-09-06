/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.egit.core.internal.storage.GitFileRevision.INDEX;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.LocalResourceTypedElement;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * Git specific implementation of {@link ISynchronizationCompareInput}
 */
public class GitCompareInput implements ISynchronizationCompareInput {

	/**
	 * Git resource that should be used for comparing
	 */
	protected final GitModelBlob resource;

	/**
	 * Creates {@link GitCompareInput}
	 * @param object
	 */
	public GitCompareInput(GitModelBlob object) {
		this.resource = object;
	}

	public String getName() {
		return resource.getName();
	}

	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getKind() {
		return resource.getKind();
	}

	public ITypedElement getAncestor() {
		return getRight();
	}

	public ITypedElement getLeft() {
		if ((resource.getKind() & RIGHT) == RIGHT)
			return CompareUtils.getFileRevisionTypedElement(resource.getGitPath(),
				resource.getBaseCommit(), resource.getRepository(),
				resource.getBaseId());
		else
			return CompareUtils.getFileRevisionTypedElement(resource.getGitPath(),
					resource.getRemoteCommit(), resource.getRepository(),
					resource.getRemoteId());
	}

	public ITypedElement getRight() {
		if ((resource.getKind() & RIGHT) == RIGHT)
			return CompareUtils.getFileRevisionTypedElement(resource.getGitPath(),
					resource.getRemoteCommit(), resource.getRepository(),
					resource.getRemoteId());
		else
			return CompareUtils.getFileRevisionTypedElement(resource.getGitPath(),
				resource.getBaseCommit(), resource.getRepository(),
				resource.getBaseId());

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
		configuration.setLeftEditable(false);
		configuration.setRightEditable(false);
	}

	public String getFullPath() {
		return resource.getLocation().toOSString();
	}

	public boolean isCompareInputFor(Object obj) {
		// not used
		return false;
	}

	private String getFileRevisionLabel(ITypedElement element) {
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
