/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Allows to create a branch based on another branch or with a selection of
 * another branch
 */
public class CreateBranchPage extends WizardPage {

	private final Repository myRepository;

	private final Ref myBaseBranch;

	private final boolean remoteMode;

	private Text nameText;

	private Button checkout;

	private Combo branchCombo;

	/**
	 * Create a branch
	 *
	 * @param repo
	 *            the repository
	 * @param baseBranch
	 *            the branch to base the new branch on, may be null
	 * @param remote
	 *            true if remote branch is to be created
	 */
	public CreateBranchPage(Repository repo, Ref baseBranch, boolean remote) {
		super(CreateBranchPage.class.getName());
		this.myRepository = repo;
		this.myBaseBranch = baseBranch;
		this.remoteMode = remote;
		if (this.remoteMode)
			if (baseBranch != null)
				setTitle(NLS.bind(
						UIText.CreateBranchPage_CreateRemoteBaseOnTitle,
						myBaseBranch.getName()));
			else
				setTitle(UIText.CreateBranchPage_CreateRemoteTitle);
		else if (baseBranch != null)
			setTitle(NLS.bind(UIText.CreateBranchPage_CreateLocalBasedTitle,
					myBaseBranch.getName()));
		else
			setTitle(UIText.CreateBranchPage_CreateLocalTitle);
	}

	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));

		Label sourceLabel = new Label(main, SWT.NONE);
		sourceLabel.setText(UIText.CreateBranchPage_SourceBranchLabel);
		sourceLabel.setToolTipText(UIText.CreateBranchPage_SourceBranchTooltip);
		this.branchCombo = new Combo(main, SWT.READ_ONLY | SWT.DROP_DOWN);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(
				this.branchCombo);

		try {
			for (Entry<String, Ref> ref : myRepository.getRefDatabase()
					.getRefs(Constants.R_HEADS).entrySet()) {
				if (!ref.getValue().isSymbolic())
					this.branchCombo.add(ref.getValue().getName());
			}
			for (Entry<String, Ref> ref : myRepository.getRefDatabase()
					.getRefs(Constants.R_REMOTES).entrySet()) {
				if (!ref.getValue().isSymbolic())
					this.branchCombo.add(ref.getValue().getName());
			}

		} catch (IOException e1) {
			// ignore here
		}

		this.branchCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}

		});
		if (myBaseBranch != null) {
			this.branchCombo.setText(myBaseBranch.getName());
			this.branchCombo.setEnabled(false);
		} else {
			String fullBranch;
			try {
				fullBranch = myRepository.getFullBranch();
				this.branchCombo.setText(fullBranch);
			} catch (IOException e1) {
				// ignore
			}
		}

		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.CreateBranchPage_BranchNameLabel);
		if (remoteMode)
			nameLabel.setToolTipText(NLS.bind(
					UIText.CreateBranchPage_BranchNameTooltip,
					Constants.R_REMOTES));
		else
			nameLabel.setToolTipText(NLS.bind(
					UIText.CreateBranchPage_BranchNameTooltip,
					Constants.R_HEADS));

		nameText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		boolean isBare = myRepository.getConfig().getBoolean(
				"core", "bare", false); //$NON-NLS-1$ //$NON-NLS-2$
		checkout = new Button(main, SWT.CHECK);
		checkout.setText(UIText.CreateBranchPage_CheckoutButton);
		// most of the time, we probably will check this out
		// unless we have a bare repository which doesn't allow
		// check out at all
		checkout.setSelection(!isBare);
		checkout.setEnabled(!isBare);
		checkout.setVisible(!isBare);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(
				checkout);
		checkout.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}

		});

		setControl(main);
		nameText.setFocus();

		// in any case, we will have to enter the name
		setPageComplete(false);
		if (this.myBaseBranch != null)
			setMessage(UIText.CreateBranchPage_ChosseNameMessage);
		else
			setMessage(UIText.CreateBranchPage_ChooseBranchAndNameMessage);
	}

	private void checkPage() {
		setErrorMessage(null);

		try {

			if (branchCombo.getText().length() == 0) {
				setErrorMessage(UIText.CreateBranchPage_MissingSourceMessage);
				return;
			}
			if (nameText.getText().length() == 0) {
				setErrorMessage(UIText.CreateBranchPage_MissingNameMessage);
				return;
			}

			String fullName = getBranchName();
			try {
				if (myRepository.getRef(fullName) != null)
					setErrorMessage(NLS.bind(
							UIText.CreateBranchPage_BranchAlreadyExistsMessage,
							fullName));
				return;
			} catch (IOException e) {
				// ignore here
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private String getBranchName() {
		if (remoteMode)
			return Constants.R_REMOTES + nameText.getText();
		else
			return Constants.R_HEADS + nameText.getText();
	}

	private String getSourceBranchName() {
		if (myBaseBranch != null)
			return myBaseBranch.getName();
		else if (this.branchCombo != null)
			return this.branchCombo.getText();
		else
			return null;
	}

	/**
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	public void createBranch(IProgressMonitor monitor) throws CoreException,
			IOException {

		String newRefName = getBranchName();
		RefUpdate updateRef;

		monitor.beginTask(UIText.CreateBranchPage_CreatingBranchMessage,
				IProgressMonitor.UNKNOWN);
		updateRef = myRepository.updateRef(newRefName);

		Ref sourceBranch;
		if (myBaseBranch != null) {
			sourceBranch = myBaseBranch;
		} else {
			sourceBranch = myRepository.getRef(getSourceBranchName());
		}
		ObjectId startAt = sourceBranch.getObjectId();
		String startBranch = myRepository
				.shortenRefName(sourceBranch.getName());
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
		if (checkout.getSelection()) {
			if (monitor.isCanceled())
				return;
			monitor.beginTask(UIText.CreateBranchPage_CheckingOutMessage,
					IProgressMonitor.UNKNOWN);
			new BranchOperation(myRepository, getBranchName()).execute(monitor);
		}
	}
}
