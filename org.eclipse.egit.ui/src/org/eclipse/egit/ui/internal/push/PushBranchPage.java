/*******************************************************************************
 * Copyright (c) 2013, 2014 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.IRemoteSelectionListener;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.components.UpstreamConfigComponent;
import org.eclipse.egit.ui.internal.components.UpstreamConfigComponent.UpstreamConfigSelectionListener;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Page that is part of the "Push Branch..." wizard, where the user selects the
 * remote, the branch name and the upstream config.
 */
public class PushBranchPage extends WizardPage {

	private final Repository repository;

	private final ObjectId commitToPush;

	private final Ref ref;

	private boolean showNewRemoteButton = true;

	private RemoteConfig remoteConfig;

	private List<RemoteConfig> remoteConfigs;

	private RemoteSelectionCombo remoteSelectionCombo;

	private Text remoteBranchNameText;

	private UpstreamConfig upstreamConfig = UpstreamConfig.NONE;

	private UpstreamConfigComponent upstreamConfigComponent;

	private boolean forceUpdateSelected = false;

	/** Only set if user selected "New Remote" */
	private AddRemotePage addRemotePage;

	private Set<Resource> disposables = new HashSet<Resource>();

	/**
	 * Create the page.
	 *
	 * @param repository
	 * @param commitToPush
	 * @param ref
	 *            An optional ref to give hints
	 */
	public PushBranchPage(Repository repository, ObjectId commitToPush, Ref ref) {
		super(UIText.PushBranchPage_PageName);
		setTitle(UIText.PushBranchPage_PageTitle);
		setMessage(UIText.PushBranchPage_PageMessage);

		this.repository = repository;
		this.commitToPush = commitToPush;
		this.ref = ref;
	}

	/**
	 * @param showNewRemoteButton
	 */
	public void setShowNewRemoteButton(boolean showNewRemoteButton) {
		this.showNewRemoteButton = showNewRemoteButton;
	}

	/**
	 * @return the page used to add a new remote, or null if an existing remote
	 *         was chosen
	 */
	AddRemotePage getAddRemotePage() {
		return addRemotePage;
	}

	RemoteConfig getRemoteConfig() {
		return remoteConfig;
	}

	/**
	 * @return the chosen short name of the branch on the remote
	 */
	String getRemoteBranchName() {
		return remoteBranchNameText.getText();
	}

	boolean isConfigureUpstreamSelected() {
		return upstreamConfig != UpstreamConfig.NONE;
	}

	boolean isRebaseSelected() {
		return upstreamConfig == UpstreamConfig.REBASE;
	}

	boolean isForceUpdateSelected() {
		return forceUpdateSelected;
	}

	public void createControl(Composite parent) {
		try {
			this.remoteConfigs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			this.remoteConfigs = new ArrayList<RemoteConfig>();
			handleError(e);
		}

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(GridLayoutFactory.swtDefaults().create());

		Composite inputPanel = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputPanel);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(inputPanel);

		Composite sourceComposite = new Composite(inputPanel, SWT.NONE);
		sourceComposite.setLayout(new RowLayout());
		Label sourceLabel = new Label(sourceComposite, SWT.NONE);
		sourceLabel.setText(UIText.PushBranchPage_Source);
		Label spacer = new Label(sourceComposite, SWT.NONE);
		spacer.setLayoutData(new RowData(10, SWT.DEFAULT));

		if (this.ref != null) {
			Image branchIcon = UIIcons.BRANCH.createImage();
			this.disposables.add(branchIcon);
			Label branchIconLabel = new Label(sourceComposite, SWT.NONE);
			branchIconLabel.setLayoutData(new RowData(branchIcon.getBounds().width, branchIcon.getBounds().height));
			branchIconLabel.setBackgroundImage(branchIcon);
			Label localBranchLabel = new Label(sourceComposite, SWT.NONE);
			localBranchLabel.setText(Repository.shortenRefName(this.ref
					.getName()));
		}

