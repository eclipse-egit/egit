package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Displays the branch-to-remote mapping if any
 *
 */
public class BranchToRemotePage extends WizardPage {

	private final Repository repository;

	private String remoteName;

	/**
	 * @param repository
	 */
	protected BranchToRemotePage(Repository repository) {
		super(BranchToRemotePage.class.getName());
		this.repository = repository;
		setTitle("Review Remote Name"); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return;
		}
		if (ObjectId.isId(branch)) {
			setMessage(NLS
					.bind(
							"Repository is currently not on any branch; you can still use the \"{0}\" remote for pushing", Constants.DEFAULT_REMOTE_NAME)); //$NON-NLS-1$ TODO
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		} else {
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);

			if (remoteName == ".") { //$NON-NLS-1$
				setMessage(NLS
						.bind(
								"Upstream configuration of branch \"{0}\" is using \".\" (the current Repository)", branch)); //$NON-NLS-1$ TODO
			}

			if (remoteName != null) {
				setMessage(NLS
						.bind(
								"Upstream configuration for branch \"{0}\" uses the \"{1}\" remote", branch, remoteName)); //$NON-NLS-1$ TODO
			} else {
				if (existsRemote(Constants.DEFAULT_REMOTE_NAME)) {
					setMessage(NLS
							.bind(
									"No upstream configuration for branch \"{0}\"; you can still use the \"{1}\" remote", branch, Constants.DEFAULT_REMOTE_NAME)); //$NON-NLS-1$ TODO
				} else {
					setMessage(NLS
							.bind(
									"No upstream configuration for branch \"{0}\"; a default remote \"{1}\" will be created", branch, Constants.DEFAULT_REMOTE_NAME)); //$NON-NLS-1$ TODO
				}

				remoteName = Constants.DEFAULT_REMOTE_NAME;
			}
		}
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		Label repoLabel = new Label(main, SWT.NONE);
		repoLabel.setText("Repository: "); //$NON-NLS-1$
		Text repoText = new Text(main, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(repoText);
		repoText.setText(repository.getDirectory().getPath());

		if (!ObjectId.isId(branch)) {
			Label branchLabel = new Label(main, SWT.NONE);
			branchLabel.setText("Branch:"); //$NON-NLS-1$
			Text branchText = new Text(main, SWT.BORDER | SWT.READ_ONLY);
			branchText.setText(branch);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchText);
		}

		Label remoteLabel = new Label(main, SWT.NONE);
		remoteLabel.setText("Remote Name: "); //$NON-NLS-1$
		Text remoteText = new Text(main, SWT.BORDER | SWT.READ_ONLY);
		remoteText.setText(remoteName);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteText);

		setControl(main);
	}

	private boolean existsRemote(String name) {
		List<RemoteConfig> rcs;
		try {
			rcs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
			for (RemoteConfig config : rcs) {
				if (config.getName().equals(name))
					return true;
			}
		} catch (URISyntaxException e) {
			// ignore here
		}
		return false;
	}

	/**
	 * @return the remote name
	 */
	public String getRemoteName() {
		return remoteName;
	}

}
