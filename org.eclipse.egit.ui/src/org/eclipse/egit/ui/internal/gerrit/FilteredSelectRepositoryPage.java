/*******************************************************************************
 * Copyright (C) 2018, 2021 vogella GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jonas Hungershausen (vogella GmbH) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.gerrit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;

/**
 * A {@link GitSelectRepositoryPage} that can be filtered to some specific
 * repositories.
 */
public abstract class FilteredSelectRepositoryPage
		extends GitSelectRepositoryPage {

	/**
	 * Creates a new {@link FilteredSelectRepositoryPage}.
	 *
	 * @param title
	 *            to use
	 * @param image
	 *            to use
	 */
	public FilteredSelectRepositoryPage(String title, ImageDescriptor image) {
		super(false, false);
		setTitle(title);
		setDescription(null);
		setImageDescriptor(image);
	}

	@Override
	protected List<String> getInitialRepositories() {
		List<String> configuredRepos = RepositoryUtil.INSTANCE
				.getConfiguredRepositories();
		return configuredRepos.stream().map(name -> {
			File gitDir = new File(name);
			if (gitDir.exists()) {
				try {
					Repository repo = RepositoryCache.INSTANCE
							.lookupRepository(gitDir);
					if (repo != null && includeRepository(repo)) {
						return name;
					}
				} catch (IOException e) {
					Activator.logWarning(e.getLocalizedMessage(), e);
				}
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	/**
	 * Determines whether the repository shall be included.
	 *
	 * @param repository
	 *            to check
	 * @return {@code true} to include the repository; {@code false} otherwise
	 */
	protected abstract boolean includeRepository(@NonNull Repository repository);
}