		Image commitIcon = UIIcons.COMMIT.createImage();
		this.disposables.add(commitIcon);
		Label commitIconLabel = new Label(sourceComposite, SWT.NONE);
		commitIconLabel.setBackgroundImage(commitIcon);
		commitIconLabel.setLayoutData(new RowData(commitIcon.getBounds().width,
				commitIcon.getBounds().height));
		Label commit = new Label(sourceComposite, SWT.NONE);
		RevWalk revWalk = new RevWalk(repository);
		StringBuilder commitBuilder = new StringBuilder(this.commitToPush
				.getName().substring(0, 8));
		StringBuilder commitTooltipBuilder = new StringBuilder(
				this.commitToPush.getName());
		try {
			RevCommit revCommit = revWalk.parseCommit(this.commitToPush);
			commitBuilder.append(' ');
			commitBuilder.append(revCommit.getShortMessage());
			commitTooltipBuilder.append(System.lineSeparator());
			commitTooltipBuilder.append(System.lineSeparator());
			commitTooltipBuilder.append(revCommit.getFullMessage());
		} catch (IOException ex) {
			commitBuilder
					.append(UIText.PushBranchPage_CannotAccessCommitDescription);
			commitTooltipBuilder.append(ex.getMessage());
			Activator.handleError(ex.getLocalizedMessage(), ex, false);
		}
		commit.setText(commitBuilder.toString());
		commit.setToolTipText(commitTooltipBuilder.toString());


		Group remoteGroup = new Group(inputPanel, SWT.NONE);
		remoteGroup.setText(UIText.PushBranchPage_Destination);
		remoteGroup.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().applyTo(remoteGroup);

		Label remoteLabel = new Label(remoteGroup, SWT.NONE);
		remoteLabel.setText(UIText.PushBranchPage_RemoteLabel);

		// Use full width in case "New Remote..." button is not shown
		int remoteSelectionSpan = showNewRemoteButton ? 1 : 2;

		remoteSelectionCombo = new RemoteSelectionCombo(remoteGroup, SWT.NONE,
				SelectionType.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).span(remoteSelectionSpan, 1)
				.applyTo(remoteSelectionCombo);
		setRemoteConfigs();
		remoteSelectionCombo
				.addRemoteSelectionListener(new IRemoteSelectionListener() {
					public void remoteSelected(RemoteConfig rc) {
						remoteConfig = rc;
						checkPage();
					}
				});

