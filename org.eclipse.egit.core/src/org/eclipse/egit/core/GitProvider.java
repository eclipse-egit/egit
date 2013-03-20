/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.GitMoveDeleteHook;
import org.eclipse.egit.core.internal.project.GitProjectData;
import org.eclipse.egit.core.internal.storage.GitFileHistoryProvider;
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

	public String getID() {
		return ID;
	}

	public void configureProject() throws CoreException {
		getData().markTeamPrivateResources();
	}

	public void deconfigure() throws CoreException {
		try {
			GitProjectData.delete(getProject());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), e.getMessage(), e));
		}
	}

	public boolean canHandleLinkedResources() {
		return true;
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		return true;
	}

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
	public synchronized GitProjectData getData() {
		if (data == null) {
			data = GitProjectData.get(getProject());
		}
		return data;
	}

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
