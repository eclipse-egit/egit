package org.eclipse.egit.ui.internal.commands.shared;

import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.egit.ui.internal.fetch.FetchChangeFromServerWizard;
import org.eclipse.egit.ui.internal.fetch.GitServer;
import org.eclipse.egit.ui.internal.gerrit.FilteredSelectRepositoryPage;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * An {@link IExecutableExtensionFactory} for creating commands for fetching
 * changes from git servers.
 */
public class FetchChangeFromServerCommandFactory
		implements IExecutableExtensionFactory, IExecutableExtension {

	private GitServer server;

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		server = GitServer.valueOf(data.toString());
	}

	@Override
	public Object create() throws CoreException {
		return new AbstractFetchFromHostCommand() {

			@Override
			protected GitSelectRepositoryPage createSelectionPage() {
				return new FilteredSelectRepositoryPage(MessageFormat.format(
						UIText.GitSelectRepositoryPage_PageTitleServer,
						server.getName()), UIIcons.WIZBAN_FETCH) {

					@Override
					protected boolean includeRepository(Repository repo) {
						try {
							return GitHosts.hasServerConfig(
									SelectionRepositoryStateCache.INSTANCE
											.getConfig(repo),
									server.getType());
						} catch (URISyntaxException e) {
							return false;
						}
					}
				};
			}

			@Override
			protected Wizard createFetchWizard(Repository repository,
					String clipText) {
				return new FetchChangeFromServerWizard(server, repository,
						clipText);
			}
		};
	}

}
