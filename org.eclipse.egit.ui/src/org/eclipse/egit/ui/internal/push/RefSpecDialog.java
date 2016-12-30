/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefContentAssistProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Add or edit a RefSpec
 */
public class RefSpecDialog extends TitleAreaDialog {
	private final boolean pushMode;

	private final Repository repo;

	private final RemoteConfig config;

	private RefSpec spec = new RefSpec();

	private Text sourceText;

	private Text destinationText;

	private Button forceButton;

	private Text specString;

	private boolean autoSuggestDestination;

	/**
	 * Create a {@link RefSpec}
	 *
	 * @param parentShell
	 * @param repository
	 * @param config
	 * @param push
	 */
	public RefSpecDialog(Shell parentShell, Repository repository,
			RemoteConfig config, boolean push) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repo = repository;
		this.config = config;
		this.pushMode = push;
		this.autoSuggestDestination = !pushMode;
		setHelpAvailable(false);
	}

	/**
	 * Edit a {@link RefSpec}
	 *
	 * @param parentShell
	 * @param repository
	 * @param config
	 * @param spec
	 * @param push
	 */
	public RefSpecDialog(Shell parentShell, Repository repository,
			RemoteConfig config, RefSpec spec, boolean push) {
		this(parentShell, repository, config, push);
		this.spec = spec;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RefSpecDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		if (pushMode) {
			setTitle(UIText.RefSpecDialog_PushTitle);
			setMessage(UIText.RefSpecDialog_PushMessage);
		} else {
			setTitle(UIText.RefSpecDialog_FetchTitle);
			setMessage(UIText.RefSpecDialog_FetchMessage);
		}
		// the user must enter something
		getButton(OK).setEnabled(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(2, false));

		URIish uriToCheck;
		if (pushMode) {
			if (config.getPushURIs().isEmpty())
				uriToCheck = config.getURIs().get(0);
			else
				uriToCheck = config.getPushURIs().get(0);
		} else
			uriToCheck = config.getURIs().get(0);

		final RefContentAssistProvider assistProvider = new RefContentAssistProvider(
				repo, uriToCheck, getShell());

		// source
		Label sourceLabel = new Label(main, SWT.NONE);
		if (pushMode)
			sourceLabel.setText(UIText.RefSpecDialog_SourceBranchPushLabel);
		else
			sourceLabel.setText(UIText.RefSpecDialog_SourceBranchFetchLabel);
		sourceText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(sourceText);
		if (spec != null && spec.getSource() != null)
			sourceText.setText(spec.getSource());
		sourceText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (sourceText.isFocusControl())
					if (autoSuggestDestination) {
						String name = sourceText.getText();
						if (name.startsWith(Constants.R_HEADS))
							name = name.substring(Constants.R_HEADS.length());
						else if (name.startsWith(Constants.R_TAGS))
							name = name.substring(Constants.R_TAGS.length());
						RefSpec sourceChanged = getSpec().setSource(
								sourceText.getText());
						setSpec(sourceChanged
								.setDestination(Constants.R_REMOTES
										+ config.getName() + '/' + name));
					} else
						setSpec(getSpec().setSource(sourceText.getText()));
			}
		});
		// content assist for source
		UIUtils.addRefContentProposalToText(sourceText, repo,
				() -> assistProvider.getRefsForContentAssist(true, pushMode));

		// suggest remote tracking branch
		if (!pushMode) {
			final Button autoSuggest = new Button(main, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(autoSuggest);
			autoSuggest.setText(UIText.RefSpecDialog_AutoSuggestCheckbox);
			autoSuggest.setSelection(true);
			autoSuggest.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					autoSuggestDestination = autoSuggest.getSelection();
				}
			});
		}

		// destination
		Label destinationLabel = new Label(main, SWT.NONE);
		if (pushMode)
			destinationLabel.setText(UIText.RefSpecDialog_DestinationPushLabel);
		else
			destinationLabel
					.setText(UIText.RefSpecDialog_DestinationFetchLabel);
		destinationText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(destinationText);
		if (spec != null && spec.getDestination() != null)
			destinationText.setText(spec.getDestination());
		destinationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (destinationText.isFocusControl())
					setSpec(getSpec().setDestination(destinationText.getText()));
			}
		});
		// content assist for destination
		UIUtils.addRefContentProposalToText(destinationText, repo,
				() -> assistProvider.getRefsForContentAssist(false, pushMode));

		// force update
		forceButton = new Button(main, SWT.CHECK);
		forceButton.setText(UIText.RefSpecDialog_ForceUpdateCheckbox);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(forceButton);
		if (spec != null)
			forceButton.setSelection(spec.isForceUpdate());
		forceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSpec().isForceUpdate() == forceButton.getSelection())
					return;
				setSpec(getSpec().setForceUpdate(forceButton.getSelection()));
			}

		});

		// RefSpec as String
		Label stringLabel = new Label(main, SWT.NONE);
		stringLabel.setText(UIText.RefSpecDialog_SpecificationLabel);
		specString = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				specString);
		if (spec != null)
			specString.setText(spec.toString());
		specString.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!specString.isFocusControl()
						|| getSpec().toString().equals(specString.getText()))
					return;
				setSpec(new RefSpec(specString.getText()));
			}
		});

		applyDialogFont(main);
		return main;
	}

	/**
	 * @return the {@link RefSpec}
	 */
	public RefSpec getSpec() {
		return this.spec;
	}

	private void setSpec(RefSpec spec) {
		setErrorMessage(null);
		this.spec = spec;
		String newSourceText = spec.getSource() != null ? spec.getSource() : ""; //$NON-NLS-1$
		String newDestinationText = spec.getDestination() != null ? spec
				.getDestination() : ""; //$NON-NLS-1$
		String newStringText = spec.toString();
		if (!sourceText.getText().equals(newSourceText))
			sourceText.setText(newSourceText);
		if (!destinationText.getText().equals(newDestinationText))
			destinationText.setText(newDestinationText);
		if (!specString.getText().equals(newStringText))
			specString.setText(newStringText);
		forceButton.setSelection(spec.isForceUpdate());
		if (sourceText.getText().length() == 0
				|| destinationText.getText().length() == 0)
			setErrorMessage(UIText.RefSpecDialog_MissingDataMessage);
		getButton(OK).setEnabled(
				sourceText.getText().length() > 0
						&& destinationText.getText().length() > 0);
	}

}
