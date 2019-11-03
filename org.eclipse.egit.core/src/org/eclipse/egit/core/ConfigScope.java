/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.jgit.lib.Repository;

/**
 * A {@code ConfigScope} can be used to make a repository cache its git config
 * for some time. On repositories not supporting config caching the scope has no
 * effect.
 * <p>
 * Intended usage is with a try-with-resource statement:
 * </p>
 *
 * <pre>
 * try (ConfigScope scope = new ConfigScope(repository)) {
 *   // ... do repository operations. The repository will check
 *   // its git config only once for having been modified and
 *   // subsequently always works with the cached config until
 *   // the end of the try-with-resource statement.
 * }
 * </pre>
 */
public class ConfigScope implements AutoCloseable {

	private CachingRepository repo;

	/**
	 * @param repository
	 */
	public ConfigScope(Repository repository) {
		Repository toCache = repository;
		if (repository instanceof RepositoryHandle) {
			toCache = ((RepositoryHandle) repository).getDelegate();
		}
		if (toCache instanceof CachingRepository) {
			repo = (CachingRepository) toCache;
			repo.cacheConfig(true);
		}
	}

	@Override
	public void close() {
		if (repo != null) {
			repo.cacheConfig(false);
			repo = null;
		}
	}
}
