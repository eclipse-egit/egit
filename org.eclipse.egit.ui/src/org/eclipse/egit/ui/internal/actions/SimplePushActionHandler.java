/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Action for "Simple Push"
 */
public class SimplePushActionHandler extends RepositoryActionHandler
		implements IElementUpdater {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemote(repository);
		if (config == null) {
			MessageDialog.openInformation(getShell(event),
					UIText.SimplePushActionHandler_NothingToPushDialogTitle,
					UIText.SimplePushActionHandler_NothingToPushDialogMessage);
			return null;
		}

		PushOperationUI op = new PushOperationUI(repository, config.getName(), false);
		op.start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository();
		return repository != null
				&& SimpleConfigurePushDialog
						.getConfiguredRemoteCached(repository) != null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemoteCached(getRepository());
		if (config != null) {
			element.setText(SimpleConfigurePushDialog
					.getSimplePushCommandLabel(config));
		}
	}
}
