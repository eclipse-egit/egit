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

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.internal.pull.PullOperationUI;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Pull action used in Synchronize view toolbar
 */
public class PullAction extends SynchronizeModelAction {

	/**
	 * Construct {@link PullAction}
	 *
	 * @param text the action's text
	 * @param configuration the actions synchronize page configuration
	 */
	public PullAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new SynchronizeModelOperation(configuration, elements) {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				GitSynchronizeDataSet gsds = (GitSynchronizeDataSet) getConfiguration()
						.getProperty(SYNCHRONIZATION_DATA);

				Set<Repository> repositories = new HashSet<>();
				for (GitSynchronizeData gsd : gsds)
					repositories.add(gsd.getRepository());

				PullOperationUI pull = new PullOperationUI(repositories);
				pull.execute(monitor);
			}
		};
	}

	@Override
	public boolean isEnabled() {
		return getConfiguration().getProperty(SYNCHRONIZATION_DATA) != null;
	}

}
