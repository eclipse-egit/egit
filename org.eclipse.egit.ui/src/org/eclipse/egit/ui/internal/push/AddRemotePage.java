/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.Set;

import org.eclipse.egit.ui.internal.UIText;
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
		remoteNameLabel.setText(UIText.AddRemotePage_RemoteNameLabel);

		remoteNameText = new Text(remoteNamePanel, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(remoteNameText);
		if (!getExistingRemotes().contains(Constants.DEFAULT_REMOTE_NAME)) {
			remoteNameText.setText(Constants.DEFAULT_REMOTE_NAME);
			remoteNameText.setSelection(remoteNameText.getText().length());
		} else
			setMessage(UIText.AddRemotePage_EnterRemoteNameMessage);

		remoteNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	@Override
	protected void checkPage() {
		String remoteName = getRemoteName();
		if (remoteName.length() == 0) {
			setErrorMessage(UIText.AddRemotePage_RemoteNameEmptyError);
			setPageComplete(false);
		} else if (!isValidRemoteName(remoteName)) {
			setErrorMessage(UIText.AddRemotePage_RemoteNameInvalidError);
			setPageComplete(false);
		} else if (getExistingRemotes().contains(remoteName)) {
			setErrorMessage(UIText.AddRemotePage_RemoteNameAlreadyExistsError);
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
