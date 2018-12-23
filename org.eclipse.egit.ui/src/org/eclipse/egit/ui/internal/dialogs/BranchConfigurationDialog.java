/*******************************************************************************
 * Copyright (C) 2012, 2016 Mathias Kinzler <mathias.kinzler@sap.com> and others
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler - Initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 499482
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.BranchRebaseModeCombo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Allows to configure the upstream configuration of a Branch
 */
public class BranchConfigurationDialog extends TitleAreaDialog {
	private final String myBranchName;

	private final Repository myRepository;

	private final StoredConfig myConfig;

	private final String myTitle;

	private Combo remoteText;

	private Combo branchText;

	private BranchRebaseModeCombo rebase;

	/**
	 * @param shell
	 * @param branchName
	 * @param repository
	 */
	public BranchConfigurationDialog(Shell shell, String branchName,
			Repository repository) {
		super(shell);
		myBranchName = branchName;
		myRepository = repository;
		myConfig = myRepository.getConfig();
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		myTitle = UIText.BranchConfigurationDialog_BranchConfigurationTitle;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label remoteLabel = new Label(main, SWT.NONE);
		remoteLabel.setText(UIText.BranchConfigurationDialog_RemoteLabel);
		remoteText = new Combo(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteText);

		Label branchLabel = new Label(main, SWT.NONE);
		branchLabel.setText(UIText.BranchConfigurationDialog_UpstreamBranchLabel);
		branchText = new Combo(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);

		remoteText.add(BranchConfig.LOCAL_REPOSITORY);
		for (String remote : myConfig
				.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION))
			remoteText.add(remote);

		remoteText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateBranchItems();
			}
		});


		rebase = new BranchRebaseModeCombo(main);
		BranchRebaseMode rebaseMode = PullCommand.getRebaseMode(myBranchName,
				myConfig);
		rebase.setRebaseMode(rebaseMode);
		GridDataFactory.fillDefaults().grab(true, false)
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(rebase.getViewer().getCombo());

		String branch = myConfig.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, myBranchName,
				ConfigConstants.CONFIG_KEY_MERGE);
		if (branch == null)
			branch = ""; //$NON-NLS-1$
		branchText.setText(branch);

		String remote = myConfig.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, myBranchName,
				ConfigConstants.CONFIG_KEY_REMOTE);
		if (remote == null)
			remote = ""; //$NON-NLS-1$
		remoteText.setText(remote);
		updateBranchItems();

		applyDialogFont(main);
		return main;
	}

	private void updateBranchItems() {
		String branchTextBefore = branchText.getText();
		branchText.removeAll();
		addBranchItems();
		branchText.setText(branchTextBefore);
	}

	private void addBranchItems() {
		String remote = remoteText.getText();
		try {
			if (remote.equals(BranchConfig.LOCAL_REPOSITORY)
					|| remote.length() == 0)
				// Add local branches only. Fetching from "." and then merging a
				// remote ref does not make much sense, so don't offer it. If
				// the user wants that, it can be entered manually.
				addBranchItemsForLocal();
			else
				addBranchItemsForRemote(remote);
		} catch (IOException e) {
			Activator.logError(
					UIText.BranchConfigurationDialog_ExceptionGettingRefs, e);
		} catch (URISyntaxException e) {
			Activator.logError(
					UIText.BranchConfigurationDialog_ExceptionGettingRefs, e);
		}
	}

	private void addBranchItemsForLocal() throws IOException {
		List<Ref> localRefs = new ArrayList<>(myRepository.getRefDatabase()
				.getRefsByPrefix(Constants.R_HEADS));
		Collections.sort(localRefs, CommonUtils.REF_ASCENDING_COMPARATOR);
		for (Ref ref : localRefs)
			branchText.add(ref.getName());
	}

	private void addBranchItemsForRemote(String remote) throws IOException,
			URISyntaxException {
		RemoteConfig remoteConfig = new RemoteConfig(myConfig, remote);
		List<RefSpec> fetchSpecs = remoteConfig.getFetchRefSpecs();
		if (fetchSpecs.isEmpty()) {
			return;
		}

		List<Ref> allRefs = new ArrayList<>(myRepository.getRefDatabase()
				.getRefsByPrefix(Constants.R_REFS));
		Collections.sort(allRefs, CommonUtils.REF_ASCENDING_COMPARATOR);
		for (Ref ref : allRefs) {
			for (RefSpec fetchSpec : fetchSpecs) {
				// Fetch specs map remote ref names (source) to local ref names
				// (destination). We want to get remote ref names, so expand
				// destination to source.
				if (fetchSpec.matchDestination(ref)) {
					RefSpec source = fetchSpec.expandFromDestination(ref);
					String refNameOnRemote = source.getSource();
					branchText.add(refNameOnRemote);
				}
			}
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.BranchConfigurationDialog_BranchConfigurationTitle);
	}

	@Override
	public void create() {
		super.create();
		setTitle(myTitle);
		setMessage(NLS.bind(
				UIText.BranchConfigurationDialog_EditBranchConfigMessage, myBranchName));
	}

	@Override
	protected void okPressed() {
		try {
			String merge = branchText.getText();
			if (merge.length() > 0) {
				myConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_MERGE, merge);
			} else {
				myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_MERGE);
			}
			String remote = remoteText.getText();
			if (remote.length() > 0) {
				myConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_REMOTE,
						remote);
			} else {
				myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_REMOTE);
			}
			BranchRebaseMode rebaseMode = rebase.getRebaseMode();
			if (rebaseMode == null) {
				myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_REBASE);
			} else {
				myConfig.setEnum(ConfigConstants.CONFIG_BRANCH_SECTION,
						myBranchName, ConfigConstants.CONFIG_KEY_REBASE,
						rebaseMode);
			}
			try {
				myConfig.save();
				super.okPressed();
			} catch (IOException e) {
				Activator.handleError(
						UIText.BranchConfigurationDialog_SaveBranchConfigFailed, e, true);
			}
		} catch (RuntimeException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.BranchConfigurationDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
