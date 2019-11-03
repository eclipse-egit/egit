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

import java.io.IOException;
import java.util.function.Supplier;

import org.eclipse.jgit.lib.Repository;

/**
 * A {@code UnitOfWork} provides a way to execute one or several operations on a
 * git repository as a single unit during which a {@link Repository} may cache
 * its {@link Repository#getConfig() Config}.
 * <p>
 * With the repositories returned by the {@link RepositoryCache}, caching of the
 * config is supported; the config is cached per {@link Thread}.
 * </p>
 */
public final class UnitOfWork {

	private UnitOfWork() {
		// No instantiation
	}

	/**
	 * A variation of {@link Runnable} possibly throwing an {@link IOException}.
	 */
	@FunctionalInterface
	public interface Work {

		/**
		 * Performs the work.
		 *
		 * @throws IOException
		 *             on errors
		 */
		void run() throws IOException;
	}

	/**
	 * A variation of {@link Supplier} possibly throwing an {@link IOException}.
	 *
	 * @param <T>
	 *            return type
	 */
	@FunctionalInterface
	public interface WorkWithResult<T> {

		/**
		 * Performs the work and returns its result.
		 *
		 * @return a value
		 * @throws IOException
		 *             on errors
		 */
		T get() throws IOException;
	}

	/**
	 * Executes the given {@link Work}. If the {@code repository} supports
	 * config caching, the git config is cached during the {@code work}.
	 *
	 * @param repository
	 *            to operate on
	 * @param work
	 *            to perform
	 * @throws IOException
	 *             if {@code work} throws it
	 */
	public static void run(Repository repository, Work work)
			throws IOException {
		try (ConfigScope scope = new ConfigScope(repository)) {
			work.run();
		}
	}

	/**
	 * Executes the given {@code work}. If the {@code repository} supports
	 * config caching, the git config is cached during the {@code work}.
	 *
	 * @param repository
	 *            to operate on
	 * @param work
	 *            to perform
	 */
	public static void execute(Repository repository, Runnable work) {
		try (ConfigScope scope = new ConfigScope(repository)) {
			work.run();
		}
	}

	/**
	 * Runs a {@link WorkWithResult}. If the {@code repository} supports config
	 * caching, the git config is cached during the {@code work}.
	 *
	 * @param repository
	 *            to operate on
	 * @param work
	 *            to perform
	 * @return the result of {@code work}
	 * @throws IOException
	 *             if {@code work} throws it
	 */
	public static <T> T run(Repository repository,
			WorkWithResult<? extends T> work) throws IOException {
		try (ConfigScope scope = new ConfigScope(repository)) {
			return work.get();
		}
	}

	/**
	 * Executes the given {@code work} and returns its result. If the
	 * {@code repository} supports config caching, the git config is cached
	 * during the {@code work}.
	 *
	 * @param repository
	 *            to operate on
	 * @param work
	 *            to perform
	 * @return the result of {@code work}
	 */
	public static <T> T get(Repository repository, Supplier<? extends T> work) {
		try (ConfigScope scope = new ConfigScope(repository)) {
			return work.get();
		}
	}

	/**
	 * A {@code ConfigScope} can be used to make a repository cache its git
	 * config for some time. On repositories not supporting config caching the
	 * scope has no effect.
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
	private static class ConfigScope implements AutoCloseable {

		private CachingRepository repo;

		/**
		 * Creates a new {@link ConfigScope} and initiates caching of the
		 * {@link Repository#getConfig() Config} if the {@code repository}
		 * supports config caching.
		 *
		 * @param repository
		 *            to open the scope for
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

}
