/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation that deletes a tag
 */
public class DeleteTagOperation implements IEGitOperation {

	private final Repository repository;

	private final String tag;

	/**
	 * Create operation that deletes a single tag
	 *
	 * @param repository
	 * @param tag
	 */
	public DeleteTagOperation(final Repository repository, final String tag) {
		this.repository = repository;
		this.tag = tag;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			Git.wrap(repository).tagDelete().setTags(tag).call();
		} catch (GitAPIException e) {
			throw new CoreException(Activator.error(
					CoreText.DeleteTagOperation_exceptionMessage, e));
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
