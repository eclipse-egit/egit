/*******************************************************************************
 * Copyright (C) 2012 Markus Duft <markus.duft@salomon.at> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.UIUtils;
import org.eclipse.egit.ui.internal.UIUtils.IRefListProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A page that allows to select a target branch for a push operation.
 */
public class SimplePushSpecPage extends WizardPage {

	private boolean forceUpdate;

	private Text remoteRefName;

	private String sourceName;

	private Repository repository;

	/**
	 * the content assist provider for the {@link Ref}s
	 */
	protected RefContentAssistProvider assist;

	/**
	 * Creates a new wizard page that allows selection of the target
	 *
	 * @param niceSourceName
	 *            the nice displayable name of the source to be pushed.
	 * @param repo
	 *            source repository
	 */
	public SimplePushSpecPage(String niceSourceName, Repository repo) {
		super(UIText.SimplePushSpecPage_title);
		setTitle(UIText.SimplePushSpecPage_title);
		setMessage(NLS.bind(UIText.SimplePushSpecPage_message, niceSourceName));

		this.sourceName = niceSourceName;
		this.repository = repo;
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));

		Composite inputPanel = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputPanel);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		inputPanel.setLayout(layout);

		final Label lblRemote = new Label(inputPanel, SWT.NONE);
		lblRemote.setText(UIText.SimplePushSpecPage_TargetRefName);
		remoteRefName = new Text(inputPanel, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteRefName);
		remoteRefName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(isPageComplete());
			}
		});

		UIUtils.addRefContentProposalToText(remoteRefName, repository,
				new IRefListProvider() {
					public List<Ref> getRefList() {
						if (assist != null)
							return assist.getRefsForContentAssist(false, true);

						return Collections.emptyList();
					}
				});

		final Button forceButton = new Button(main, SWT.CHECK);
		forceButton.setText(UIText.RefSpecDialog_ForceUpdateCheckbox);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(forceButton);

		forceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				forceUpdate = forceButton.getSelection();
			}
		});

		setControl(main);
	}

	/*
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 * @since 2.0
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			remoteRefName.setFocus();
	}

	/**
	 * pre-fills the destination box with a remote ref name if one exists that
	 * matches the local branch name.
	 */
	protected void updateDestinationField() {
		setMessage(NLS.bind(UIText.SimplePushSpecPage_message, sourceName));
		String checkRemote = sourceName;

		if (sourceName.startsWith(Constants.R_HEADS)) {
			try {
				BranchTrackingStatus status = BranchTrackingStatus.of(
						repository,
						sourceName.substring(Constants.R_HEADS.length()));

				if (status != null) {
					// calculate the name of the branch on the other side.
					checkRemote = status.getRemoteTrackingBranch();
					checkRemote = Constants.R_HEADS
							+ checkRemote.substring(checkRemote.indexOf('/',
									Constants.R_REMOTES.length() + 1) + 1);

					setMessage(NLS.bind(
							UIText.SimplePushSpecPage_pushAheadInfo,
							new Object[] { sourceName,
									Integer.valueOf(status.getAheadCount()),
									status.getRemoteTrackingBranch() }),
							IStatus.INFO);
				}
			} catch (Exception e) {
				// ignore and continue...
			}

			if (assist == null) {
				if (checkRemote != null)
					remoteRefName.setText(checkRemote);
				return;
			}

			if (checkRemote == null)
				checkRemote = sourceName;

			for (Ref ref : assist.getRefsForContentAssist(false, true))
				if (ref.getName().equals(checkRemote))
					remoteRefName.setText(checkRemote);
		}
	}

	@Override
	public boolean isPageComplete() {
		return remoteRefName.getText().length() > 0;
	}

	/**
	 * Whether the user wants to force pushing.
	 *
	 * @return whether to force the push
	 */
	public boolean isForceUpdate() {
		return forceUpdate;
	}

	/**
	 * Retrieves the target name to push to.
	 *
	 * @return the target name.
	 */
	public String getTargetRef() {
		return remoteRefName.getText();
	}

}
