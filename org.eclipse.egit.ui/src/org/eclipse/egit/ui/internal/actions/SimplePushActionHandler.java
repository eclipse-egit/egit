/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.INamedHandleStateIds;
import org.eclipse.core.commands.State;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Action for "Push to Upstream"
 */
public class SimplePushActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;
		Shell shell = getShell(event);
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemote(repository);

		pushOrConfigure(repository, config, shell);
		return null;
	}

	/**
	 * @param repository
	 * @param config
	 * @param shell
	 */
	public static void pushOrConfigure(final Repository repository,
			RemoteConfig config, Shell shell) {
		if (config != null) {
			int timeout = Activator.getDefault().getPreferenceStore().getInt(
					UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			PushOperationUI op = new PushOperationUI(repository, config.getName(), timeout,
					false);
			op.start();
		} else {
			Ref head = getHeadIfSymbolic(repository);
			if (head != null) {
				PushBranchWizard pushBranchWizard = new PushBranchWizard(
						repository, head.getTarget());

				WizardDialog dlg = new WizardDialog(shell,
						pushBranchWizard);
				dlg.open();
			}
		}
	}

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository();
		if (repository == null)
			return false;

		Ref head = getHeadIfSymbolic(repository);
		if (head == null)
			return false;

		ICommandService commandService = (ICommandService) PlatformUI
				.getWorkbench().getService(ICommandService.class);
		if (commandService != null) {
			boolean isConfigured = SimpleConfigurePushDialog
					.getConfiguredRemote(repository) != null;
			Command command = commandService.getCommand(ActionCommands.SIMPLE_PUSH_ACTION);
			State name = command.getState(INamedHandleStateIds.NAME);
			String nameText = isConfigured ? UIText.SimplePushActionHandler_ConfiguredName : UIText.SimplePushActionHandler_UnconfiguredName;
			name.setValue(nameText);
		}

		return true;
	}

	private static Ref getHeadIfSymbolic(Repository repository) {
		try {
			Ref head = repository.getRef(Constants.HEAD);
			if (head != null && head.isSymbolic())
				return head;
			else
				return null;
		} catch (IOException e) {
			return null;
		}
	}
}
