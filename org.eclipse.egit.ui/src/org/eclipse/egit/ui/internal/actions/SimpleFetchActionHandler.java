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
import org.eclipse.egit.ui.internal.fetch.FetchOperationUI;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Action for "Simple fetch"
 */
public class SimpleFetchActionHandler extends RepositoryActionHandler
		implements IElementUpdater {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;
		RemoteConfig config = SimpleConfigureFetchDialog
				.getConfiguredRemote(repository);
		if (config == null) {
			MessageDialog
					.openInformation(
							getShell(event),
							UIText.SimpleFetchActionHandler_NothingToFetchDialogTitle,
							UIText.SimpleFetchActionHandler_NothingToFetchDialogMessage);
			return null;
		}

		new FetchOperationUI(repository, config, false).start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository();
		return repository != null
				&& SimpleConfigureFetchDialog
						.getConfiguredRemoteCached(repository) != null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		RemoteConfig config = SimpleConfigureFetchDialog
				.getConfiguredRemoteCached(getRepository());
		if (config != null) {
			element.setText(SimpleConfigureFetchDialog
					.getSimpleFetchCommandLabel(config));
		}
	}
}
