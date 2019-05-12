/*******************************************************************************
 * Copyright (c) 2016, 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.AsynchronousBranchList;
import org.eclipse.egit.ui.internal.components.AsynchronousRefProposalProvider;
import org.eclipse.egit.ui.internal.components.BranchRebaseModeCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.egit.ui.internal.push.AddRemoteWizard;
import org.eclipse.egit.ui.internal.push.PushBranchPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
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

	private Repository repository;

	private String fullBranch;

	private Button rememberConfigForBranch;

	private BranchRebaseModeCombo upstreamConfigComponent;

	private BranchRebaseMode upstreamConfig;

	private Ref head;

	private Text remoteBranchNameText;

	private ControlDecoration missingBranchDecorator;

	private boolean configureUpstream;

	private Map<String, AsynchronousBranchList> refs = new HashMap<>();

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
		} catch (IOException ex) {
			Activator.logError(ex.getMessage(), ex);
		}
	}

	@Override
	public void createControl(Composite parent) {
		parent.addDisposeListener(event -> {
			for (CancelableFuture<Collection<Ref>> l : refs.values()) {
				l.cancel(CancelableFuture.CancelMode.INTERRUPT);
			}
			refs.clear();
		});
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
			this.remoteConfigs = new ArrayList<>();
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
		.addRemoteSelectionListener((RemoteConfig rc) -> {
			remoteConfig = rc;
			setRefAssist(rc);
			checkPage();
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
		remoteBranchNameText.setText(getSuggestedBranchName());
		AsynchronousRefProposalProvider candidateProvider = new AsynchronousRefProposalProvider(
				getContainer(), remoteBranchNameText, () -> {
					RemoteConfig config = remoteSelectionCombo
							.getSelectedRemote();
					if (config == null) {
						return null;
					}
					List<URIish> uris = config.getURIs();
					if (uris == null || uris.isEmpty()) {
						return null;
					}
					return uris.get(0).toString();
				}, uri -> {
					AsynchronousBranchList list = refs.get(uri);
					if (list == null) {
						list = new AsynchronousBranchList(repository, uri,
								null);
						refs.put(uri, list);
					}
					return list;
				});
		candidateProvider.setContentProposalAdapter(
				UIUtils.addRefContentProposalToText(remoteBranchNameText,
						this.repository, candidateProvider, true));
		remoteBranchNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		this.upstreamConfigComponent = new BranchRebaseModeCombo(res);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(upstreamConfigComponent.getViewer().getCombo());
		this.upstreamConfigComponent.getViewer().addSelectionChangedListener(
				(event) -> upstreamConfig = upstreamConfigComponent
						.getRebaseMode());
		if (upstreamConfig != null) {
			upstreamConfigComponent.setRebaseMode(upstreamConfig);
		}
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
							checkPage();
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
			try {
				StoredConfig repoConfig = repository.getConfig();
				RemoteConfig newRemoteConfig = new RemoteConfig(repoConfig,
						remoteName);
				newRemoteConfig.addURI(uri);
				RefSpec defaultFetchSpec = new RefSpec().setForceUpdate(true)
						.setSourceDestination(Constants.R_HEADS + "*", //$NON-NLS-1$
								Constants.R_REMOTES + remoteName + "/*"); //$NON-NLS-1$
				newRemoteConfig.addFetchRefSpec(defaultFetchSpec);
				newRemoteConfig.update(repoConfig);
				repoConfig.save();
				List<RemoteConfig> allRemoteConfigs = RemoteConfig
						.getAllRemoteConfigs(repository.getConfig());
				remoteSelectionCombo.setItems(allRemoteConfigs);
				// find the new remote in the list, as the initial
				// newRemoteConfig object
				// isn't what's stored and returned by getAllRemoteConfigs
				for (RemoteConfig current : allRemoteConfigs) {
					if (newRemoteConfig.getName().equals(current.getName())) {
						setSelectedRemote(current);
					}
				}
			} catch (URISyntaxException ex) {
				Activator.logError(ex.getMessage(), ex);
			} catch (IOException ex) {
				Activator.logError(ex.getMessage(), ex);
			}
		}
	}

	private void setRefAssist(RemoteConfig config) {
		if (config != null && config.getURIs().size() > 0) {
			String uriText = config.getURIs().get(0).toString();
			AsynchronousBranchList list = refs.get(uriText);
			if (list == null) {
				list = new AsynchronousBranchList(repository, uriText, null);
				refs.put(uriText, list);
				preFetch(list);
			}
		}
	}

	private void preFetch(AsynchronousBranchList list) {
		try {
			list.start();
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getLocalizedMessage(), e.getCause(), true);
		}
	}

	void setSelectedRemote(RemoteConfig config) {
		remoteSelectionCombo.setSelectedRemote(config);
		this.remoteConfig = config;
		setRefAssist(this.remoteConfig);
		checkPage();
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
			String branchNameMessage = null;
			if (branchName.length() == 0) {
				branchNameMessage = MessageFormat.format(
						UIText.PullWizardPage_ChooseReference,
						remoteConfig.getName());
			}
			if (branchNameMessage != null) {
				setErrorMessage(branchNameMessage);
				if (this.missingBranchDecorator == null) {
					this.missingBranchDecorator = new ControlDecoration(this.remoteBranchNameText, SWT.TOP | SWT.LEFT);
					this.missingBranchDecorator
							.setImage(FieldDecorationRegistry.getDefault()
									.getFieldDecoration(
											FieldDecorationRegistry.DEC_ERROR)
									.getImage());
				}
				this.missingBranchDecorator
						.setDescriptionText(branchNameMessage);
				this.missingBranchDecorator.show();
				return;
			} else if (this.missingBranchDecorator != null) {
				this.missingBranchDecorator.hide();
			}

			if (overrideUpstreamConfiguration()
					&& hasDifferentUpstreamConfiguration()) {
				setMessage(UIText.PushBranchPage_UpstreamConfigOverwriteWarning,
						IMessageProvider.WARNING);
			} else {
				setMessage(UIText.PullWizardPage_PageMessage);
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
		if (fullBranch != null) {
			String branchName = Repository.shortenRefName(fullBranch);
			StoredConfig config = repository.getConfig();
			BranchConfig branchConfig = new BranchConfig(config, branchName);
			String merge = branchConfig.getMerge();
			if (!branchConfig.isRemoteLocal() && merge != null
					&& merge.startsWith(Constants.R_HEADS)) {
				return Repository.shortenRefName(merge);
			}
		}
		return ""; //$NON-NLS-1$
	}

	boolean overrideUpstreamConfiguration() {
		return this.configureUpstream;
	}

	BranchRebaseMode getUpstreamConfig() {
		return this.upstreamConfig;
	}

	private boolean hasDifferentUpstreamConfiguration() {
		if (fullBranch == null) {
			return false;
		}
		String branchName = Repository.shortenRefName(this.fullBranch);
		BranchConfig branchConfig = new BranchConfig(repository.getConfig(),
				branchName);

		String remote = branchConfig.getRemote();
		// No upstream config -> don't show warning
		if (remote == null) {
			return false;
		}
		if (!remote.equals(remoteConfig.getName())) {
			return true;
		}
		String merge = branchConfig.getMerge();
		if (merge == null || !merge.equals(getFullRemoteReference())) {
			return true;
		}
		if (branchConfig.getRebaseMode() != getUpstreamConfig()) {
			return true;
		}
		return false;
	}

	private void setDefaultUpstreamConfig() {
		if (fullBranch == null) {
			return;
		}
		String branchName = Repository.shortenRefName(this.fullBranch);
		BranchConfig branchConfig = new BranchConfig(repository.getConfig(),
				branchName);
		boolean alreadyConfigured = branchConfig.getMerge() != null;
		BranchRebaseMode config;
		if (alreadyConfigured) {
			config = PullCommand.getRebaseMode(branchName,
					repository.getConfig());
		} else {
			config = CreateLocalBranchOperation
					.getDefaultUpstreamConfig(repository, Constants.R_REMOTES
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

	@Override
	public void dispose() {
		super.dispose();
		if (this.missingBranchDecorator != null) {
			this.missingBranchDecorator.dispose();
		}
	}
}
