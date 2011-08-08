/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.eclipse.egit.core.project.RepositoryMapping.findRepositoryMapping;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.Repository;

/**
 *
 */
public class RemoveFromIndexOperation implements IEGitOperation {

	private final Repository repo;

	private final Collection<String> paths;

	private final IResource[] resources;

	/**
	 * @param repo
	 *            repository in with given files should be removed from index
	 * @param resources
	 *            list of resources that should be removed from index
	 */
	public RemoveFromIndexOperation(Repository repo, IResource[] resources) {
		this.repo = repo;
		this.resources = resources;
		paths = new ArrayList<String>();

		RepositoryMapping mapping = RepositoryMapping.findRepositoryMapping(repo);
		for (IResource res : resources)
			paths.add(mapping.getRepoRelativePath(res));
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		ResetCommand resetCommand = new Git(repo).reset();
		resetCommand.setRef(HEAD);
		monitor.worked(1);

		for (String path : paths) {
			resetCommand.addPath(path);
			monitor.worked(1);
		}

		try {
			resetCommand.call();
			monitor.worked(1);
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		} finally {
			monitor.done();
			findRepositoryMapping(repo).fireRepositoryChanged();
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(resources);
	}

}
