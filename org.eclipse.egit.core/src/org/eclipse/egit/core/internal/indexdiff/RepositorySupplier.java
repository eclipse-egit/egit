/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.function.Supplier;

import org.eclipse.jgit.lib.Repository;

class RepositorySupplier implements Supplier<Repository> {

	private boolean initialized = false;

	private Repository repository;

	private final IndexDiffCacheEntry entry;

	RepositorySupplier(IndexDiffCacheEntry entry) {
		this.entry = entry;
	}

	@Override
	public Repository get() {
		if (!initialized) {
			initialized = true;
			repository = entry.getRepository();
		}
		return repository;
	}

}
