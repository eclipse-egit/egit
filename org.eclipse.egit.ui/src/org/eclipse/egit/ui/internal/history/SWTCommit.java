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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
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
			walk.parseBody(this);
		}
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter != Repository.class) {
			return null;
		}
		return adapter.cast(walk.getRepository());
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
