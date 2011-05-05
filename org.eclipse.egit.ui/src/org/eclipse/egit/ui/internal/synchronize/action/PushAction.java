/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.action;

import static org.eclipse.egit.ui.internal.synchronize.GitModelSynchronizeParticipant.SYNCHRONIZATION_DATA;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
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
		final int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);

		return new SynchronizeModelOperation(configuration, elements) {

			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				GitSynchronizeDataSet gsds = (GitSynchronizeDataSet) getConfiguration()
						.getProperty(SYNCHRONIZATION_DATA);
				GitSynchronizeData gsd = gsds.iterator().next();

				String remoteName = gsd.getSrcRemoteName();
				if (remoteName == null)
					remoteName = gsd.getDstRemoteName();

				RemoteConfig rc;
				try {
					rc = new RemoteConfig(gsd.getRepository()
							.getConfig(), gsd.getDstRemoteName());
					PushOperationUI push = new PushOperationUI(gsd.getRepository(),
							rc, timeout, false);
					push.setCredentialsProvider(new EGitCredentialsProvider());
					push.execute(monitor);
				} catch (URISyntaxException e) {
					new InvocationTargetException(e);
				} catch (CoreException e) {
					new InvocationTargetException(e);
				}
			}
		};
	}

	@Override
	public boolean isEnabled() {
		GitSynchronizeDataSet gsds = (GitSynchronizeDataSet) getConfiguration()
				.getProperty(SYNCHRONIZATION_DATA);

		if (gsds == null || gsds.size() != 1)
			return false;

		GitSynchronizeData gsd = gsds.iterator().next();
		String srcRemoteName = gsd.getSrcRemoteName();
		String dstRemoteName = gsd.getDstRemoteName();

		return srcRemoteName != dstRemoteName
				&& (srcRemoteName != null || dstRemoteName != null);
	}

}
