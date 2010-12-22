package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * A simplified push wizard
 */
public class SimplePushWizard extends Wizard {
	private final Repository repository;

	private BranchToRemotePage branchToRemotePage;

	private PushUriPage pushUriPage;

	/**
	 * @param repository
	 * @param configToUse
	 * @return the wizard to open, or null
	 */
	public static SimplePushWizard getWizard(Repository repository,
			RemoteConfig configToUse) {
		if (configToUse == null) {
			return new SimplePushWizard(repository, null);
		}

		if (configToUse.getPushURIs().isEmpty()
				&& configToUse.getURIs().isEmpty()) {
			return new SimplePushWizard(repository, configToUse);
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
	private SimplePushWizard(Repository repository, RemoteConfig config) {
		this.repository = repository;
		branchToRemotePage = new BranchToRemotePage(repository);
		addPage(branchToRemotePage);
		pushUriPage = new PushUriPage(repository);
		addPage(pushUriPage);
		setNeedsProgressMonitor(true);
		setWindowTitle("Push"); //$NON-NLS-1$ TODO
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == branchToRemotePage) {
			pushUriPage.setRemoteName(branchToRemotePage.getRemoteName());
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		pushUriPage.getRemoteConfig().update(repository.getConfig());
		try {
			repository.getConfig().save();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return false;
		}
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					PushConfiguredRemoteOperation op = new PushConfiguredRemoteOperation(
							repository, pushUriPage.getRemoteConfig(), 1000);
					op.execute(monitor);
					PushOperationResult result = op.getOperationResult();
					PushResultDialog dlg = new PushResultDialog(getShell(),
							repository, result, pushUriPage.getRemoteConfig()
									.getName());
					dlg.showConfigureButton(false);
					dlg.open();
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
