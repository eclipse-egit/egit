package org.eclipse.egit.ui.internal.push;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.IRemoteSelectionListener;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Page for pushing a branch for the first time and setting up the upstream
 * config.
 */
public class PushBranchPage extends WizardPage {

	private final Repository repository;

	private final Ref ref;

	private boolean showNewRemoteButton = true;

	private RemoteConfig remoteConfig;

	private List<RemoteConfig> remoteConfigs;

	private Text branchNameText;

	private UpstreamConfig upstreamConfig;

	private Button rebaseConfigRadio;

	private Button mergeConfigRadio;

	private RemoteSelectionCombo remoteSelectionCombo;

	private RepositorySelection repositorySelection;

	private Button configureUpstreamCheck;

	/**
	 * Create the page.
	 *
	 * @param repository
	 * @param ref
	 */
	public PushBranchPage(Repository repository, Ref ref) {
		super("Push Branch"); //$NON-NLS-1$
		setTitle("Push Branch to Remote"); //$NON-NLS-1$
		setMessage("Select a remote and set the name the branch should have on the remote."); //$NON-NLS-1$

		this.repository = repository;
		this.ref = ref;

		this.upstreamConfig = UpstreamConfig.getDefault(repository, Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + Repository.shortenRefName(ref.getName())); //$NON-NLS-1$
	}

	/**
	 * @param showNewRemoteButton
	 */
	public void setShowNewRemoteButton(boolean showNewRemoteButton) {
		this.showNewRemoteButton = showNewRemoteButton;
	}

	/**
	 * @return repository selection
	 */
	public RepositorySelection getRepositorySelection() {
		if (repositorySelection != null)
			return repositorySelection;
		else
			return new RepositorySelection(null, remoteConfig);
	}

	/**
	 * @return the chosen name of the branch on the remote
	 */
	public String getBranchName() {
		return branchNameText.getText();
	}

	public void createControl(Composite parent) {
		try {
			this.remoteConfigs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			throw new RuntimeException("TODO", e); //$NON-NLS-1$
		}

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(GridLayoutFactory.swtDefaults().create());

		Composite inputPanel = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputPanel);
		inputPanel.setLayout(GridLayoutFactory.fillDefaults().numColumns(3)
				.create());

		Label remoteLabel = new Label(inputPanel, SWT.NONE);
		remoteLabel.setText("R&emote: "); //$NON-NLS-1$

		// Use full width in case "New Remote..." button is not shown
		int remoteSelectionSpan = showNewRemoteButton ? 1 : 2;

		remoteSelectionCombo = new RemoteSelectionCombo(
				inputPanel, SWT.NONE, SelectionType.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).span(remoteSelectionSpan, 1)
				.applyTo(remoteSelectionCombo);
		remoteConfig = remoteSelectionCombo.setItems(remoteConfigs);
		remoteSelectionCombo
				.addRemoteSelectionListener(new IRemoteSelectionListener() {
					public void remoteSelected(RemoteConfig rc) {
						remoteConfig = rc;
						checkPage();
					}
				});

		if (showNewRemoteButton) {
			Button newRemoteButton = new Button(inputPanel, SWT.PUSH);
			newRemoteButton.setText("&New Remote..."); //$NON-NLS-1$
			GridDataFactory.fillDefaults().applyTo(newRemoteButton);
			newRemoteButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					showNewRemoteDialog();
				}
			});
		}

		Label branchNameLabel = new Label(inputPanel, SWT.NONE);
		branchNameLabel.setText("&Branch name: "); //$NON-NLS-1$

		branchNameText = new Text(inputPanel, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(branchNameText);
		branchNameText.setText(getSuggestedBranchName());

		configureUpstreamCheck = new Button(inputPanel, SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(configureUpstreamCheck);
		configureUpstreamCheck
				.setText("Configure &upstream branch"); //$NON-NLS-1$
		configureUpstreamCheck.setToolTipText("TODO"); //$NON-NLS-1$
		configureUpstreamCheck.setSelection(true);

		final Group upstreamConfigGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(upstreamConfigGroup);
		upstreamConfigGroup.setText("Pull strategy"); //$NON-NLS-1$
		upstreamConfigGroup.setLayout(new GridLayout(1, false));

		rebaseConfigRadio = new Button(upstreamConfigGroup, SWT.RADIO);
		rebaseConfigRadio.setText("&Rebase"); //$NON-NLS-1$
		mergeConfigRadio = new Button(upstreamConfigGroup, SWT.RADIO);
		mergeConfigRadio.setText("&Merge"); //$NON-NLS-1$

		configureUpstreamCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = configureUpstreamCheck.getSelection();
				upstreamConfigGroup.setEnabled(enabled);
				rebaseConfigRadio.setEnabled(enabled);
				mergeConfigRadio.setEnabled(enabled);
				checkPage();
			}
		});

		setControl(main);

		checkPage();

		// Add listener now to avoid setText above to already trigger it.
		branchNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	private void showNewRemoteDialog() {
		AddRemoteWizard wizard = new AddRemoteWizard(repository);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		int result = dialog.open();
		if (result == Window.OK) {
			URIish uri = wizard.getUri();
			String remoteName = wizard.getRemoteName();
			try {
				RemoteConfig config = new RemoteConfig(repository.getConfig(), remoteName);
				RefSpec refSpec = new RefSpec("+" + Constants.R_HEADS + "*:" + Constants.R_REMOTES + remoteName + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				config.addFetchRefSpec(refSpec);
				config.addURI(uri);
				setRemoteConfig(config);
				repositorySelection = wizard.getRepositorySelection();
				// TODO: Remember secure storage settings for later
			} catch (URISyntaxException e) {
				// TODO
			}
		}
	}

	private void checkPage() {
		try {
			rebaseConfigRadio.setSelection(upstreamConfig == UpstreamConfig.REBASE);
			mergeConfigRadio.setSelection(upstreamConfig != UpstreamConfig.REBASE);

			if (remoteConfig == null) {
				setErrorMessage("Please choose/add a remote."); //$NON-NLS-1$
				return;
			}
			String branchName = branchNameText.getText();
			if (branchName.length() == 0) {
				setErrorMessage("Please choose a name for the new branch."); //$NON-NLS-1$
				return;
			}
			if (!Repository.isValidRefName(Constants.R_HEADS + branchName)) {
				setErrorMessage("Invalid branch name."); //$NON-NLS-1$
				return;
			}
			if (branchAlreadyHasUpstreamConfiguration() && configureUpstreamCheck.getSelection()) {
				setMessage("The existing upstream configuration for the branch will be overwritten.", IMessageProvider.WARNING); //$NON-NLS-1$
			} else {
				setMessage(null, IMessageProvider.WARNING);
			}
			setErrorMessage(null);
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	/**
	 * @param remoteConfig
	 */
	protected void setRemoteConfig(RemoteConfig remoteConfig) {
		remoteSelectionCombo.setItems(Arrays.asList(remoteConfig));
		this.remoteConfig = remoteConfig;
		remoteSelectionCombo.setEnabled(false);
		checkPage();
	}

	private String getSuggestedBranchName() {
		return Repository.shortenRefName(ref.getName());
	}

	private boolean branchAlreadyHasUpstreamConfiguration() {
		StoredConfig config = repository.getConfig();
		BranchConfig branchConfig = new BranchConfig(config, Repository.shortenRefName(ref.getName()));
		String trackingBranch = branchConfig.getTrackingBranch();
		return trackingBranch != null;
	}
}
