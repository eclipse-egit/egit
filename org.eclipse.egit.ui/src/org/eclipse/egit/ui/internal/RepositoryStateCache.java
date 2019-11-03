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
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * A cache of some state of repositories. Concrete subclasses are responsible
 * for clearing this cache in response to some event.
 */
public abstract class RepositoryStateCache {

	private enum RepositoryItem {
		CONFIG, HEAD, HEAD_REF, HEAD_COMMIT, FULL_BRANCH_NAME, STATE
	}

	/** "null" marker in the maps. */
	private static final Object NOTHING = new Object();

	private final Map<File, Map<RepositoryItem, Object>> cache = new ConcurrentHashMap<>();

	/**
	 * Initializes the {@link RepositoryStateCache} and makes it listen to changes
	 * that may affect the cache.
	 */
	public abstract void initialize();

	/**
	 * Disposes the {@link RepositoryStateCache}.
	 */
	public void dispose() {
		clear();
	}

	/**
	 * Completely clears the cache.
	 */
	public void clear() {
		cache.clear();
	}

	/**
	 * Clears all cached entries for the given {@link Repository}.
	 *
	 * @param repository
	 *            to remove cached items of
	 */
	public void clear(Repository repository) {
		cache.remove(repository.getDirectory());
	}

	private Map<RepositoryItem, Object> getItems(Repository repository) {
		return cache.computeIfAbsent(repository.getDirectory(),
				gitDir -> new ConcurrentHashMap<>());
	}

	/**
	 * Retrieves the given repository's {@link StoredConfig}.
	 *
	 * @param repository
	 * @return the {@link StoredConfig} of the repository
	 */
	public StoredConfig getConfig(Repository repository) {
		Object value = getItems(repository).computeIfAbsent(
				RepositoryItem.CONFIG, key -> repository.getConfig());
		return (StoredConfig) value;
	}

	private ObjectId getHead(Repository repository,
			String[] fullName, Ref[] ref) {
		return UnitOfWork.get(repository, () -> {
			ObjectId head = ObjectId.zeroId();
			String name = null;
			Ref r = null;
			try {
				r = repository.exactRef(Constants.HEAD);
			} catch (IOException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
			ref[0] = r;
			if (r != null) {
				if (r.isSymbolic()) {
					name = r.getTarget().getName();
				}
				head = r.getObjectId();
				if (head != null) {
					if (name == null) {
						name = head.name();
					}
				} else {
					head = ObjectId.zeroId();
				}
			}
			fullName[0] = name != null ? name : ""; //$NON-NLS-1$
			return head;
		});
	}

	/**
	 * Retrieves the {@link ObjectId} of the current HEAD.
	 *
	 * @param repository
	 * @return ObjectId of HEAD, or {@code null} if none
	 */
	public ObjectId getHead(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object value = items.get(RepositoryItem.HEAD);
		if (value == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			value = items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			items.computeIfAbsent(RepositoryItem.FULL_BRANCH_NAME,
					key -> fullName[0]);
			items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		ObjectId head = (ObjectId) value;
		if (head == null || head.equals(ObjectId.zeroId())) {
			return null;
		}
		return head;
	}


	/**
	 * Retrieves the current HEAD ref.
	 *
	 * @param repository
	 * @return the HEAD ref, or {@code null} if none
	 */
	public Ref getHeadRef(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object value = items.get(RepositoryItem.HEAD_REF);
		if (value == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			items.computeIfAbsent(RepositoryItem.FULL_BRANCH_NAME,
					key -> fullName[0]);
			value = items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		if (value == null || value == NOTHING) {
			return null;
		}
		return (Ref) value;
	}

	/**
	 * Retrieves the current HEAD commit of the repository.
	 *
	 * @param repository
	 * @return the commit, or {@code null} if none could be determined
	 */
	public RevCommit getHeadCommit(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object value = items.get(RepositoryItem.HEAD_COMMIT);
		if (value == null) {
			ObjectId headId = getHead(repository);
			if (headId != null) {
				try (RevWalk w = new RevWalk(repository)) {
					RevCommit commit = w.parseCommit(headId);
					items.put(RepositoryItem.HEAD_COMMIT, commit);
					return commit;
				} catch (IOException e) {
					// Ignore here
				}
			}
			items.put(RepositoryItem.HEAD_COMMIT, NOTHING);
			return null;
		} else if (value == NOTHING) {
			return null;
		}
		return (RevCommit) value;
	}

	/**
	 * Retrieves the full name of the current branch.
	 *
	 * @param repository
	 * @return the full branch name
	 */
	public String getFullBranchName(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object fullBranchName = items.get(RepositoryItem.FULL_BRANCH_NAME);
		if (fullBranchName == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			fullBranchName = items.computeIfAbsent(
					RepositoryItem.FULL_BRANCH_NAME, key -> fullName[0]);
			items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		String name = (String) fullBranchName;
		if (name == null || name.isEmpty()) {
			return null;
		}
		return name;
	}

	/**
	 * Retrieves the repository state.
	 *
	 * @param repository
	 * @return the {@link RepositoryState}
	 */
	public @NonNull RepositoryState getRepositoryState(Repository repository) {
		RepositoryState state = UnitOfWork.get(repository, () -> {
			Object value = getItems(repository).computeIfAbsent(
					RepositoryItem.STATE,
					key -> repository.getRepositoryState());
			return (RepositoryState) value;
		});
		assert state != null; // Keep the compiler happy.
		return state;
	}

}
