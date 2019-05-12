/*******************************************************************************
 * Copyright (c) 2013, 2019 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.AsynchronousBranchList;
import org.eclipse.egit.ui.internal.components.AsynchronousRefProposalProvider;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.components.UpstreamConfigComponent;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Page that is part of the "Push Branch..." wizard, where the user selects the
 * remote, the branch name and the upstream config.
 */
public class PushBranchPage extends WizardPage {

	private static final int MAX_SHORTCOMMIT_MESSAGE_LENGTH = 65;

	private final Repository repository;

	private final ObjectId commitToPush;

	private final Ref ref;

	private boolean showNewRemoteButton = true;

	private RemoteConfig remoteConfig;

	private List<RemoteConfig> remoteConfigs;

	private RemoteSelectionCombo remoteSelectionCombo;

	private Text remoteBranchNameText;

	private BranchRebaseMode upstreamConfig;

	private UpstreamConfigComponent upstreamConfigComponent;

	private boolean forceUpdateSelected = false;

	/** Only set if user selected "New Remote" */
	private AddRemotePage addRemotePage;

	private Set<Resource> disposables = new HashSet<>();

	private Map<String, AsynchronousBranchList> refs = new HashMap<>();

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
	String getFullRemoteReference() {
		if (!remoteBranchNameText.getText().startsWith(Constants.R_REFS))
			return Constants.R_HEADS + remoteBranchNameText.getText();
		else
			return remoteBranchNameText.getText();
	}

	BranchRebaseMode getUpstreamConfig() {
		return upstreamConfig;
	}

	boolean isForceUpdateSelected() {
		return forceUpdateSelected;
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
			this.remoteConfigs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
			Collections.sort(remoteConfigs, new Comparator<RemoteConfig>() {

				@Override
				public int compare(RemoteConfig first, RemoteConfig second) {
					return String.CASE_INSENSITIVE_ORDER.compare(
							first.getName(), second.getName());
				}
			});
		} catch (URISyntaxException e) {
			this.remoteConfigs = new ArrayList<>();
			handleError(e);
		}

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(GridLayoutFactory.swtDefaults().create());

