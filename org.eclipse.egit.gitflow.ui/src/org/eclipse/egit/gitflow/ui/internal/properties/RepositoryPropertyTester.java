/*******************************************************************************
 * Copyright (C) 2015, 2016 Max Hohenegger <eclipse@hohenegger.eu> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> Bug 484795
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.properties;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.gitflow.Activator;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.jgit.lib.Repository;

/**
 * Testing Git Flow states.
 */
public class RepositoryPropertyTester extends AbstractPropertyTester {
	private static final String IS_MASTER = "isMaster"; //$NON-NLS-1$

	private static final String IS_DEVELOP = "isDevelop"; //$NON-NLS-1$

	private static final String IS_HOTFIX = "isHotfix"; //$NON-NLS-1$

	private static final String IS_RELEASE = "isRelease"; //$NON-NLS-1$

	private static final String IS_INITIALIZED = "isInitialized"; //$NON-NLS-1$

	private static final String IS_FEATURE = "isFeature"; //$NON-NLS-1$

	private static final String HAS_DEFAULT_REMOTE = "hasDefaultRemote"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (receiver == null) {
			return false;
		}
		Repository repository = null;
		if (receiver instanceof String) {
			String gitDir = (String) receiver;
			repository = org.eclipse.egit.core.Activator.getDefault()
					.getRepositoryCache().getRepository(new File(gitDir));
		} else if (receiver instanceof Repository) {
			repository = (Repository) receiver;
		}
		if (repository == null || repository.isBare()) {
			return false;
		}
		return computeResult(expectedValue, internalTest(repository, property));
	}

	private boolean internalTest(Repository repository, String property) {
		GitFlowRepository gitFlowRepository = new GitFlowRepository(repository);
		try {
			if (IS_INITIALIZED.equals(property)) {
				return gitFlowRepository.getConfig().isInitialized();
			} else if (IS_FEATURE.equals(property)) {
				return gitFlowRepository.isFeature();
			} else if (IS_RELEASE.equals(property)) {
				return gitFlowRepository.isRelease();
			} else if (IS_HOTFIX.equals(property)) {
				return gitFlowRepository.isHotfix();
			} else if (IS_DEVELOP.equals(property)) {
				return gitFlowRepository.isDevelop();
			} else if (IS_MASTER.equals(property)) {
				return gitFlowRepository.isMaster();
			} else if (HAS_DEFAULT_REMOTE.equals(property)) {
				return gitFlowRepository.getConfig().hasDefaultRemote();
			}
		} catch (IOException e) {
			Activator.getDefault().getLog().log(error(e.getMessage(), e));
		}
		return false;
	}

}
