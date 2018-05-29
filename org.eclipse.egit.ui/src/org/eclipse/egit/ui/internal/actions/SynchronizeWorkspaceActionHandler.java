/*******************************************************************************
 * Copyright (C) 2011, 2012 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.eclipse.egit.core.synchronize.dto.GitSynchronizeData.BRANCH_NAME_PATTERN;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_MERGE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * An action that launch synchronization with selected repository
 */
public class SynchronizeWorkspaceActionHandler extends RepositoryActionHandler {

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		Map<Repository, Set<IResource>> containerMap = mapContainerResources(resources);

		if (containerMap.isEmpty())
			return null;

		boolean launchFetch = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH);
		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet();
		for (Entry<Repository, Set<IResource>> entry : containerMap.entrySet())
			try {
				Repository repo = entry.getKey();
				String dstRef = getDstRef(repo, launchFetch);
				GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, dstRef, true);
				Set<IResource> containers = entry.getValue();
				if (!containers.isEmpty())
					data.setIncludedResources(containers);

				gsdSet.add(data);
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}

		GitModelSynchronize.launch(gsdSet, getSelectedResources(event));

		return null;
	}

	private Map<Repository, Set<IResource>> mapContainerResources(
			IResource[] resources) {
		Map<Repository, Set<IResource>> result = new HashMap<>();

		for (IResource resource : resources) {
			RepositoryMapping rm = RepositoryMapping.getMapping(resource);
			if (rm == null)
				continue; // Linked resources may not be in a repo
			if (resource instanceof IProject)
				result.put(rm.getRepository(), new HashSet<IResource>());
			else if (resource instanceof IContainer) {
				Set<IResource> containers = result.get(rm.getRepository());
				if (containers == null) {
					containers = new HashSet<>();
					result.put(rm.getRepository(), containers);
					containers.add(resource);
				} else if (containers.size() > 0)
					containers.add(resource);
			}
		}

		return result;
	}

	private String getDstRef(Repository repo, boolean launchFetch) {
		if (launchFetch) {
			String realName = getRealBranchName(repo);
			String name = BRANCH_NAME_PATTERN.matcher(realName).replaceAll(""); //$NON-NLS-1$
			StoredConfig config = repo.getConfig();
			String remote = config.getString(CONFIG_BRANCH_SECTION, name,
					CONFIG_KEY_REMOTE);
			String merge = config.getString(CONFIG_BRANCH_SECTION, name,
					CONFIG_KEY_MERGE);
			if (remote == null || merge == null)
				return HEAD;

			String mergeBranchName = merge.replace(R_HEADS, ""); //$NON-NLS-1$
			return R_REMOTES + remote + "/" + mergeBranchName; //$NON-NLS-1$
		} else
			return HEAD;
	}

	private String getRealBranchName(Repository repo) {
		Ref ref;

		try {
			ref = repo.exactRef(HEAD);
		} catch (IOException e) {
			ref = null;
		}

		if (ref != null && ref.isSymbolic())
			return ref.getTarget().getName();
		else
			return HEAD;
	}

}
