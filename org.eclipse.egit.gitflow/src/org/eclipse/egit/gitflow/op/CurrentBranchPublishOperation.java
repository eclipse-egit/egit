/*******************************************************************************
 * Copyright (C) 2015, 2022 Max Hohenegger <eclipse@hohenegger.eu> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushConfig.PushDefault;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * git flow * publish
 */
public class CurrentBranchPublishOperation extends GitFlowOperation {
	private PushOperationResult operationResult;

	private int timeout;

	/**
	 * publish given branch
	 *
	 * @param repository
	 * @param timeout
	 * @throws CoreException
	 */
	public CurrentBranchPublishOperation(GitFlowRepository repository,
			int timeout) throws CoreException {
		super(repository);
		this.timeout = timeout;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String fullBranchName;
		try {
			fullBranchName = repository.getRepository().getFullBranch();
		} catch (IOException e) {
			throw new CoreException(error(e.getLocalizedMessage(), e));
		}
		String shortBranchName = Repository.shortenRefName(fullBranchName);
		RemoteConfig cfg = PushOperation.getRemote(shortBranchName,
				repository.getRepository().getConfig());
		try {
			PushOperation pushOperation = new PushOperation(
					repository.getRepository(), cfg.getName(),
					PushDefault.CURRENT, false, timeout);
			pushOperation.run(monitor);
			operationResult = pushOperation.getOperationResult();
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			throw new CoreException(error(targetException.getMessage(),
					targetException));
		}

		try {
			repository.setRemote(shortBranchName, cfg.getName());
			repository.setUpstreamBranchName(shortBranchName, fullBranchName);
		} catch (IOException e) {
			throw new CoreException(error(CoreText.unableToStoreGitConfig, e));
		}
	}

	/**
	 * @return result set after operation was executed
	 */
	public PushOperationResult getOperationResult() {
		return operationResult;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
