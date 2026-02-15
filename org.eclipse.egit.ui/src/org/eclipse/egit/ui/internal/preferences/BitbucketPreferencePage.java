/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for Bitbucket Data Center configuration
 */
public class BitbucketPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/**
	 * Creates a new {@link BitbucketPreferencePage}
	 */
	public BitbucketPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Configure Bitbucket Data Center connection settings.\n\n" //$NON-NLS-1$
				+ "Note: Use personal access tokens for authentication. " //$NON-NLS-1$
				+ "Tokens should be stored securely using the Eclipse secure storage."); //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		// Server URL field
		StringFieldEditor serverUrlEditor = new StringFieldEditor(
				UIPreferences.BITBUCKET_SERVER_URL, 
				"Server &URL:", //$NON-NLS-1$
				parent);
		serverUrlEditor.getTextControl(parent).setToolTipText(
				"Bitbucket Data Center server URL (e.g., https://bitbucket.example.com)"); //$NON-NLS-1$
		addField(serverUrlEditor);

		// Project key field
		StringFieldEditor projectKeyEditor = new StringFieldEditor(
				UIPreferences.BITBUCKET_PROJECT_KEY,
				"Project &Key:", //$NON-NLS-1$
				parent);
		projectKeyEditor.getTextControl(parent).setToolTipText(
				"Default project key (e.g., PROJ)"); //$NON-NLS-1$
		addField(projectKeyEditor);

		// Repository slug field
		StringFieldEditor repoSlugEditor = new StringFieldEditor(
				UIPreferences.BITBUCKET_REPO_SLUG,
				"Repository &Slug:", //$NON-NLS-1$
				parent);
		repoSlugEditor.getTextControl(parent).setToolTipText(
				"Default repository slug (e.g., my-repo)"); //$NON-NLS-1$
		addField(repoSlugEditor);

	// Username field
	StringFieldEditor usernameEditor = new StringFieldEditor(
			UIPreferences.BITBUCKET_USERNAME,
			"&Username:", //$NON-NLS-1$
			parent);
	usernameEditor.getTextControl(parent).setToolTipText(
			"Your Bitbucket username/slug (e.g., 'firstname.lastname'). NOT your email address or display name."); //$NON-NLS-1$
	addField(usernameEditor);
	
	// Add help text for username format
	Label usernameHintLabel = new Label(parent, SWT.WRAP);
	usernameHintLabel.setText(
			"Use your Bitbucket username (e.g., 'firstname.lastname'), NOT your email address."); //$NON-NLS-1$
	GridDataFactory.fillDefaults().span(2, 1).indent(20, 0).applyTo(usernameHintLabel);

		// Personal access token field (password style)
		StringFieldEditor tokenEditor = new StringFieldEditor(
				UIPreferences.BITBUCKET_ACCESS_TOKEN,
				"Personal Access &Token:", //$NON-NLS-1$
				parent) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		tokenEditor.getTextControl(parent).setToolTipText(
				"Personal access token for API authentication"); //$NON-NLS-1$
		addField(tokenEditor);

		// Add info label
		Label infoLabel = new Label(parent, SWT.WRAP);
		infoLabel.setText(
				"\nNote: You can create a personal access token in Bitbucket under:\n" //$NON-NLS-1$
						+ "Profile > Manage account > Personal access tokens"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().span(2, 1).hint(400, SWT.DEFAULT)
				.applyTo(infoLabel);

		// Separator before display options
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
				.indent(0, 10).applyTo(separator);

		// Inline comments toggle
		BooleanFieldEditor inlineCommentsEditor = new BooleanFieldEditor(
				UIPreferences.PULLREQUEST_SHOW_INLINE_COMMENTS,
				"Show inline comments in pull request &compare viewer", //$NON-NLS-1$
				parent);
		addField(inlineCommentsEditor);
	}
}
