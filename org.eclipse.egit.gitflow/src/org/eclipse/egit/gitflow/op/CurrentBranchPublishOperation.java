/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;

/**
 * git flow * publish
 */
@SuppressWarnings("restriction")
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
		try {
			PushOperation pushOperation = new PushOperation(
					repository.getRepository(), DEFAULT_REMOTE_NAME, false,
					timeout);
			pushOperation.run(monitor);
			operationResult = pushOperation.getOperationResult();

			if (!operationResult.isSuccessfulConnectionForAnyURI()) {
				String errorMessage = String.format(
						CoreText.pushToRemoteFailed,
						operationResult.getErrorStringForAllURis());
				throw new CoreException(error(errorMessage));
			}
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			throw new CoreException(error(targetException.getMessage(),
					targetException));
		}

		String newLocalBranch = getCurrentBranchhName();
		try {
			repository.setRemote(newLocalBranch, DEFAULT_REMOTE_NAME);
			repository.setMerge(newLocalBranch, repository.getRepository().getFullBranch());
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

	private String getCurrentBranchhName() {
		try {
			return repository.getRepository().getBranch();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
