/*******************************************************************************
 * Copyright (C) 2008, 2017 Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Tobias Baumann <tobbaumann@gmail.com> - Bug 475836
 *   Thomas Wolf <thomas.wolf@paranor.ch> - IRepositoryCommit
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.widgets.Widget;

class SWTCommit extends PlotCommit<SWTCommitList.SWTLane>
		implements IAdaptable, IRepositoryCommit {
	Widget widget;

	private SWTWalk walk;

	SWTCommit(final AnyObjectId id, SWTWalk walk) {
		super(id);
		this.walk = walk;
	}

	@Override
	public void reset() {
		widget = null;
		walk = null;
		super.reset();
	}

	public void parseBody() throws IOException {
		if (getRawBuffer() == null) {
			Repository repo = walk.getRepository();
			try (RevWalk w = new RevWalk(repo)) {
				// We *know* that the commit has had its headers parsed, so all
				// this does is add the cached bytes. Thus using a different
				// walk than the one that created this commit is fine. We
				// mustn't use "walk" since this may be called from different
				// threads: the UI thread, the FormatJob, and the
				// FindToolbarJob.
				//
				// Additionally the GitHistoryJob may still be using the walk.
				w.parseBody(this);
			}
		}
	}

	/**
	 * Retrieves the HEAD ref if it is symbolic.
	 *
	 * @return the Ref, or {@code null} if HEAD is not a symbolic Ref
	 * @throws IOException
	 *             if the head cannot be read
	 */
	public Ref getHead() throws IOException {
		return walk.getHead();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter != Repository.class) {
			return null;
		}
		return adapter.cast(getRepository());
	}

	@Override
	public Repository getRepository() {
		return walk != null ? walk.getRepository() : null;
	}

	@Override
	public RevCommit getRevCommit() {
		return this;
	}
}
