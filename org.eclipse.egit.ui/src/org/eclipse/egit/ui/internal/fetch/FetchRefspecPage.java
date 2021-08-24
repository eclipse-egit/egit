/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.net.URISyntaxException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A wizard page combining a repository selection part and a refspec page for
 * Fetch
 */
public class FetchRefspecPage extends RefSpecPage {

	private RemoteSelectionCombo remoteSelectionCombo;

	/**
	 * @param repo
	 */
	public FetchRefspecPage(Repository repo) {
		super(repo, false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(1, true));
		createRepoSelection(res);
		super.createControl(res);
		getControl()
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); // getControl()
																				// is
																				// the
																				// result
																				// of
																				// super.setControl
																				// at
																				// this
																				// time
		setSelection(new RepositorySelection(null, getRemoteConfig()));
		setControl(res);
	}

	private void createRepoSelection(Composite res) {
		Composite container = new Composite(res, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(
				new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		new Label(container, SWT.NONE).setText(UIText.PushBranchPage_RemoteLabel);
		remoteSelectionCombo = new RemoteSelectionCombo(
				container, SWT.READ_ONLY, SelectionType.FETCH);
		remoteSelectionCombo.setLayoutData(
				new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		try {
			remoteSelectionCombo.setItems(
					RemoteConfig.getAllRemoteConfigs(local.getConfig()), local);
		} catch (URISyntaxException e) {
			MessageDialog.openError(getShell(),
					UIText.FetchRefspecPage_couldParseRemote_title,
					UIText.FetchRefspecPage_couldParseRemote_message);
			Activator.logError(UIText.FetchRefspecPage_couldParseRemote_title,
					e);
		}
		remoteSelectionCombo.addRemoteSelectionListener(
				remoteConfig -> setSelection(
						new RepositorySelection(null, remoteConfig)));
	}

	@Override
	public void setSelection(RepositorySelection selection) {
		super.setSelection(selection);
		remoteSelectionCombo.setSelectedRemote(selection.getConfig());
	}

	/**
	 * @return the selected repository
	 */
	public RemoteConfig getRemoteConfig() {
		return remoteSelectionCombo.getSelectedRemote();
	}

	@Override
	public boolean isPageComplete() {
		return getRemoteConfig() != null && super.isPageComplete();
	}

}
