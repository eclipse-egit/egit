/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IRefListProvider;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefContentAssistProvider;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.IRemoteSelectionListener;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.push.AddRemoteWizard;
import org.eclipse.egit.ui.internal.push.PushBranchPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This Wizard Page allows to configure a Push operation (remote, reference,
 * rebase/merge)
 *
 * It is heavily inspired/copy-pasted from the {@link PushBranchPage} and a lot
 * of code could be factorized.
 */
public class PullWizardPage extends WizardPage {

	private RemoteSelectionCombo remoteSelectionCombo;

	private List<RemoteConfig> remoteConfigs;
	private RemoteConfig remoteConfig;

	private RefContentAssistProvider assist;

	private Repository repository;

	private String fullBranch;

	private Button mergeRadio;

	private Button rebaseRadio;

	private Button rememberConfigForBranch;

	private UpstreamConfig upstreamConfig;

	private Ref head;

	private Text remoteBranchNameText;

	private boolean configureUpstream;

	/**
	 * Create the page.
	 *
	 * @param repository
	 */
	public PullWizardPage(Repository repository) {
		super(UIText.PullWizardPage_PageName);
		setTitle(UIText.PullWizardPage_PageTitle);
		setMessage(UIText.PullWizardPage_PageMessage);
		setImageDescriptor(UIIcons.WIZBAN_PULL);
		this.repository = repository;
		try {
			this.head = repository.findRef(Constants.HEAD);
			this.fullBranch = repository.getFullBranch();
		} catch (Exception ex) {
			// TODO
		}
	}

	@Override
	public void createControl(Composite parent) {
		try {
			this.remoteConfigs = RemoteConfig
					.getAllRemoteConfigs(repository.getConfig());
			Collections.sort(remoteConfigs, new Comparator<RemoteConfig>() {
				@Override
				public int compare(RemoteConfig first, RemoteConfig second) {
					return String.CASE_INSENSITIVE_ORDER
							.compare(first.getName(), second.getName());
				}
			});
			setDefaultUpstreamConfig();
		} catch (URISyntaxException e) {
			this.remoteConfigs = new ArrayList<RemoteConfig>();
			handleError(e);
		}

		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(3, false));

		Label remoteLabel = new Label(res, SWT.NONE);
		remoteLabel.setText(UIText.PushBranchPage_RemoteLabel);

		this.remoteSelectionCombo = new RemoteSelectionCombo(
				res, SWT.NONE, SelectionType.PUSH);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(remoteSelectionCombo);
		setRemoteConfigs();
		remoteSelectionCombo
				.addRemoteSelectionListener(new IRemoteSelectionListener() {
					@Override
					public void remoteSelected(RemoteConfig rc) {
						remoteConfig = rc;
						setRefAssist(rc);
						checkPage();
					}
				});

