/*******************************************************************************
 * Copyright (C) 2018 vogella GmbH and others.
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
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.jgit.lib.Repository;

/**
 * Select a gerrit repository.
 */
public class GerritSelectRepositoryPage extends GitSelectRepositoryPage {

	private RepositoryUtil util;

	private RepositoryCache cache;

	/**
	 * Creates a new {@link GerritSelectRepositoryPage} that allows to select a
	 * configured gerrit repository
	 */
	public GerritSelectRepositoryPage() {
		super(false, false);
		util = Activator.getDefault().getRepositoryUtil();
		cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
		setTitle(UIText.GerritSelectRepositoryPage_PageTitle);
		setDescription(null);
		setImageDescriptor(UIIcons.WIZBAN_FETCH_GERRIT);
	}

	@Override
	protected List<String> getInitialRepositories() {
		List<String> configuredRepos = util.getConfiguredRepositories();
		return configuredRepos.stream().map(name -> {
			File gitDir = new File(name);
			if (gitDir.exists()) {
				try {
					Repository repo = cache.lookupRepository(gitDir);
					if (repo != null && ResourcePropertyTester.hasGerritConfiguration(repo)) {
						return name;
					}
				} catch (IOException e) {
					Activator.logWarning(e.getLocalizedMessage(), e);
				}
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
}
