/*******************************************************************************
 * Copyright (C) 2011-2012, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Adds a fetch specification of the cloned repository and performs a fetch
 */
public class ConfigureFetchAfterCloneTask implements PostCloneTask {

	private String fetchRefSpec;

	private final String remoteName;

	/**
	 * @param remoteName name of the remote in the git config file
	 * @param fetchRefSpec the fetch ref spec which will be added
	 */
	public ConfigureFetchAfterCloneTask(String remoteName, String fetchRefSpec) {
		this.remoteName = remoteName;
		this.fetchRefSpec = fetchRefSpec;
	}

	/**
	 * @param repository the cloned repository
	 * @param monitor
	 * @throws CoreException
	 */
	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try (Git git = new Git(repository)) {
			RemoteConfig configToUse = new RemoteConfig(repository.getConfig(),
					remoteName);
			if (fetchRefSpec != null) {
				configToUse.addFetchRefSpec(new RefSpec(fetchRefSpec));
			}
			configToUse.update(repository.getConfig());
			repository.getConfig().save();
			git.fetch().setRemote(remoteName).call();
		} catch (Exception e) {
			Activator.logError(NLS.bind(
					CoreText.ConfigureFetchAfterCloneTask_couldNotFetch,
					fetchRefSpec), e);
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

}
