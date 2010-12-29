package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * A simplified wizard for configuring fetch
 */
public class SimpleConfigureFetchWizard extends Wizard {
	private final Repository repository;

	private FetchUriPage fetchUriPage;

	/**
	 * @param repository
	 * @param configToUse
	 * @return the wizard to open, or null
	 */
	public static SimpleConfigureFetchWizard getWizard(Repository repository,
			RemoteConfig configToUse) {
		if (configToUse == null) {
			try {
				return new SimpleConfigureFetchWizard(repository,
						new RemoteConfig(repository.getConfig(),
								Constants.DEFAULT_REMOTE_NAME));
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		} else if (configToUse.getURIs().isEmpty()) {
			return new SimpleConfigureFetchWizard(repository, configToUse);
		}
		return null;
	}

	/**
	 * @param repository
	 * @return the configured remote for the current branch if any, or null
	 */
	public static RemoteConfig getConfiguredRemote(Repository repository) {
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}

		String remoteName;
		if (ObjectId.isId(branch)) {
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		} else {
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);
		}

		// check if we find the configured and default Remotes
		List<RemoteConfig> allRemotes;
		try {
			allRemotes = RemoteConfig.getAllRemoteConfigs(repository
					.getConfig());
		} catch (URISyntaxException e) {
			allRemotes = new ArrayList<RemoteConfig>();
		}

		RemoteConfig defaultConfig = null;
		RemoteConfig configuredConfig = null;
		for (RemoteConfig config : allRemotes) {
			if (config.getName().equals(Constants.DEFAULT_REMOTE_NAME))
				defaultConfig = config;
			if (remoteName != null && config.getName().equals(remoteName))
				configuredConfig = config;
		}

		RemoteConfig configToUse = configuredConfig != null ? configuredConfig
				: defaultConfig;
		return configToUse;
	}

	/**
	 * @param repository
	 * @param config
	 */
	private SimpleConfigureFetchWizard(Repository repository,
			RemoteConfig config) {
		setHelpAvailable(false);
		this.repository = repository;
		fetchUriPage = new FetchUriPage(repository, config);
		addPage(fetchUriPage);
		setNeedsProgressMonitor(true);
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		boolean detachedHead = false;
		String branchName;
		try {
			String branch = repository.getBranch();
			if (ObjectId.isId(branch)) {
				detachedHead = true;
			}
			branchName = branch;
		} catch (IOException e) {
			setWindowTitle(NLS
					.bind(
							UIText.SimpleConfigureFetchWizard_ConfigureFetchNoBranchTitle,
							repoName));
			return;
		}
		if (detachedHead) {
			setWindowTitle(NLS
					.bind(
							UIText.SimpleConfigureFetchWizard_ConfigureFetchNoBranchTitle,
							repoName));
		} else {
			setWindowTitle(NLS
					.bind(
							UIText.SimpleConfigureFetchWizard_ConfigureFetchWithBranchTitle,
							repoName, branchName));
		}
	}

	@Override
	public boolean performFinish() {
		fetchUriPage.getRemoteConfig().update(repository.getConfig());
		try {
			repository.getConfig().save();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return false;
		}
		if (fetchUriPage.shouldFetchUponFinish())
			try {
				getContainer().run(false, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						new FetchConfiguredRemoteAction(
								repository,
								fetchUriPage.getRemoteConfig(),
								Activator
										.getDefault()
										.getPreferenceStore()
										.getInt(
												UIPreferences.REMOTE_CONNECTION_TIMEOUT))
								.start();
					}
				});
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getMessage(), e, true);
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		return true;
	}
}
