/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Open the "Configure Fetch" dialog
 */
public class ConfigureFetchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);
		if (repository != null)
			SimpleConfigureFetchDialog.getDialog(getShell(event), repository)
					.open();
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		return repository != null
				&& SimpleConfigureFetchDialog
						.getConfiguredRemote(repository) != null;
	}

}
