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
package org.eclipse.egit.ui.internal.decorators;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.RepositoryStateCache;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.util.StringUtils;

/**
 * A {@link RepositoryStateCache} for decorators.
 */
public class DecoratorRepositoryStateCache extends RepositoryStateCache {

	/**
	 * The singleton {@link DecoratorRepositoryStateCache}.
	 */
	public static final DecoratorRepositoryStateCache INSTANCE = new DecoratorRepositoryStateCache();

	private final Map<File, String> branchLabels = new ConcurrentHashMap<>();

	private final Map<File, String> branchStateLabels = new ConcurrentHashMap<>();

	private DecoratorRepositoryStateCache() {
		// No public instantiation
	}

	@Override
	public void initialize() {
		// Nothing. Instead the label providers or decorators that listen to
		// events will clear their instance of this cache as appropriate.
	}

	@Override
	public void clear() {
		super.clear();
		branchLabels.clear();
		branchStateLabels.clear();
	}

	@Override
	public void clear(Repository repository) {
		super.clear(repository);
		branchLabels.remove(repository.getDirectory());
		branchStateLabels.remove(repository.getDirectory());
	}

	/**
	 * Retrieves a display name for the {@link Repository}, possibly augmented
	 * with state information.
	 *
	 * @param repository
	 *            to get the name of
	 * @return the name
	 */
	public String getRepositoryNameAndState(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = getRepositoryState(repository);
		if (state != RepositoryState.SAFE) {
			return repoName + '|' + state.getDescription();
		} else {
			return repoName;
		}
	}

	/**
	 * Retrieves a label for the current HEAD of a {@link Repository}.
	 *
	 * @param repository
	 *            to get the label of the current HEAD of
	 * @return the label
	 */
	public String getCurrentBranchLabel(Repository repository) {
		return branchLabels.computeIfAbsent(repository.getDirectory(), dir -> {
			return UnitOfWork.get(repository, () -> {
				Ref head = getHeadRef(repository);
				if (head == null) {
					return CoreText.RepositoryUtil_noHead;
				}
				if (head.isSymbolic()) {
					String branchName = getFullBranchName(repository);
					return Repository.shortenRefName(branchName);
				}
				ObjectId objectId = head.getObjectId();
				if (objectId == null) {
					return CoreText.RepositoryUtil_noHead;
				}
				String ref = Activator.getDefault().getRepositoryUtil()
						.mapCommitToRef(repository, objectId.name(), false);
				if (ref != null) {
					return Repository.shortenRefName(ref) + ' '
							+ objectId.abbreviate(7).name();
				} else {
					return objectId.abbreviate(7).name();
				}
			});
		});
	}

	/**
	 * Retrieves a label for the {@link BranchTrackingStatus} of the current
	 * HEAD.
	 *
	 * @param repository
	 *            to get the status label for
	 * @return the label, or {@code null} if none
	 */
	public String getBranchStatus(Repository repository) {
		String label = branchStateLabels
				.computeIfAbsent(repository.getDirectory(), dir -> {
					return UnitOfWork.get(repository, () -> {
						String branchName = getFullBranchName(repository);
						if (branchName == null) {
							return ""; //$NON-NLS-1$
						}
						BranchTrackingStatus status = null;
						try {
							status = BranchTrackingStatus.of(repository,
									branchName);
						} catch (IOException e) {
							// Ignore here; return null below.
						}
						if (status == null) {
							return ""; //$NON-NLS-1$
						}
						if (status.getAheadCount() == 0
								&& status.getBehindCount() == 0) {
							return ""; //$NON-NLS-1$
						}
						return GitLabels.formatBranchTrackingStatus(status);

					});
				});
		return StringUtils.isEmptyOrNull(label) ? null : label;
	}
}
