/*******************************************************************************
 * Copyright (C) 2023, Simon Eder <simon.eclipse@hotmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.job;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jgit.lib.Repository;

/**
 * An implementation to calculate an {@link ISchedulingRule} for Jobs working on
 * the index of a plain Git {@link Repository}. <br>
 * It is used if a repository is not available as project within the Eclipse
 * workspace. The rule uses the repository's location as lock.
 */
public class RepositoryRule implements ISchedulingRule {

	private final Repository repo;

	private String getRepositoryPath() {
		return repo.getDirectory().getAbsolutePath();
	}

	private boolean isSameRepositoryRule(ISchedulingRule rule) {
		if (this == rule) {
			return true;
		}
		if (rule instanceof RepositoryRule) {
			RepositoryRule r = (RepositoryRule) rule;
			if (this.repo.getDirectory().getAbsolutePath()
					.equals(r.getRepositoryPath())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a new <code>ISchedulingRule</code> for Git repository
	 * <code>repo</code>.
	 *
	 * @param repo
	 */
	public RepositoryRule(Repository repo) {
		this.repo = repo;
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return isSameRepositoryRule(rule);
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return isSameRepositoryRule(rule);
	}
}
