/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.storage.GitFileHistoryProvider;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;

/**
 * The Team provider class for a Git repository.
 */
public class GitProvider extends RepositoryProvider {

	/**
	 * Id of repository provider
	 *
	 * @see #getID()
	 */
	public static final String ID = "org.eclipse.egit.core.GitProvider"; //$NON-NLS-1$

	private GitProjectData data;

	private GitMoveDeleteHook hook;

	private GitFileHistoryProvider historyProvider;

	private final IResourceRuleFactory resourceRuleFactory = new GitResourceRuleFactory();

	/**
	 * Default constructor
	 */
	public GitProvider() {
		super();
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public void configureProject() throws CoreException {
		GitProjectData projectData = getData();
		if (projectData != null) {
			projectData.markTeamPrivateResources();
		}
	}

	@Override
	public void deconfigure() throws CoreException {
		try {
			GitProjectData.deconfigure(getProject());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), e.getMessage(), e));
		}
	}

	@Override
	public boolean canHandleLinkedResources() {
		return true;
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		return true;
	}

	@Override
	public synchronized IMoveDeleteHook getMoveDeleteHook() {
		if (hook == null) {
			GitProjectData _data = getData();
			if (_data != null)
				hook = new GitMoveDeleteHook(_data);
		}
		return hook;
	}

	/**
	 * @return information about the mapping of an Eclipse project
	 * to a Git repository.
	 */
	@Nullable
	public synchronized GitProjectData getData() {
		if (data == null) {
			IProject project = getProject();
			if (project != null) {
				data = GitProjectData.get(project);
			}
		}
		return data;
	}

	@Override
	public synchronized IFileHistoryProvider getFileHistoryProvider() {
		if (historyProvider == null) {
			historyProvider = new GitFileHistoryProvider();
		}
		return historyProvider;
	}

	@Override
	public IResourceRuleFactory getRuleFactory() {
		return resourceRuleFactory;
	}

	private static class GitResourceRuleFactory extends ResourceRuleFactory {
		// Use the default rule factory instead of the
		// pessimistic one by default.
	}
}
