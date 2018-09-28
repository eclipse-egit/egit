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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;

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
		util = Activator.getDefault().getRepositoryUtil();
		cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
		setTitle(UIText.GerritSelectRepositoryPage_PageTitle);
		setDescription(null);
	}

	@Override
	protected List<String> getInitialRepositories() {
		List<String> configuredRepos = util.getConfiguredRepositories();
		return configuredRepos.stream().map(name -> {
			File gitDir = new File(name);
			if (gitDir.exists()) {
				try {
					Repository repo = cache.lookupRepository(gitDir);
					if (repo == null) {
						return null;
					}
					if (ResourcePropertyTester.hasGerritConfiguration(repo)) {
						return name;
					}
				} catch (IOException e) {
					// TODO: Need to add some kind of message?
					Activator.logError(e.getMessage(), e);
					return null;
				}
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	protected void createAddRepositoryButton(Composite tb) {
		return;
	}

}
