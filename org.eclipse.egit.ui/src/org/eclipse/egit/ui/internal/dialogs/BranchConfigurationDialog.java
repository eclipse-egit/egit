/*******************************************************************************
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler - Initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
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

	private Combo branchText;

	private Combo remoteText;

	private Button rebase;

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
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, false).indent(5, 5)
				.applyTo(main);
		Label branchLabel = new Label(main, SWT.NONE);
		branchLabel.setText(UIText.BranchConfigurationDialog_UpstreamBranchLabel);
		branchText = new Combo(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);

		try {
			for (Ref ref : myRepository.getRefDatabase()
					.getRefs(Constants.R_HEADS).values())
				branchText.add(ref.getName());
			for (Ref ref : myRepository.getRefDatabase()
					.getRefs(Constants.R_REMOTES).values())
				branchText.add(ref.getName());
		} catch (IOException e) {
			Activator.logError(UIText.BranchConfigurationDialog_ExceptionGettingRefs, e);
		}

		Label remoteLabel = new Label(main, SWT.NONE);
		remoteLabel.setText(UIText.BranchConfigurationDialog_RemoteLabel);
		remoteText = new Combo(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteText);

		// TODO do we have a constant somewhere?
		remoteText.add("."); //$NON-NLS-1$
		for (String remote : myConfig
				.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION))
			remoteText.add(remote);

		rebase = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(rebase);
		rebase.setText(UIText.BranchConfigurationDialog_RebaseLabel);

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

		boolean rebaseFlag = myConfig.getBoolean(
				ConfigConstants.CONFIG_BRANCH_SECTION, myBranchName,
				ConfigConstants.CONFIG_KEY_REBASE, false);
		rebase.setSelection(rebaseFlag);

		applyDialogFont(main);
		// return result;
		return main;
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
				if (merge.length() > 0)
					myConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_MERGE,
							merge);
				else
					myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_MERGE);

				String remote = remoteText.getText();
				if (remote.length() > 0)
					myConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_REMOTE,
							remote);
				else
					myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_REMOTE);

				boolean rebaseFlag = rebase.getSelection();
				if (rebaseFlag)
					myConfig.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_REBASE,
							true);
				else
					myConfig.unset(ConfigConstants.CONFIG_BRANCH_SECTION,
							myBranchName, ConfigConstants.CONFIG_KEY_REBASE);
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
}
