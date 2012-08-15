package org.eclipse.egit.ui.internal.push;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * A wizard dedicated to pushing branches for the first time.
 */
public class PushBranchWizard extends Wizard {

	private final Repository repository;
	private final Ref refToPush;

	private AddRemotePage addRemotePage;
	private PushBranchPage pushBranchPage;
	private ConfirmationPage confirmationPage;


	/**
	 * @param repository
	 *            the repository the ref belongs to
	 * @param refToPush
	 */
	public PushBranchWizard(final Repository repository, Ref refToPush) {
		this.repository = repository;
		this.refToPush = refToPush;

		Set<String> remoteNames = repository.getConfig().getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		if (remoteNames.isEmpty())
			addRemotePage = new AddRemotePage(repository);

		pushBranchPage = new PushBranchPage(repository, refToPush) {
			@Override
			public void setVisible(boolean visible) {
				if (visible && addRemotePage != null) {
					setRemoteConfig(getNewRemoteConfig());
				}
				super.setVisible(visible);
			}
		};
		// Don't show button if we're configuring a remote in the first step
		pushBranchPage.setShowNewRemoteButton(addRemotePage == null);

		confirmationPage = new ConfirmationPage(repository) {
			@Override
			public void setVisible(boolean visible) {
				setSelection(getRepositorySelection(), getRefSpecs());
				super.setVisible(visible);
			}
		};
	}

	@Override
	public void addPages() {
		if (addRemotePage != null)
			addPage(addRemotePage);
		addPage(pushBranchPage);
		addPage(confirmationPage);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	private RemoteConfig getNewRemoteConfig() {
		String remoteName = addRemotePage.getRemoteName();
		try {
			RemoteConfig remoteConfig = new RemoteConfig(repository.getConfig(), remoteName);
			remoteConfig.addURI(addRemotePage.getSelection().getURI());
			// TODO: This is the same as when using "New Remote..." in step 2, unify
			return remoteConfig;
		} catch (URISyntaxException e) {
			throw new RuntimeException("TODO", e); //$NON-NLS-1$
		}
	}

	private RepositorySelection getRepositorySelection() {
		if (addRemotePage != null)
			return addRemotePage.getSelection();
		else
			return pushBranchPage.getRepositorySelection();
	}

	private List<RefSpec> getRefSpecs() {
		String src = refToPush.getName();
		String dst = Constants.R_HEADS + pushBranchPage.getBranchName();
		RefSpec refSpec = new RefSpec(src + ":" + dst);  //$NON-NLS-1$
		return Arrays.asList(refSpec);
	}
}
