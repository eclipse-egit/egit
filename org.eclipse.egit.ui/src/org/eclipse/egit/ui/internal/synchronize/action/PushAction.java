/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.action;

import static org.eclipse.egit.ui.internal.synchronize.GitModelSynchronizeParticipant.SYNCHRONIZATION_DATA;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Push action used in Synchronize view toolbar
 */
public class PushAction extends SynchronizeModelAction {

	/**
	 * Construct {@link PushAction}
	 *
	 * @param text the action's text
	 * @param configuration the actions synchronize page configuration
	 */
	public PushAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new SynchronizeModelOperation(configuration, elements) {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				runPushOperation();
			}
		};
	}

	@Override
	public boolean isEnabled() {
		GitSynchronizeDataSet gsds = (GitSynchronizeDataSet) getConfiguration()
				.getProperty(SYNCHRONIZATION_DATA);
		for (GitSynchronizeData gsd : gsds)
			if (gsd.getDstRemoteName() != null)
				return true;

		return false;
	}

	private void runPushOperation() {
		GitSynchronizeDataSet gsds = (GitSynchronizeDataSet) getConfiguration()
				.getProperty(SYNCHRONIZATION_DATA);

		for (GitSynchronizeData gsd : gsds) {
			String remoteName = gsd.getDstRemoteName();
			if (remoteName == null)
				continue;

			RemoteConfig rc;
			Repository repo = gsd.getRepository();
			StoredConfig config = repo.getConfig();
			try {
				rc = new RemoteConfig(config, remoteName);
			} catch (URISyntaxException e) {
				Activator
						.logError(
								"Unable to create RemoteConfiguration for remote: " + remoteName, e); //$NON-NLS-1$
				continue;
			}

			if (rc.getPushRefSpecs().isEmpty())
				rc.addPushRefSpec(new RefSpec(HEAD + ":" + gsd.getDstMerge())); //$NON-NLS-1$
			PushOperationUI push = new PushOperationUI(repo, rc, false);
			push.setCredentialsProvider(new EGitCredentialsProvider());
			push.start();
		}
	}

}