		if (showNewRemoteButton) {
			Button newRemoteButton = new Button(remoteGroup, SWT.PUSH);
			newRemoteButton.setText(UIText.PushBranchPage_NewRemoteButton);
			GridDataFactory.fillDefaults().applyTo(newRemoteButton);
			newRemoteButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					showNewRemoteDialog();
				}
			});
		}

		Label branchNameLabel = new Label(remoteGroup, SWT.NONE);
		branchNameLabel.setText(UIText.PushBranchPage_RemoteBranchNameLabel);

		remoteBranchNameText = new Text(remoteGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(remoteBranchNameText);
		remoteBranchNameText.setText(getSuggestedBranchName());

		upstreamConfigComponent = new UpstreamConfigComponent(inputPanel,
				SWT.NONE);
		upstreamConfigComponent.getContainer().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
						.indent(SWT.DEFAULT, 20).create());
		upstreamConfigComponent
				.addUpstreamConfigSelectionListener(new UpstreamConfigSelectionListener() {
					public void upstreamConfigSelected(
							UpstreamConfig newUpstreamConfig) {
						upstreamConfig = newUpstreamConfig;
						checkPage();
					}
				});

		final Button forceUpdateButton = new Button(inputPanel, SWT.CHECK);
		forceUpdateButton.setText(UIText.PushBranchPage_ForceUpdateButton);
		forceUpdateButton.setSelection(false);
		forceUpdateButton.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).span(3, 1).create());
		forceUpdateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				forceUpdateSelected = forceUpdateButton.getSelection();
			}
		});

		setDefaultUpstreamConfig();

		setControl(main);

		checkPage();

		// Add listener now to avoid setText above to already trigger it.
		remoteBranchNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	private void setRemoteConfigs() {
		remoteSelectionCombo.setItems(remoteConfigs);

		String branchName = Repository.shortenRefName(this.commitToPush.getName());
		String remoteName = repository.getConfig().getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REMOTE);
		if (remoteName != null) {
			for (RemoteConfig rc : remoteConfigs) {
				if (remoteName.equals(rc.getName()))
					remoteSelectionCombo.setSelectedRemote(rc);
			}
		}

		remoteConfig = remoteSelectionCombo.getSelectedRemote();
	}

	private void setDefaultUpstreamConfig() {
		boolean alreadyConfigured = false;
		String branchName = Constants.MASTER;
		if (ref != null) {
			branchName = Repository.shortenRefName(ref.getName());
			alreadyConfigured = repository.getConfig()
					.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION)
					.contains(branchName);
		}
		UpstreamConfig config;
		if (alreadyConfigured) {
			boolean rebase = repository.getConfig().getBoolean(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REBASE, false);
			config = rebase ? UpstreamConfig.REBASE : UpstreamConfig.MERGE;
		} else {
			config = UpstreamConfig.getDefault(repository, Constants.R_REMOTES
					+ Constants.DEFAULT_REMOTE_NAME + "/" + branchName); //$NON-NLS-1$
		}
		upstreamConfigComponent.setUpstreamConfig(config);
		upstreamConfig = config;
	}

	private void showNewRemoteDialog() {
		AddRemoteWizard wizard = new AddRemoteWizard(repository);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		int result = dialog.open();
		if (result == Window.OK) {
			URIish uri = wizard.getUri();
			String remoteName = wizard.getRemoteName();
			addRemotePage = wizard.getAddRemotePage();
			setSelectedRemote(remoteName, uri);
		}
	}

	private void checkPage() {
		try {
			if (remoteConfig == null) {
				setErrorMessage(UIText.PushBranchPage_ChooseRemoteError);
				return;
			}
			String branchName = remoteBranchNameText.getText();
			if (branchName.length() == 0) {
				setErrorMessage(UIText.PushBranchPage_ChooseBranchNameError);
				return;
			}
			if (!Repository.isValidRefName(Constants.R_HEADS + branchName)) {
				setErrorMessage(UIText.PushBranchPage_InvalidBranchNameError);
				return;
			}
			if (isConfigureUpstreamSelected()
					&& hasDifferentUpstreamConfiguration()) {
				setMessage(
						UIText.PushBranchPage_UpstreamConfigOverwriteWarning,
						IMessageProvider.WARNING);
			} else {
				setMessage(UIText.PushBranchPage_PageMessage);
			}
			setErrorMessage(null);
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	void setSelectedRemote(String remoteName, URIish urIish) {
		try {
			RemoteConfig config = new RemoteConfig(repository.getConfig(),
					remoteName);
			config.addURI(urIish);
			remoteSelectionCombo.setItems(Arrays.asList(config));
			this.remoteConfig = config;
			remoteSelectionCombo.setEnabled(false);
			checkPage();
		} catch (URISyntaxException e) {
			handleError(e);
		}
	}

	private String getSuggestedBranchName() {
		if (ref != null) {
			StoredConfig config = repository.getConfig();
			String branchName = Repository.shortenRefName(ref.getName());

			String merge = config.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_MERGE);
			if (merge != null && merge.startsWith(Constants.R_HEADS))
				return Repository.shortenRefName(merge);

			return branchName;
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private boolean hasDifferentUpstreamConfiguration() {
		StoredConfig config = repository.getConfig();
		String branchName = Repository.shortenRefName(ref.getName());

		String remote = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REMOTE);
		// No upstream config -> don't show warning
		if (remote == null)
			return false;
		if (!remote.equals(remoteConfig.getName()))
			return true;

		String merge = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_MERGE);
		if (merge == null || !merge.equals(Constants.R_HEADS + getRemoteBranchName()))
			return true;

		boolean rebase = config.getBoolean(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REBASE, false);
		if (rebase != isRebaseSelected())
			return true;

		return false;
	}

	private void handleError(URISyntaxException e) {
		Activator.handleError(e.getMessage(), e, false);
		setErrorMessage(e.getMessage());
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Resource disposable : this.disposables)
			disposable.dispose();
	}
}
