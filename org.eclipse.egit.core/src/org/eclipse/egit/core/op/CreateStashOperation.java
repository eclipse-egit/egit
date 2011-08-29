/*******************************************************************************
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com>
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;

/**
 * Create Stash Operation creates a new stash and attaches it to refs/stash
 */
public class CreateStashOperation implements IEGitOperation {

	private IResource[] resources;

	private ISchedulingRule schedulingRule;

	private PersonIdent author;

	private PersonIdent committer;

	private String commitMessage;

	/**
	 * Construct an CreateStashOperation
	 *
	 * @param resources
	 * @param author
	 * @param committer
	 * @param commitMessage
	 */
	public CreateStashOperation(IResource[] resources, String[] author, String[] committer, String commitMessage) {
		setCommitMessage(commitMessage);
		setAuthor(new PersonIdent(author[0], author[1]));
		setCommitter(new PersonIdent(committer[0], committer[1]));
		this.resources = new IResource[resources.length];
		System.arraycopy(resources, 0, this.resources, 0, resources.length);
		schedulingRule = calcSchedulingRule();
	}

	/**
	 * Construct a CreateStashOperation
	 *
	 * @param resources
	 * @param paths
	 */
	public CreateStashOperation(IResource[] resources, Set<String> paths) {
		this.resources = new IResource[resources.length];
		System.arraycopy(resources, 0, this.resources, 0, resources.length);
		schedulingRule = calcSchedulingRule();
	}

	public void execute(IProgressMonitor monitor) {
		Git repoTree;

		for (IResource res : resources) {
			repoTree = new Git(getRepository(res));
			try {
				repoTree.stashCreate().setAuthor(author).setCommitter(committer).setMessage(commitMessage).call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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

	/**
	 * @param author
	 */
	public void setAuthor(PersonIdent author) {
		this.author = author;
	}

	/**
	 * @return author
	 */
	public PersonIdent getAuthor() {
		return author;
	}

	/**
	 * @param committer
	 */
	public void setCommitter(PersonIdent committer) {
		this.committer = committer;
	}

	/**
	 * @return committer
	 */
	public PersonIdent getCommitter() {
		return committer;
	}

	/**
	 * @param commitMessage
	 */
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	/**
	 * @return commitMessage
	 */
	public String getCommitMessage() {
		return commitMessage;
	}
}
