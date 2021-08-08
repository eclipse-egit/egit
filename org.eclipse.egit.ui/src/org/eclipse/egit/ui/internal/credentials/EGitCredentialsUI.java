/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.credentials;

import org.eclipse.egit.core.credentials.CredentialsUI;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CustomPromptDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

/**
 * A basic UI for getting credentials, published as an OSGi service with default
 * service ranking.
 */
@Component(immediate = false)
public class EGitCredentialsUI implements CredentialsUI {

	@Override
	public boolean fillCredentials(URIish uri, CredentialItem... items) {
		if (items.length == 0) {
			return true;
		}
		boolean[] result = { false };

		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			Shell shell = PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell();

			if (items.length == 1) {
				result[0] = getSingleSpecial(shell, uri, items[0]);
			} else {
				result[0] = getMultiSpecial(shell, uri, items);
			}
		});

		return result[0];
	}

	/**
	 * Opens a dialog for a single non-user, non-password type item.
	 *
	 * @param shell
	 *            the shell to use
	 * @param uri
	 *            the uri of the get request
	 * @param item
	 *            the item to handle
	 * @return {@code true} if the request was successful and values were
	 *         supplied; {@code false} if the user canceled the request and did
	 *         not supply all requested values.
	 */
	private boolean getSingleSpecial(Shell shell, URIish uri,
			CredentialItem item) {
		if (item instanceof CredentialItem.InformationalMessage) {
			MessageDialog.openInformation(shell,
					UIText.EGitCredentialsProvider_information,
					item.getPromptText());
			return true;
		} else if (item instanceof CredentialItem.YesNoType) {
			CredentialItem.YesNoType v = (CredentialItem.YesNoType) item;
			String[] labels = new String[] { IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
			int[] resultIDs = new int[] { IDialogConstants.YES_ID,
					IDialogConstants.NO_ID, IDialogConstants.CANCEL_ID };

			MessageDialog dialog = new MessageDialog(shell,
					UIText.EGitCredentialsProvider_question, null,
					item.getPromptText(), MessageDialog.QUESTION_WITH_CANCEL,
					labels, 0);
			int r = dialog.open();
			if (r < 0) {
				return false;
			}

			switch (resultIDs[r]) {
			case IDialogConstants.YES_ID:
				v.setValue(true);
				return true;
			case IDialogConstants.NO_ID:
				v.setValue(false);
				return true;
			default:
				// abort
				return false;
			}
		} else {
			// generically handles all other types of items
			return getMultiSpecial(shell, uri, item);
		}
	}

	/**
	 * Opens a generic dialog presenting all CredentialItems to the user.
	 *
	 * @param shell
	 *            the shell to use
	 * @param uri
	 *            the uri of the get request
	 * @param items
	 *            the items to handle
	 * @return {@code true} if the request was successful and values were
	 *         supplied; {@code false} if the user canceled the request and did
	 *         not supply all requested values.
	 */
	private boolean getMultiSpecial(Shell shell, URIish uri,
			CredentialItem... items) {
		CustomPromptDialog dialog = new CustomPromptDialog(shell, uri,
				UIText.EGitCredentialsProvider_information, items);
		if (dialog.open() == Window.OK) {
			return true;
		}
		return false;
	}

	@Override
	public UserPasswordCredentials getCredentials(URIish uri) {
		UserPasswordCredentials[] credentials = { null };
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			Shell shell = PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell();
			credentials[0] = LoginService.login(shell, uri);
		});
		return credentials[0];
	}
}
