/*******************************************************************************
 * Copyright (C) 2008, 2015 Shawn O. Pearce <spearce@spearce.org>
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
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;

class SWTWalk extends PlotWalk {

	private final Repository repo;

	SWTWalk(final Repository repo) {
		super(repo);
		this.repo = repo;
	}

	Repository getRepository() {
		return repo;
	}

	@Override
	protected RevCommit createCommit(final AnyObjectId id) {
		return new SWTCommit(id, this);
	}
}
