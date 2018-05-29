/*******************************************************************************
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;

/**
 * Sets the config property gerrit.createchangeid
 */
public class SetChangeIdTask implements PostCloneTask {

	private final boolean createchangeid;

	/**
	 * @param createchangeid
	 */
	public SetChangeIdTask(boolean createchangeid) {
		this.createchangeid = createchangeid;
	}

	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			repository.getConfig().setBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
					null, ConfigConstants.CONFIG_KEY_CREATECHANGEID, createchangeid);
			repository.getConfig().save();
		} catch (IOException e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

}
