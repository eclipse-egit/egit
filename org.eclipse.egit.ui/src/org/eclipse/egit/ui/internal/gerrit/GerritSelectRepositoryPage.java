package org.eclipse.egit.ui.internal.gerrit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.jgit.lib.Repository;

/**
 * Select a gerrit repository
 *
 */
public class GerritSelectRepositoryPage extends GitSelectRepositoryPage {

	private RepositoryUtil util;

	private RepositoryCache cache;

	/**
	 *
	 */
	public GerritSelectRepositoryPage() {
		util = Activator.getDefault().getRepositoryUtil();
		cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
	}

	@Override
	protected List<String> getInitialRepositories() {
		List<String> configuredRepos = util.getConfiguredRepositories();
		return configuredRepos.stream().map(name -> {
			File gitDir = new File(name);
			if (gitDir.exists()) {
				try {
					Repository repo = cache.lookupRepository(gitDir);
					if (ResourcePropertyTester.hasGerritConfiguration(repo)) {
						return name;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			return null;
		}).filter(r -> r != null).collect(Collectors.toList());
	}

}
