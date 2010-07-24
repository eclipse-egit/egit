/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

/**
 *
 */
public class GitSubscriberMergeContext extends SubscriberMergeContext {

	private final GitSynchronizeDataSet gsds;

	/**
	 * @param subscriber
	 * @param manager
	 * @param gsds
	 */
	public GitSubscriberMergeContext(Subscriber subscriber,
			ISynchronizationScopeManager manager, GitSynchronizeDataSet gsds) {
		super(subscriber, manager);
		this.gsds = gsds;

		initialize();
	}

	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	/**
	 * @return git synchronization data
	 */
	public GitSynchronizeDataSet getSyncData() {
		return gsds;
	}

	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

}
