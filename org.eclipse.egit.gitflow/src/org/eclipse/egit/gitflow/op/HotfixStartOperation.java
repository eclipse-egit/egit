/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.gitflow.GitFlowRepository;

/**
 * git flow hotfix start
 */
public final class HotfixStartOperation extends AbstractHotfixOperation {
	/**
	 * @param repository
	 * @param hotfixName
	 */
	public HotfixStartOperation(GitFlowRepository repository, String hotfixName) {
		super(repository, hotfixName);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String branchName = repository.getHotfixBranchName(versionName);

		start(monitor, branchName, repository.findHead(repository.getMaster()));
	}
}
