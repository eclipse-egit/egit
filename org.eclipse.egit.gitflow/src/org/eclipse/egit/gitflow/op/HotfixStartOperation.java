/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.gitflow.GitFlowConfig;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

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
		GitFlowConfig config = repository.getConfig();
		String branchName = config.getHotfixBranchName(versionName);
		RevCommit head = repository.findHead(config.getMaster());
		if (head == null) {
			throw new IllegalStateException(NLS.bind(CoreText.StartOperation_unableToFindCommitFor, config.getDevelop()));
		}
		start(monitor, branchName, head);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
