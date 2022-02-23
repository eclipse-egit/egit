/*******************************************************************************
 * Copyright (C) 2010, 2022 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Renames a branch.
 */
public class RenameBranchOperation implements IEGitOperation {

	private final Repository repository;

	private final Ref branch;

	private final String newName;

	/**
	 * @param repository
	 * @param branch
	 *            the branch to rename
	 * @param newName
	 *            the new name
	 */
	public RenameBranchOperation(Repository repository, Ref branch,
			String newName) {
		this.repository = repository;
		this.branch = branch;
		this.newName = newName;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String taskName = NLS.bind(CoreText.RenameBranchOperation_TaskName,
				branch.getName(), newName);
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.setTaskName(taskName);
		try (Git git = new Git(repository)) {
			git.branchRename().setOldName(branch.getName()).setNewName(newName)
					.call();
		} catch (JGitInternalException | GitAPIException e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
