/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.fetch.FetchConfiguredRemoteAction;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for "Simple fetch"
 */
public class SimpleFetchActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		if (SimpleConfigureFetchDialog.shouldConfigure(repository)) {
			Dialog dlg = SimpleConfigureFetchDialog.getDialog(getShell(event),
					repository);
			dlg.open();
		} else {
			FetchConfiguredRemoteAction op = new FetchConfiguredRemoteAction(
					repository, SimpleConfigureFetchDialog
							.getConfiguredRemote(repository), Activator
							.getDefault().getPreferenceStore().getInt(
									UIPreferences.REMOTE_CONNECTION_TIMEOUT));
			op.start();
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
