/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.swt.widgets.Shell;

/**
 * Action for "Push to Upstream" or "Push Branch..." if not configured
 */
public class PushUpstreamOrBranchActionHandler extends RepositoryActionHandler {
	@Override
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
			PushOperationUI op = new PushOperationUI(repository,
					config.getName(), false);
			op.start();
		} else {
			Ref head = getHeadIfSymbolic(repository);
			if (head != null) {
				PushBranchWizard pushBranchWizard = new PushBranchWizard(
						repository, head);

				PushWizardDialog dlg = new PushWizardDialog(shell,
						pushBranchWizard);
				dlg.open();
			}
		}
	}

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository();
		if (repository == null) {
			return false;
		}
		Ref head = SelectionRepositoryStateCache.INSTANCE.getHeadRef(repository);
		return head != null && head.isSymbolic();
	}

	private static Ref getHeadIfSymbolic(Repository repository) {
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			if (head != null && head.isSymbolic())
				return head;
			else
				return null;
		} catch (IOException e) {
			return null;
		}
	}

}
