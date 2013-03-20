/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.AdaptableFileTreeIterator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.project.RepositoryMapping;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 */
public class AddToIndexOperation implements IEGitOperation {
	private final Collection<? extends IResource> rsrcList;

	/**
	 * Create a new operation to add files to the Git index
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be added to the
	 *            relevant Git repositories.
	 */
	public AddToIndexOperation(final Collection<? extends IResource> rsrcs) {
		rsrcList = rsrcs;
	}

	/**
	 * Create a new operation to add files to the Git index
	 *
	 * @param resources
	 *            array of {@link IResource}s which should be added to the
	 *            relevant Git repositories.
	 */
	public AddToIndexOperation(IResource[] resources) {
		rsrcList = Arrays.asList(resources);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.internal.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		Map<RepositoryMapping, AddCommand> addCommands = new HashMap<RepositoryMapping, AddCommand>();
		try {
			for (IResource obj : rsrcList) {
				addToCommand(obj, addCommands);
				monitor.worked(200);
			}

			for (AddCommand command : addCommands.values()) {
				command.call();
			}
		} catch (RuntimeException e) {
			throw new CoreException(Activator.error(CoreText.AddToIndexOperation_failed, e));
		} catch (GitAPIException e) {
			throw new CoreException(Activator.error(CoreText.AddToIndexOperation_failed, e));
		} finally {
			for (final RepositoryMapping rm : addCommands.keySet())
				rm.fireRepositoryChanged();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.internal.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(rsrcList.toArray(new IResource[rsrcList.size()]));
	}

	private void addToCommand(IResource resource, Map<RepositoryMapping, AddCommand> addCommands) {
		IProject project = resource.getProject();
		RepositoryMapping map = RepositoryMapping.getMapping(project);
		AddCommand command = addCommands.get(map);
		if (command == null) {
			Repository repo = map.getRepository();
			Git git = new Git(repo);
			AdaptableFileTreeIterator it = new AdaptableFileTreeIterator(repo,
					resource.getWorkspace().getRoot());
			command = git.add().setWorkingTreeIterator(it);
			addCommands.put(map, command);
		}
		String filepattern = map.getRepoRelativePath(resource);
		if ("".equals(filepattern)) //$NON-NLS-1$
			filepattern = "."; //$NON-NLS-1$
		command.addFilepattern(filepattern);
	}

}
