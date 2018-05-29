/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.fetch.FetchWizard;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for displaying fetch wizard - allowing selection of specifications for
 * fetch, and fetching objects/refs from another repository.
 */
public class FetchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		final FetchWizard fetchWizard;
		try {
			fetchWizard = new FetchWizard(repository);
		} catch (URISyntaxException x) {
			ErrorDialog.openError(getShell(event),
					UIText.FetchAction_wrongURITitle,
					UIText.FetchAction_wrongURIMessage, new Status(
							IStatus.ERROR, Activator.getPluginId(), x
									.getMessage(), x));
			return null;
		}
		WizardDialog dlg = new WizardDialog(getShell(event), fetchWizard);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}
}
