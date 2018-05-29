/*******************************************************************************
 *  Copyright (c) 2011, 2014 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * Commit editor input class. This class wraps a {@link RepositoryCommit} to be
 * viewed in an editor.
 */
public class CommitEditorInput extends PlatformObject implements IEditorInput,
		IPersistableElement {

	private RepositoryCommit commit;

	/**
	 * Create commit editor input
	 *
	 * @param commit
	 */
	public CommitEditorInput(RepositoryCommit commit) {
		Assert.isNotNull(commit, "Repository commit cannot be null"); //$NON-NLS-1$
		this.commit = commit;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return commit.getRevCommit().hashCode();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		else if (obj instanceof CommitEditorInput) {
			RepositoryCommit inputCommit = ((CommitEditorInput) obj).commit;
			return commit.getRevCommit().equals(inputCommit.getRevCommit())
					&& commit.getRepository().equals(
							inputCommit.getRepository());
		} else
			return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (RepositoryCommit.class == adapter)
			return commit;

		if (RevCommit.class == adapter)
			return commit.getRevCommit();

		if (Repository.class == adapter)
			return commit.getRepository();

		return super.getAdapter(adapter);
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return UIIcons.CHANGESET;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return MessageFormat.format(UIText.CommitEditorInput_Name,
				commit.abbreviate(), commit.getRepositoryName());
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return MessageFormat.format(UIText.CommitEditorInput_ToolTip, commit
				.getRevCommit().getShortMessage(), commit.getRepositoryName());
	}

	/**
	 * Get repository commit
	 *
	 * @return commit
	 */
	public RepositoryCommit getCommit() {
		return this.commit;
	}

	/**
	 * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		CommitEditorInputFactory.saveState(memento, this);
	}

	/**
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	@Override
	public String getFactoryId() {
		return CommitEditorInputFactory.ID;
	}

}
