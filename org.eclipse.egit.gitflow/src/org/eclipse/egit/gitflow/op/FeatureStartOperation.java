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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * git flow feature start
 */
public final class FeatureStartOperation extends AbstractFeatureOperation {
	/**
	 * @param repository
	 * @param featureName
	 */
	public FeatureStartOperation(GitFlowRepository repository,
			String featureName) {
		super(repository, featureName);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String branchName = repository.getConfig().getFeatureBranchName(featureName);

		try {
			if (!repository.isDevelop()) {
				throw new CoreException(
						error(NLS.bind(CoreText.FeatureStartOperation_notOn, repository.getConfig().getDevelop())));
			}
		} catch (IOException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
		RevCommit head;
		try {
			head = repository.findHead();
		} catch (WrongGitFlowStateException e) {
			throw new CoreException(error(e));
		}
		start(monitor, branchName, head);
	}
}