		Button newRemoteButton = new Button(res, SWT.PUSH);
		newRemoteButton.setText(UIText.PushBranchPage_NewRemoteButton);
		GridDataFactory.fillDefaults().applyTo(newRemoteButton);
		newRemoteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showNewRemoteDialog();
			}
		});

		Label branchNameLabel = new Label(res, SWT.NONE);
		branchNameLabel.setText(UIText.PullWizardPage_referenceLabel);
		branchNameLabel.setToolTipText(UIText.PullWizardPage_referenceTooltip);

		remoteBranchNameText = new Text(res, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(remoteBranchNameText);
		UIUtils.addRefContentProposalToText(remoteBranchNameText,
				this.repository, new IRefListProvider() {

					@Override
					public List<Ref> getRefList() {
						if (PullWizardPage.this.assist != null) {
							return PullWizardPage.this.assist
									.getRefsForContentAssist(false, true);
						}
						return Collections.emptyList();
					}
				});
		remoteBranchNameText.setText(getSuggestedBranchName());
		remoteBranchNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		this.mergeRadio = new Button(res, SWT.RADIO);
		this.mergeRadio.setText(UIText.UpstreamConfigComponent_MergeRadio);
		this.mergeRadio.setLayoutData(
				new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1));
		this.mergeRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				upstreamConfig = UpstreamConfig.MERGE;
			}
		});
		this.rebaseRadio = new Button(res, SWT.RADIO);
		this.rebaseRadio.setText(UIText.UpstreamConfigComponent_RebaseRadio);
		this.rebaseRadio.setLayoutData(
				new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1));
		this.mergeRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				upstreamConfig = UpstreamConfig.REBASE;
			}
		});
		this.mergeRadio
				.setSelection(this.upstreamConfig == UpstreamConfig.MERGE);
		this.rebaseRadio
				.setSelection(this.upstreamConfig == UpstreamConfig.REBASE);
		if (this.fullBranch != null
				&& this.fullBranch.startsWith(Constants.R_HEADS)) {
			this.rememberConfigForBranch = new Button(res, SWT.CHECK);
			GridData checkboxLayoutData = new GridData(SWT.BEGINNING,
					SWT.CENTER, false, false, 3, 1);
			checkboxLayoutData.verticalIndent = 20;
			this.rememberConfigForBranch.setText(
					UIText.UpstreamConfigComponent_ConfigureUpstreamCheck);
			this.rememberConfigForBranch.setToolTipText(
					UIText.UpstreamConfigComponent_ConfigureUpstreamToolTip);
			this.rememberConfigForBranch.setLayoutData(checkboxLayoutData);
			this.rememberConfigForBranch.setSelection(this.configureUpstream);
			this.rememberConfigForBranch
					.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							configureUpstream = rememberConfigForBranch
									.getSelection();
						}
					});
		}

		setPageComplete(isPageComplete());
		setControl(res);
	}

	private void setRemoteConfigs() {
		remoteSelectionCombo.setItems(remoteConfigs);
		if (this.head != null) {
			String branchName = Repository.shortenRefName(this.head.getName());
			BranchConfig branchConfig = new BranchConfig(repository.getConfig(),
					branchName);
			String remoteName = branchConfig.getRemote();
			if (remoteName != null) {
				for (RemoteConfig rc : remoteConfigs) {
					if (remoteName.equals(rc.getName()))
						remoteSelectionCombo.setSelectedRemote(rc);
				}
			}
		}

		remoteConfig = remoteSelectionCombo.getSelectedRemote();
		setRefAssist(remoteConfig);
	}

	private void showNewRemoteDialog() {
		AddRemoteWizard wizard = new AddRemoteWizard(repository);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		int result = dialog.open();
		if (result == Window.OK) {
			URIish uri = wizard.getUri();
			String remoteName = wizard.getRemoteName();
			setSelectedRemote(remoteName, uri);
		}
	}

	private void setRefAssist(RemoteConfig config) {
		if (config != null && config.getURIs().size() > 0) {
			this.assist = new RefContentAssistProvider(
					PullWizardPage.this.repository, config.getURIs().get(0),
					getShell());
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
			setRefAssist(this.remoteConfig);
			checkPage();
		} catch (URISyntaxException e) {
			handleError(e);
		}
	}

	@Override
	public boolean isPageComplete() {
		return remoteConfig != null && remoteBranchNameText.getText() != null
				&& !remoteBranchNameText.getText().isEmpty();
	}

	private void checkPage() {
		try {
			if (remoteConfig == null) {
				setErrorMessage(UIText.PushBranchPage_ChooseRemoteError);
				return;
			}
			String branchName = remoteBranchNameText.getText();
			if (branchName.length() == 0) {
				setErrorMessage(MessageFormat.format(
						UIText.PushBranchPage_ChooseBranchNameError,
						remoteConfig.getName()));
				return;
			}
			if (!Repository.isValidRefName(Constants.R_HEADS + branchName)) {
				setErrorMessage(UIText.PushBranchPage_InvalidBranchNameError);
				return;
			}
			if (isConfigureUpstreamSelected()
					&& hasDifferentUpstreamConfiguration()) {
				setMessage(UIText.PushBranchPage_UpstreamConfigOverwriteWarning,
						IMessageProvider.WARNING);
			} else {
				setMessage(UIText.PushBranchPage_PageMessage);
			}
			setErrorMessage(null);
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private void handleError(URISyntaxException e) {
		Activator.handleError(e.getMessage(), e, false);
		setErrorMessage(e.getMessage());
	}

	private String getSuggestedBranchName() {
		try {
			if (fullBranch != null) {
				String branchName = Repository.shortenRefName(fullBranch);
				StoredConfig config = repository.getConfig();
				BranchConfig branchConfig = new BranchConfig(config,
						branchName);
				String merge = branchConfig.getMerge();
				if (!branchConfig.isRemoteLocal() && merge != null
						&& merge.startsWith(Constants.R_HEADS)) {
					return Repository.shortenRefName(merge);
				} else if (merge == null) {
					String fullBranch = repository.getFullBranch();
					if (fullBranch != null
							&& fullBranch.startsWith(Constants.R_HEADS)) {
						return branchName;
					}
				}
			}
		} catch (Exception ex) {
			// TODO
		}
		return ""; //$NON-NLS-1$
	}

	boolean isConfigureUpstreamSelected() {
		return this.configureUpstream;
	}

	boolean isRebaseSelected() {
		return upstreamConfig == UpstreamConfig.REBASE;
	}

	UpstreamConfig getUpstreamConfig() {
		return this.upstreamConfig;
	}

	private boolean hasDifferentUpstreamConfiguration() {
		String branchName = Repository.shortenRefName(this.fullBranch);
		BranchConfig branchConfig = new BranchConfig(repository.getConfig(),
				branchName);

		String remote = branchConfig.getRemote();
		// No upstream config -> don't show warning
		if (remote == null)
			return false;
		if (!remote.equals(remoteConfig.getName()))
			return true;

		String merge = branchConfig.getMerge();
		if (merge == null || !merge.equals(getFullRemoteReference()))
			return true;

		boolean rebase = branchConfig.isRebase();
		if (rebase != isRebaseSelected())
			return true;

		return false;
	}

	private void setDefaultUpstreamConfig() {
		String branchName = Repository.shortenRefName(this.fullBranch);
		BranchConfig branchConfig = new BranchConfig(repository.getConfig(),
				branchName);
		boolean alreadyConfigured = branchConfig.getMerge() != null;
		UpstreamConfig config;
		if (alreadyConfigured) {
			boolean rebase = branchConfig.isRebase();
			config = rebase ? UpstreamConfig.REBASE : UpstreamConfig.MERGE;
		} else {
			config = UpstreamConfig.getDefault(repository, Constants.R_REMOTES
					+ Constants.DEFAULT_REMOTE_NAME + "/" + branchName); //$NON-NLS-1$
		}
		this.upstreamConfig = config;
	}

	/**
	 * @return the chosen short name of the branch on the remote
	 */
	String getFullRemoteReference() {
		if (!remoteBranchNameText.getText().startsWith(Constants.R_REFS))
			return Constants.R_HEADS + remoteBranchNameText.getText();
		else
			return remoteBranchNameText.getText();
	}

	RemoteConfig getRemoteConfig() {
		return this.remoteConfig;
	}
}
