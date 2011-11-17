/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation to stage adds and deletes in the index
 */
public class AddToIndexOperation implements IEGitOperation {

	private static class IndexCommand {

		private final RepositoryMapping mapping;

		private AddCommand addCommand;

		private RmCommand rmCommand;

		public IndexCommand(final RepositoryMapping mapping) {
			this.mapping = mapping;
		}

		private String getFilePattern(final IResource resource) {
			String pattern = mapping.getRepoRelativePath(resource);
			return "".equals(pattern) ? "." : pattern; //$NON-NLS-1$ //$NON-NLS-2$
		}

		public void add(final IResource resource) {
			if (resource.exists()) {
				if (addCommand == null) {
					Repository repo = mapping.getRepository();
					AdaptableFileTreeIterator it = new AdaptableFileTreeIterator(
							repo, resource.getWorkspace().getRoot());
					addCommand = new Git(repo).add().setWorkingTreeIterator(it);
				}
				addCommand.addFilepattern(getFilePattern(resource));
			} else {
				if (rmCommand == null)
					rmCommand = new Git(mapping.getRepository()).rm();
				rmCommand.addFilepattern(getFilePattern(resource));
			}
		}

		public void call() throws NoFilepatternException {
			if (addCommand != null)
				addCommand.call();
			if (rmCommand != null)
				rmCommand.call();
		}
	}

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

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		Map<RepositoryMapping, IndexCommand> commands = new HashMap<RepositoryMapping, IndexCommand>();
		try {
			for (IResource resource : rsrcList) {
				addToCommand(resource, commands);
				monitor.worked(200);
			}
			for (IndexCommand command : commands.values())
				command.call();
		} catch (RuntimeException e) {
			throw new CoreException(Activator.error(CoreText.AddToIndexOperation_failed, e));
		} catch (NoFilepatternException e) {
			throw new CoreException(Activator.error(CoreText.AddToIndexOperation_failed, e));
		} finally {
			for (final RepositoryMapping rm : commands.keySet())
				rm.fireRepositoryChanged();
			monitor.done();
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(rsrcList.toArray(new IResource[rsrcList.size()]));
	}

	private void addToCommand(IResource resource,
			Map<RepositoryMapping, IndexCommand> commands) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource
				.getProject());
		IndexCommand command = commands.get(mapping);
		if (command == null) {
			command = new IndexCommand(mapping);
			commands.put(mapping, command);
		}
		command.add(resource);
	}
}
