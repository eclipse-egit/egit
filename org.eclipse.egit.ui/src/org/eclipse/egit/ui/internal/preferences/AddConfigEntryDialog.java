/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - input validation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Requests a key and value for adding a configuration entry.
 */
public class AddConfigEntryDialog extends TitleAreaDialog {

	/**
	 * Regular expression describing a valid git config key. See config.c in the
	 * CGit sources, or https://git-scm.com/docs/git-config. Basically it's
	 * section.subsection.name, where section and name must contain only
	 * alphanumeric characters or the dash, and name must start with a letter.
	 *
	 * Cgit also allows periods in the section name to support the legacy syntax
	 * [section.subsection]. For our use case, this is irrelevant, and EGit
	 * takes only the first segment as section name.
	 *
	 * Note that we allow arbitrary whitespace before and after; we'll trim that
	 * away in {@link #okPressed}.
	 */
	private static final Pattern VALID_KEY = Pattern.compile(
			"(\\h|\\v)*[-\\p{Alnum}]+(?:\\..*)?\\.\\p{Alpha}[-\\p{Alnum}]*(\\h|\\v)*"); //$NON-NLS-1$

	private Text keyText;

	private Text valueText;

	private String key;

	private String value;

	private final String suggestedKey;

	/**
	 * @param parentShell
	 * @param suggestedKey
	 *            may be null
	 */
	public AddConfigEntryDialog(Shell parentShell, String suggestedKey) {
		super(parentShell);
		setHelpAvailable(false);
		this.suggestedKey = suggestedKey;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(UIText.AddConfigEntryDialog_AddConfigTitle);
		setTitle(UIText.AddConfigEntryDialog_AddConfigTitle);
		setMessage(UIText.AddConfigEntryDialog_DialogMessage);
		Composite titleParent = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(titleParent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		Label keyLabel = new Label(main, SWT.NONE);
		keyLabel.setText(UIText.AddConfigEntryDialog_KeyLabel);
		keyLabel.setToolTipText(UIText.AddConfigEntryDialog_ConfigKeyTooltip);
		keyText = new Text(main, SWT.BORDER);
		if (suggestedKey != null) {
			keyText.setText(trimKey(suggestedKey));
			keyText.selectAll();
		}

		keyText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				check();
			}
		});
		keyText.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				addValueContentProposal(valueText, keyText.getText());
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(keyText);
		new Label(main, SWT.NONE)
				.setText(UIText.AddConfigEntryDialog_ValueLabel);
		valueText = new Text(main, SWT.BORDER);
		valueText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				check();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueText);

		applyDialogFont(main);

		addKeyContentProposal(keyText);
		return main;
	}

	private boolean isValidKey(String keyValue) {
		return keyValue != null && VALID_KEY.matcher(keyValue).matches();
	}

	private String trimKey(String keyValue) {
		return keyValue.replaceAll("^(?:\\h|\\v)*|(?:\\h|\\v)*$", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void create() {
		super.create();
		// we need to enter something
		getButton(OK).setEnabled(false);
	}

	private void check() {
		setErrorMessage(null);
		boolean hasError = false;
		try {
			if (keyText.getText().length() == 0) {
				setErrorMessage(
						UIText.AddConfigEntryDialog_MustEnterKeyMessage);
				hasError = true;
				return;
			}
			StringTokenizer st = new StringTokenizer(keyText.getText(), "."); //$NON-NLS-1$
			if (st.countTokens() < 2) {
				setErrorMessage(
						UIText.AddConfigEntryDialog_KeyComponentsMessage);
				hasError = true;
				return;
			}
			if (!isValidKey(keyText.getText())) {
				setErrorMessage(UIText.AddConfigEntryDialog_InvalidKeyMessage);
				hasError = true;
				return;
			}
			if (valueText.getText().length() == 0) {
				setErrorMessage(UIText.AddConfigEntryDialog_EnterValueMessage);
				hasError = true;
				return;
			}
		} finally {
			getButton(OK).setEnabled(!hasError);
		}
	}

	@Override
	protected void okPressed() {
		key = trimKey(keyText.getText());
		value = valueText.getText();
		super.okPressed();
	}

	/**
	 * @return the key as entered by the user
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return the value as entered by the user
	 */
	public String getValue() {
		return value;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.AddConfigEntryDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	private void addKeyContentProposal(final Text textField) {
		Map<String, ConfigProposal> configProposals = createAllProposals();
		List<String> keys = new ArrayList<>(configProposals.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		UIUtils.<String> addContentProposalToText(textField, () -> keys,
				(pattern, possibleKey) -> {
					if (pattern != null && !pattern.matcher(possibleKey).matches()) {
						return null;
					}
					ConfigProposal configProposal = configProposals.get(possibleKey);
					return new ContentProposal(configProposal.key,
							configProposal.description);
				}, null,
				UIText.AddConfigEntryDialog_ContentProposalStartTypingText,
				UIText.AddConfigEntryDialog_ContentProposalHoverText);
	}

	private void addValueContentProposal(final Text textField,
			final String selectedKey) {
		ConfigProposal configProposal = createAllProposals().get(selectedKey);
		List<String> values;
		if (configProposal != null) {
			values = configProposal.values;
		} else {
			values = Collections.emptyList();
		}
		UIUtils.<String> addContentProposalToText(textField, () -> values,
				(pattern, possibleValue) -> {
					if (pattern != null && !pattern.matcher(possibleValue).matches()) {
						return null;
					}
					return new ContentProposal(possibleValue);
				}, null,
				UIText.AddConfigEntryDialog_ContentProposalStartTypingText,
				UIText.AddConfigEntryDialog_ContentProposalHoverText);
	}

	private static Map<String, ConfigProposal> createAllProposals() {
		List<ConfigProposal> proposals = Arrays.asList(new ConfigProposal(
				"branch.autosetuprebase", //$NON-NLS-1$
				"When a new branch is created, this configures the branch to use rebase instead of merge, when pulling.", //$NON-NLS-1$
				Arrays.asList("always", "never", "local", "remote")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				new ConfigProposal("user.email", //$NON-NLS-1$
						"Your email address. Will be used as author email address and committer email address in new commits.", //$NON-NLS-1$
						Collections.emptyList()),
				new ConfigProposal("user.name", //$NON-NLS-1$
						"Your user name. Will be used as author and committer in new commits.", //$NON-NLS-1$
						Collections.emptyList()),
				new ConfigProposal("fetch.prune", //$NON-NLS-1$
						"Always prune the repository when fetching or pulling.", //$NON-NLS-1$
						Arrays.asList("true"))); //$NON-NLS-1$
		return proposals.stream()
				.collect(Collectors.toMap(configProposal -> configProposal.key,
						configProposal -> configProposal));
	}

	private static class ConfigProposal {
		public ConfigProposal(String key, String description,
				List<String> values) {
			this.key = key;
			this.description = description;
			this.values = values;
		}

		private final String key;

		private final String description;

		private final List<String> values;
	}
}
