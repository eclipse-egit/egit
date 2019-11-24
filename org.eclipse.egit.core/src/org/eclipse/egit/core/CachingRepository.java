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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.storage.file.FileBasedConfig;

/**
 * A {@link org.eclipse.jgit.internal.storage.file.FileRepository} providing
 * some means to cache its {@link org.eclipse.jgit.lib.Config Config} for some
 * time.
 */
@SuppressWarnings("restriction")
class CachingRepository extends FileRepository {

	private static final ThreadLocal<Map<File, CachedConfig>> CACHED_CONFIG = ThreadLocal
			.withInitial(HashMap::new);

	/**
	 * @param options
	 * @throws IOException
	 */
	public CachingRepository(BaseRepositoryBuilder options) throws IOException {
		super(options);
	}

	/**
	 * @param filename
	 * @throws IOException
	 */
	public CachingRepository(String filename) throws IOException {
		super(filename);
	}

	@Override
	public FileBasedConfig getConfig() {
		Map<File, CachedConfig> cache = CACHED_CONFIG.get();
		CachedConfig cfg = cache.get(getDirectory());
		if (cfg == null) {
			return super.getConfig();
		} else {
			if (cfg.config == null) {
				cfg.config = super.getConfig();
			}
			return cfg.config;
		}
	}

	/**
	 * Sets the git config caching mode. Calls with {@code doCache == true} and
	 * {@code false} must be balanced, but can be nested. The git config is
	 * cached <em>for the calling {@link Thread}</em> and not re-checked for
	 * having been modified on disk after the first call with
	 * {@code doCache == true} until an equal number of calls with
	 * {@code doCache == false} have occurred. A call to
	 * {@link #clearConfigCache()} will clear the caching for the calling
	 * thread.
	 *
	 * @param doCache
	 *            whether to start caching the git config in the calling
	 *            {@link Thread}
	 */
	void cacheConfig(boolean doCache) {
		Map<File, CachedConfig> cache = CACHED_CONFIG.get();
		CachedConfig cfg = cache.get(getDirectory());
		if (cfg == null) {
			if (!doCache) {
				return;
			}
			cfg = new CachedConfig();
			cache.put(getDirectory(), cfg);
		}
		if (!doCache) {
			if (cfg.level > 0) {
				if (--cfg.level == 0) {
					cache.remove(getDirectory());
				}
			} else {
				Activator.logWarning(MessageFormat.format(
						CoreText.CachingRepository_cacheLevelZero,
						getDirectory()), null);
			}
		} else if (doCache) {
			cfg.level++;
		}
	}

	/**
	 * Completely unwinds the caching stack in case of nested calls to
	 * {@link #cacheConfig(boolean)}. The git config of the repository will not
	 * be cached anymore for the calling thread.
	 */
	void clearConfigCache() {
		Map<File, CachedConfig> cache = CACHED_CONFIG.get();
		cache.remove(getDirectory());
	}

	@Override
	public void close() {
		clearConfigCache();
		super.close();
	}

	private static class CachedConfig {
		int level;
		FileBasedConfig config;
	}
}
