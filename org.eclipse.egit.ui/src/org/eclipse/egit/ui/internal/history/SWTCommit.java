/*******************************************************************************
 * Copyright (C) 2008, 2015 Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tobias Baumann <tobbaumann@gmail.com> - Bug 475836
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.swt.widgets.Widget;

class SWTCommit extends PlotCommit<SWTCommitList.SWTLane> implements IAdaptable {
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
		if (getRawBuffer() == null)
			walk.parseBody(this);
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter != Repository.class) {
			return null;
		}
		return adapter.cast(walk.getRepository());
	}
}
