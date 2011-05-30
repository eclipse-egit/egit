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
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
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
		Git repoTree;
		// discover repositories and run clean on them
		for (IResource res : resources) {
			repoTree = new Git(getRepository(res));
			repoTree.clean().call();
		}
	}

	/**
	 * Dry run on cleancommand
	 * @return a Set<String>
	 */
	public Set<String> dryRun() {
		if (resources.length == 0)
			return null;

		Repository repo = getRepository(resources[0]);
		CleanCommand clean = new Git(repo).clean();

		return clean.setDryRun(true).call();
	}

	private static Repository getRepository(IResource resource) {
		RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(resource.getProject());
		if (repositoryMapping != null)
			return repositoryMapping.getRepository();
		else
			return null;
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
