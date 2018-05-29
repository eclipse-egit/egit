/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
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
package org.eclipse.egit.ui.internal.search;

import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Commit match class.
 */
public class CommitMatch extends Match implements IWorkbenchAdapter {

	private RepositoryCommit commit;

	/**
	 * @param commit
	 */
	public CommitMatch(RepositoryCommit commit) {
		this(commit, 0, 0);
	}

	/**
	 * @param commit
	 * @param offset
	 * @param length
	 */
	public CommitMatch(RepositoryCommit commit, int offset, int length) {
		super(commit, offset, length);
		this.commit = commit;
	}

	/**
	 * Get repository commit
	 *
	 * @return commit
	 */
	public RepositoryCommit getCommit() {
		return this.commit;
	}

	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return this.commit.getImageDescriptor(object);
	}

	@Override
	public String getLabel(Object o) {
		return this.commit.getLabel(o);
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

}
