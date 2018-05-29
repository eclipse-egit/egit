/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.egit.ui.internal.KnownHosts;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

/**
 * Stores the host name after a successful clone.
 */
public class RememberHostTask implements PostCloneTask {

	private final @NonNull String hostName;

	/**
	 * Creates a {@link PostCloneTask} that stores the given host name after a
	 * successful clone.
	 *
	 * @param hostName
	 *            to store
	 */
	public RememberHostTask(@NonNull String hostName) {
		this.hostName = hostName;
	}

	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				KnownHosts.addKnownHost(hostName);
			}
		});

	}

}
