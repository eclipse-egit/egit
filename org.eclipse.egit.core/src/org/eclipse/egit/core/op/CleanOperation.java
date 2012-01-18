/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <zx@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

/**
 * Clean operation cleans a repository or a selected list of resources
 */
public class CleanOperation implements IEGitOperation {

	private IResource[] resources;

	private ISchedulingRule schedulingRule;

	/**
	 * Construct an CleanOperation
	 *
	 * @param resources
	 */
	public CleanOperation(IResource[] resources) {
		this.resources = new IResource[resources.length];
		System.arraycopy(resources, 0, this.resources, 0, resources.length);
		schedulingRule = calcSchedulingRule();
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		IProgressMonitor m = monitor;
		if(m == null)
			m = new NullProgressMonitor();

		if(resources == null || resources.length == 0)
			return;

		m.beginTask(CoreText.CleanOperation_title, resources.length);

		try {
			Map<Repository, Set<String>> pathsInRepos = calculatePathsInRepos();
			for(Map.Entry<Repository, Set<String>> entry : pathsInRepos.entrySet()) {
				Repository repo = entry.getKey();
				Set<String> pathsToClean = entry.getValue();

				Git git = Git.wrap(repo);
				CleanCommand clean = git.clean();

				clean.setPaths(findPathsToCleanFromOffsets(pathsToClean, git.status().call()));
				clean.call();
			}
		} catch(final Exception e) {
			if(m.isCanceled())
				throw new CoreException(Status.CANCEL_STATUS);
		} finally {
			m.done();
		}
	}

	private Set<String> findPathsToCleanFromOffsets(Set<String> pathsToClean, org.eclipse.jgit.api.Status status) {
		Set<String> paths = new HashSet<String>();
		Set<String> untracked = status.getUntracked();

		for(String offset : pathsToClean) {
			for(String candidate : untracked) {
				if(candidate.startsWith(offset + "/")) //$NON-NLS-1$
					paths.add(candidate);
			}
		}

		return paths;
	}

	private Map<Repository, Set<String>> calculatePathsInRepos() {
		Map<Repository, Set<String>> repoPaths = new HashMap<Repository, Set<String>>();

		for(IResource res : resources) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(res);
			if(mapping != null) {
				if(!repoPaths.containsKey(mapping.getRepository()))
					repoPaths.put(mapping.getRepository(), new HashSet<String>());

				repoPaths.get(mapping.getRepository()).add(mapping.getRepoRelativePath(res));
			}
		}

		return repoPaths;
	}

	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private ISchedulingRule calcSchedulingRule() {
		List<ISchedulingRule> rules = new ArrayList<ISchedulingRule>();
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace()
				.getRuleFactory();
		for (IResource resource : resources) {
			IContainer container = resource.getParent();
			if (!(container instanceof IWorkspaceRoot)) {
				ISchedulingRule rule = ruleFactory.modifyRule(container);
				if (rule != null)
					rules.add(rule);
			}
		}
		if (rules.size() == 0)
			return null;
		else
			return new MultiRule(rules.toArray(new IResource[rules.size()]));
	}
}
