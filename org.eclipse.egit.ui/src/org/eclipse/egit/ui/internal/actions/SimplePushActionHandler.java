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
import org.eclipse.egit.ui.internal.push.PushConfiguredRemoteOperation;
import org.eclipse.egit.ui.internal.push.SimplePushWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Action for choosing specifications for push, and pushing out to another
 * repository.
 */
public class SimplePushActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		RemoteConfig config = SimplePushWizard.getConfiguredRemote(repository);
		SimplePushWizard wizard = SimplePushWizard
				.getWizard(repository, config);
		if (config == null || wizard != null) {
			new WizardDialog(getShell(event), wizard).open();
		} else {
			PushConfiguredRemoteOperation op = new PushConfiguredRemoteOperation(
					repository, config, Activator.getDefault()
							.getPreferenceStore().getInt(
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
