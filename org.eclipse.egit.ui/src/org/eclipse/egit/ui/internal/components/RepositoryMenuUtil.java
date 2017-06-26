/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jgit.lib.Repository;

/**
 * Provides a way to populate a menu with a list of repositories.
 */
public final class RepositoryMenuUtil {

	private RepositoryMenuUtil() {
		// Utility class shall not be instantiated
	}

	/**
	 * Populates the given {@link IMenuManager} with a list of repositories.
	 * Each currently known configured repository is shown with its repository
	 * name and the path to the .git directory as tooltip; when a menu item is
	 * selected, the given {@code action} is invoked. Bare repositories can be
	 * excluded from the list. Menu items are sorted by repository name and .git
	 * directory paths.
	 *
	 * @param menuManager
	 *            to populate with the list of repositories
	 * @param includeBare
	 *            {@code true} is bare repositories should be included in the
	 *            list, {@code false} otherwise
	 * @param action
	 *            to perform on the chosen repository
	 */
	public static void fillRepositories(IMenuManager menuManager,
			boolean includeBare, Consumer<Repository> action) {
		RepositoryUtil util = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryUtil();
		RepositoryCache cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
		Set<String> repositories = util.getRepositories();
		Map<String, Set<File>> repos = new HashMap<>();
		for (String repo : repositories) {
			File gitDir = new File(repo);
			String name = null;
			try {
				Repository r = cache.lookupRepository(gitDir);
				if (!includeBare && r.isBare()) {
					continue;
				}
				name = util.getRepositoryName(r);
			} catch (IOException e) {
				continue;
			}
			Set<File> files = repos.get(name);
			if (files == null) {
				files = new HashSet<>();
				files.add(gitDir);
				repos.put(name, files);
			} else {
				files.add(gitDir);
			}
		}
		String[] repoNames = repos.keySet().toArray(new String[repos.size()]);
		Arrays.sort(repoNames);
		for (String repoName : repoNames) {
			Set<File> files = repos.get(repoName);
			File[] gitDirs = files.toArray(new File[files.size()]);
			Arrays.sort(gitDirs);
			for (File f : gitDirs) {
				IAction menuItem = new Action(repoName) {
					@Override
					public void run() {
						try {
							Repository r = cache.lookupRepository(f);
							action.accept(r);
						} catch (IOException e) {
							Activator.showError(e.getLocalizedMessage(), e);
						}
					}
				};
				menuItem.setToolTipText(f.getPath());
				menuManager.add(menuItem);
			}
		}
	}
}
