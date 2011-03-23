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
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Configures the Remote
 */
public class ConfigureRemoteCommand extends
		RepositoriesViewCommandHandler<RemotesNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RemotesNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();
		StoredConfig config = repository.getConfig();
		final Set<String> remoteNames = config
				.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);

		IInputValidator validator = new IInputValidator() {
			public String isValid(String newText) {
				if (remoteNames.contains(newText))
					return NLS
							.bind(
									UIText.ConfigureRemoteCommand_RemoteAlreadyExistsMessage,
									newText);
				return null;
			}
		};
		InputDialog dlg = new InputDialog(getShell(event),
				UIText.ConfigureRemoteCommand_RemoteNameDialogTitle,
				UIText.ConfigureRemoteCommand_RemoteNameDialogMessage, null,
				validator);
		if (dlg.open() == Window.OK) {
			try {
				RemoteConfig rc = new RemoteConfig(config, dlg.getValue());
				rc.update(config);
				config.save();
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
				return null;
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				return null;
			}
		}
		return null;
	}
}
