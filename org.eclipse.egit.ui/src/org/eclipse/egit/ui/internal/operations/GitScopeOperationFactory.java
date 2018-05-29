/*******************************************************************************
 * Copyright (C) 2011, Tasktop Technologies Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Factory to create {@link GitScopeOperation}s. Used to replace
 * the ScopeOperations on the fly.
 *
 */
public class GitScopeOperationFactory {

	private static GitScopeOperationFactory instance;

	/**
	 * @param part
	 * @param manager
	 * @return a
	 */
	public GitScopeOperation createGitScopeOperation(
			IWorkbenchPart part, SubscriberScopeManager manager) {
		GitScopeOperation buildScopeOperation = new GitScopeOperation(part,
				manager);
		return buildScopeOperation;
	}

	/**
	 * @return the current factory
	 */
	public static synchronized GitScopeOperationFactory getFactory() {
		if(instance == null)
			instance = new GitScopeOperationFactory();
		return instance;
	}

	/**
	 * @param newInstance
	 */
	public static synchronized void setFactory(GitScopeOperationFactory newInstance) {
		instance = newInstance;
	}
}
