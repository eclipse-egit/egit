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

import java.io.IOException;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;

class SWTWalk extends PlotWalk {

	@NonNull
	private final Repository repo;

	private Ref head;

	private boolean headInitialized;

	SWTWalk(final @NonNull Repository repo) {
		super(repo);
		this.repo = repo;
	}

	@NonNull
	Repository getRepository() {
		return repo;
	}

	@Override
	protected void reset(int retainFlags) {
		headInitialized = false;
		head = null;
		super.reset(retainFlags);
	}

	@Override
	public RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (!headInitialized) {
			head = determineHead();
			headInitialized = true;
		}
		return super.next();
	}

	/**
	 * Retrieves the HEAD ref if it is symbolic.
	 *
	 * @return the Ref, or {@code null} if HEAD is not a symbolic Ref
	 * @throws IOException
	 *             if the head cannot be read
	 */
	public Ref getHead() throws IOException {
		if (!headInitialized) {
			head = determineHead();
			headInitialized = true;
		}
		return head;
	}

	@Override
	protected RevCommit createCommit(final AnyObjectId id) {
		return new SWTCommit(id, this);
	}

	private Ref determineHead() throws IOException {
		Ref h = repo.exactRef(Constants.HEAD);
		if (h != null && h.isSymbolic()) {
			return h;
		}
		return null;
	}
}
