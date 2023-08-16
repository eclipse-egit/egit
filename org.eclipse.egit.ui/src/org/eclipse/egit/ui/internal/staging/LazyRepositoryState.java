/*******************************************************************************
 * Copyright (C) 2023 SSI (Joerg Kubitz) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.function.Supplier;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

/** Used to cache result. This implementation is not thread-safe. **/
class LazyRepositoryState implements Supplier<RepositoryState> {
	private final Repository repo;

	private RepositoryState state;

	LazyRepositoryState(Repository repo) {
		this.repo = repo;
	}

    @Override
	public RepositoryState get() {
		if (state == null) {
			state = repo == null ? null : repo.getRepositoryState();
        }
		return state;
	}

	Repository getRepository() {
		return repo;
    }
}