/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;

import java.io.File;
import java.util.List;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting repositories to synchronization
 */
public class SelectSynchronizeResourceDialog extends TitleAreaDialog {

	private String dstRef;

	private String srcRef;

	private boolean shouldIncluldeLocal;

	private final String repoName;

	private final List<SyncRepoEntity> syncRepos;

	private RemoteSelectionCombo dstRefCombo;

	private RemoteSelectionCombo srcRefCombo;

	private Button shouldIncludeLocalButton;

	/**
	 * Construct dialog that gives user possibility to choose with which
	 * repository and branch he want to synchronize
	 *
	 * @param parent
	 * @param repoDirectory
	 * @param syncRepos
	 */
	public SelectSynchronizeResourceDialog(Shell parent, File repoDirectory,
			List<SyncRepoEntity> syncRepos) {
		super(parent);
		this.repoName = repoDirectory.getParentFile().getName()
				+ File.separator + repoDirectory.getName();
		this.syncRepos = syncRepos;
		setHelpAvailable(false);
	}

	/**
	 * @return remote ref name
	 */
	public String getValue() {
		return dstRef;
	}

	/**
	 * @return destination ref
	 */
	public String getDstRef() {
		return dstRef;
	}

	/**
	 * @return source ref
	 */
	public String getSrcRef() {
		return srcRef;
	}

	/**
	 * @return <code>true</code> if local uncommited changes should be included
	 *         in comparison
	 */
	public boolean shouldIncludeLocal() {
		return shouldIncluldeLocal;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(GridLayoutFactory.swtDefaults().create());

		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH / 2);

		new Label(composite, SWT.WRAP)
				.setText(UIText.SelectSynchronizeResourceDialog_srcRef);

		srcRefCombo = new RemoteSelectionCombo(composite, syncRepos,
				UIText.RemoteSelectionCombo_sourceName,
				UIText.RemoteSelectionCombo_sourceRef);
		srcRefCombo.setDefaultValue(UIText.SynchronizeWithAction_localRepoName, HEAD);
		srcRefCombo.setLayoutData(data);
		srcRefCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).create());

		shouldIncludeLocalButton = new Button(composite, SWT.CHECK | SWT.WRAP);
		shouldIncludeLocalButton
				.setText(UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges);
		shouldIncludeLocalButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean includeLocal = shouldIncludeLocalButton.getSelection();
				srcRefCombo.setEnabled(!includeLocal);
				if (includeLocal)
					srcRefCombo.setDefaultValue(
							UIText.SynchronizeWithAction_localRepoName, HEAD);
			}
		});

		new Label(composite, SWT.WRAP)
				.setText(UIText.SelectSynchronizeResourceDialog_dstRef);

		dstRefCombo = new RemoteSelectionCombo(composite, syncRepos,
				UIText.RemoteSelectionCombo_destinationName,
				UIText.RemoteSelectionCombo_destinationRef);
		dstRefCombo.setDefaultValue(getDefaultRemoteName(), MASTER);
		dstRefCombo.setLayoutData(data);
		dstRefCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).create());

		setTitle(NLS.bind(UIText.SelectSynchronizeResourceDialog_selectProject,
				repoName));
		setMessage(UIText.SelectSynchronizeResourceDialog_header);
		setTitleImage(UIIcons.WIZBAN_CONNECT_REPO.createImage());

		return composite;
	}

	private String getDefaultRemoteName() {
		boolean onlyOneRemote = syncRepos.size() == 2;
		if (onlyOneRemote)
			return syncRepos.get(1).getName();
		else {
			for (SyncRepoEntity repo : syncRepos)
				if (repo.getName().equals(DEFAULT_REMOTE_NAME))
					return DEFAULT_REMOTE_NAME;
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			dstRef = dstRefCombo.getValue();
			srcRef = srcRefCombo.getValue();
			shouldIncluldeLocal = shouldIncludeLocalButton.getSelection();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell
				.setText(NLS.bind(
						UIText.SelectSynchronizeResourceDialog_selectProject,
						repoName));

		newShell.setMinimumSize(600, 180);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

}
