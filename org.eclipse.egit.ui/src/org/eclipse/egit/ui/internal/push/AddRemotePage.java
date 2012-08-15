package org.eclipse.egit.ui.internal.push;

import java.util.Set;

import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard page for adding a new remote (and setting its name).
 */
public class AddRemotePage extends RepositorySelectionPage {

	private final Repository repository;

	private Text remoteNameText;

	/**
	 * @param repository
	 */
	public AddRemotePage(Repository repository) {
		super(false, null);
		this.repository = repository;
	}

	/**
	 * @return the remote name entered by the user
	 */
	public String getRemoteName() {
		return remoteNameText.getText();
	}

	@Override
	protected void createRemoteNamePanel(Composite panel) {
		Composite remoteNamePanel = new Composite(panel, SWT.NONE);
		remoteNamePanel.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteNamePanel);

		Label remoteNameLabel = new Label(remoteNamePanel, SWT.NONE);
		remoteNameLabel.setText("&Remote name: "); //$NON-NLS-1$

		remoteNameText = new Text(remoteNamePanel, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(remoteNameText);
		if (!getExistingRemotes().contains(Constants.DEFAULT_REMOTE_NAME)) {
			remoteNameText.setText(Constants.DEFAULT_REMOTE_NAME);
			remoteNameText.setSelection(remoteNameText.getText().length());
		} else
			setMessage("Please enter a remote name"); //$NON-NLS-1$

		remoteNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	@Override
	protected void checkPage() {
		String remoteName = getRemoteName();
		if (remoteName.length() == 0) {
			setErrorMessage("Enter a name for the remote"); //TODO //$NON-NLS-1$
			setPageComplete(false);
		} else if (!isValidRemoteName(remoteName)) {
			setErrorMessage("Remote name is not valid"); //TODO //$NON-NLS-1$
			setPageComplete(false);
		} else if (getExistingRemotes().contains(remoteName)) {
			setErrorMessage("Remote already exists"); //TODO //$NON-NLS-1$
			setPageComplete(false);
		} else {
			super.checkPage();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			remoteNameText.setFocus();
	}

	private Set<String> getExistingRemotes() {
		return repository.getConfig().getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
	}

	private static boolean isValidRemoteName(String remoteName) {
		String testRef = Constants.R_REMOTES + remoteName + "/test"; //$NON-NLS-1$
		return Repository.isValidRefName(testRef);
	}
}