		Composite inputPanel = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputPanel);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(inputPanel);

		Label sourceLabel = new Label(inputPanel, SWT.NONE);
		sourceLabel.setText(UIText.PushBranchPage_Source);

		Composite sourceComposite = new Composite(inputPanel, SWT.NONE);
		sourceComposite.setLayoutData(GridDataFactory.fillDefaults()
				.indent(UIUtils.getControlIndent(), 0).create());
		RowLayout rowLayout = RowLayoutFactory.fillDefaults().create();
		rowLayout.center = true;
		sourceComposite.setLayout(rowLayout);

		if (this.ref != null) {
			Image branchIcon = UIIcons.BRANCH.createImage();
			this.disposables.add(branchIcon);

			Label branchIconLabel = new Label(sourceComposite, SWT.NONE);
			branchIconLabel
					.setLayoutData(new RowData(branchIcon.getBounds().width,
							branchIcon.getBounds().height));
			branchIconLabel.setImage(branchIcon);
			Label localBranchLabel = new Label(sourceComposite, SWT.NONE);
			localBranchLabel.setText(Repository.shortenRefName(this.ref
					.getName()));

			Label spacer = new Label(sourceComposite, SWT.NONE);
			spacer.setLayoutData(new RowData(3, SWT.DEFAULT));
		}

		Image commitIcon = UIIcons.CHANGESET.createImage();
		this.disposables.add(commitIcon);
		Label commitIconLabel = new Label(sourceComposite, SWT.NONE);
		commitIconLabel.setImage(commitIcon);
		commitIconLabel.setLayoutData(new RowData(commitIcon.getBounds().width,
				commitIcon.getBounds().height));

		Label commit = new Label(sourceComposite, SWT.NONE);
		StringBuilder commitBuilder = new StringBuilder(
				Utils.getShortObjectId(commitToPush));
		StringBuilder commitTooltipBuilder = new StringBuilder(
				this.commitToPush.getName());
		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit revCommit = revWalk.parseCommit(this.commitToPush);
			commitBuilder.append("  "); //$NON-NLS-1$
			commitBuilder.append(Utils.shortenText(revCommit.getShortMessage(),
					MAX_SHORTCOMMIT_MESSAGE_LENGTH));
			commitTooltipBuilder.append("\n\n"); //$NON-NLS-1$
			commitTooltipBuilder.append(revCommit.getFullMessage());
		} catch (IOException ex) {
			commitBuilder
					.append(UIText.PushBranchPage_CannotAccessCommitDescription);
			commitTooltipBuilder.append(ex.getMessage());
			Activator.handleError(ex.getLocalizedMessage(), ex, false);
		}
		commit.setText(commitBuilder.toString());
		commit.setToolTipText(commitTooltipBuilder.toString());

		Label destinationLabel = new Label(inputPanel, SWT.NONE);
		destinationLabel.setText(UIText.PushBranchPage_Destination);
		GridDataFactory.fillDefaults().applyTo(destinationLabel);

		Composite remoteGroup = new Composite(inputPanel, SWT.NONE);
		remoteGroup.setLayoutData(GridDataFactory.fillDefaults()
				.indent(UIUtils.getControlIndent(), 0).create());
		remoteGroup.setLayout(GridLayoutFactory.fillDefaults().numColumns(3)
				.create());

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
		.addRemoteSelectionListener((RemoteConfig rc) -> {
			remoteConfig = rc;
			setRefAssist(rc);
			checkPage();
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
								getLocalBranchName());
						refs.put(uri, list);
					}
					return list;
				});
		candidateProvider.setContentProposalAdapter(
				UIUtils.addRefContentProposalToText(remoteBranchNameText,
						this.repository, candidateProvider, true));
		if (this.ref != null) {
			upstreamConfigComponent = new UpstreamConfigComponent(inputPanel,
					SWT.NONE);
			upstreamConfigComponent.getContainer().setLayoutData(
					GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
							.indent(SWT.NONE, 20).create());
			upstreamConfigComponent
			.addUpstreamConfigSelectionListener((BranchRebaseMode newUpstreamConfig) -> {
				upstreamConfig = newUpstreamConfig;
				checkPage();
			});
			setDefaultUpstreamConfig();
		}

		final Button forceUpdateButton = new Button(inputPanel, SWT.CHECK);
		forceUpdateButton.setText(UIText.PushBranchPage_ForceUpdateButton);
		forceUpdateButton.setSelection(false);
		forceUpdateButton.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).span(3, 1).create());
		forceUpdateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				forceUpdateSelected = forceUpdateButton.getSelection();
			}
		});

		Link advancedDialogLink = new Link(main, SWT.NONE);
		advancedDialogLink.setText(UIText.PushBranchPage_advancedWizardLink);
		advancedDialogLink
				.setToolTipText(UIText.PushBranchPage_advancedWizardLinkTooltip);
		advancedDialogLink.setLayoutData(new GridData(SWT.END, SWT.END, false,
				true));
		advancedDialogLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell parentShell = getShell().getParent().getShell();
				PushWizard advancedWizard = null;
				try {
					advancedWizard = new PushWizard(repository);
					getShell().close();
					new WizardDialog(parentShell, advancedWizard).open();
				} catch (URISyntaxException ex) {
					Activator.logError(ex.getMessage(), ex);
				}

			}
		});

		setControl(main);

		checkPage();

		// Add listener now to avoid setText above to already trigger it.
		remoteBranchNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
		// Do not use a tooltip since there is already a content proposal
		// adapter on this field
		BranchNameNormalizer normalizer = new BranchNameNormalizer(
				remoteBranchNameText, null);
		normalizer.setVisible(false);
	}

	private void setRemoteConfigs() {
		remoteSelectionCombo.setItems(remoteConfigs);
		if (this.ref != null) {
			String branchName = Repository.shortenRefName(this.ref.getName());
			BranchConfig branchConfig = new BranchConfig(
					repository.getConfig(), branchName);
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

	private void setDefaultUpstreamConfig() {
		if (this.ref != null) {
			String branchName = Repository.shortenRefName(ref.getName());
			BranchConfig branchConfig = new BranchConfig(
					repository.getConfig(), branchName);
			boolean alreadyConfigured = branchConfig.getMerge() != null;
			BranchRebaseMode config;
			if (alreadyConfigured) {
				config = PullCommand.getRebaseMode(branchName,
						repository.getConfig());
			} else {
				config = CreateLocalBranchOperation.getDefaultUpstreamConfig(
						repository, Constants.R_REMOTES
						+ Constants.DEFAULT_REMOTE_NAME + "/" + branchName); //$NON-NLS-1$
			}
			this.upstreamConfig = config;
			this.upstreamConfigComponent.setUpstreamConfig(this.upstreamConfig);
		}
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
				setErrorMessage(MessageFormat.format(
						UIText.PushBranchPage_ChooseBranchNameError,
						remoteConfig.getName()));
				return;
			}
			if (!Repository.isValidRefName(Constants.R_HEADS + branchName)) {
				setErrorMessage(UIText.PushBranchPage_InvalidBranchNameError);
				return;
			}
			if (getUpstreamConfig() != null
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
			setRefAssist(this.remoteConfig);
			checkPage();
		} catch (URISyntaxException e) {
			handleError(e);
		}
	}

	private String getLocalBranchName() {
		if (ref != null && !ref.getName().startsWith(Constants.R_REMOTES)) {
			return Repository.shortenRefName(ref.getName());
		}
		return null;
	}

	private String getSuggestedBranchName() {
		if (ref != null && !ref.getName().startsWith(Constants.R_REMOTES)) {
			StoredConfig config = repository.getConfig();
			String branchName = Repository.shortenRefName(ref.getName());

			BranchConfig branchConfig = new BranchConfig(config, branchName);
			String merge = branchConfig.getMerge();
			if (!branchConfig.isRemoteLocal() && merge != null
					&& merge.startsWith(Constants.R_HEADS))
				return Repository.shortenRefName(merge);

			return branchName;
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private void setRefAssist(RemoteConfig config) {
		if (config != null && config.getURIs().size() > 0) {
			String uriText = config.getURIs().get(0).toString();
			AsynchronousBranchList list = refs.get(uriText);
			if (list == null) {
				list = new AsynchronousBranchList(repository, uriText,
						getLocalBranchName());
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

	private boolean hasDifferentUpstreamConfiguration() {
		String branchName = Repository.shortenRefName(ref.getName());
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
		if (branchConfig.getRebaseMode() != upstreamConfig) {
			return true;
		}
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
