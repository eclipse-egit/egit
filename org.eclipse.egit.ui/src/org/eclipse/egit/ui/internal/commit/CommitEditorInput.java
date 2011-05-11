/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
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

	public int hashCode() {
		return commit.getRevCommit().hashCode();
	}

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

	public String toString() {
		return getName();
	}

	public Object getAdapter(Class adapter) {
		if (RepositoryCommit.class == adapter)
			return commit;

		if (RevCommit.class == adapter)
			return commit.getRevCommit();

		if (Repository.class == adapter)
			return commit.getRepository();

		return super.getAdapter(adapter);
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return UIIcons.CHANGESET;
	}

	public String getName() {
		return MessageFormat.format(UIText.CommitEditorInput_Name,
				commit.abbreviate(), commit.getRepositoryName());
	}

	public IPersistableElement getPersistable() {
		return this;
	}

	public String getToolTipText() {
		return MessageFormat.format(UIText.CommitEditorInput_ToolTip, commit
				.getRevCommit().name(), commit.getRepositoryName());
	}

	/**
	 * Get repository commit
	 *
	 * @return commit
	 */
	public RepositoryCommit getCommit() {
		return this.commit;
	}

	public void saveState(IMemento memento) {
		CommitEditorInputFactory.saveState(memento, this);
	}

	public String getFactoryId() {
		return CommitEditorInputFactory.ID;
	}

}
