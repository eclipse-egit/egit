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

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushWizard;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for choosing specifications for push, and pushing out to another
 * repository.
 */
public class PushActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		final PushWizard pushWizard;
		try {
			pushWizard = new PushWizard(repository);
		} catch (URISyntaxException x) {
			ErrorDialog.openError(getShell(event),
					UIText.PushAction_wrongURITitle,
					UIText.PushAction_wrongURIDescription, new Status(
							IStatus.ERROR, Activator.getPluginId(), x
									.getMessage(), x));
			return null;
		}
		WizardDialog dlg = new WizardDialog(getShell(event), pushWizard);
		dlg.setHelpAvailable(true);
		dlg.open();
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			Repository repository = getRepository();
			if (repository == null) {
				return false;
			}
			Ref ref = repository.exactRef(Constants.HEAD);
			return ref != null && ref.getObjectId() != null;
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}
}
