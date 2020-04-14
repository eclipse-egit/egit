/*******************************************************************************
 * Copyright (c) 2010, 2013, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bugs 477281, 478877
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.RepositorySearchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.RepositoryProvider;

/**
 * "Adds" repositories
 */
public class AddCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroup group = getSelectedRepositoryGroup(event);
		RepositorySearchWizard wizard = new RepositorySearchWizard(
				util.getConfiguredRepositories(), true);
		WizardDialog dialog = new WizardDialog(getShell(event), wizard) {
			@Override
			protected Button createButton(Composite parent, int id,
					String label, boolean defaultButton) {
				if (id == IDialogConstants.FINISH_ID) {
					return super.createButton(parent, id,
							UIText.AddCommand_AddButtonLabel, defaultButton);
				}
				return super.createButton(parent, id, label, defaultButton);
			}
		};
		if (dialog.open() == Window.OK) {
			for (String dir : wizard.getDirectories()) {
				File repositoryDir = FileUtils.canonicalize(new File(dir));
				addRepository(repositoryDir, group);
			}
			expandRepositoryGroup(event, group);
		}
		return null;
	}

	private void addRepository(File repositoryDir, RepositoryGroup group) {
		GerritUtil.tryToAutoConfigureForGerrit(repositoryDir);
		if (group != null) {
			RepositoryGroups.getInstance().addRepositoriesToGroup(group,
				Collections.singletonList(repositoryDir));
		}
		util.addConfiguredRepository(repositoryDir);
		if (doAutoShare()) {
			autoShareProjects(repositoryDir);
		}
	}

	private void autoShareProjects(File repositoryDir) {
		// Don't even try to auto-share for bare repositories.
		IPath workingDirPath;
		try {
			Repository repo = Activator.getDefault().getRepositoryCache()
					.lookupRepository(repositoryDir);
			if (repo.isBare()) {
				return;
			}
			workingDirPath = new Path(repo.getWorkTree().getAbsolutePath());
		} catch (IOException e) {
			org.eclipse.egit.ui.Activator.logError(e.getLocalizedMessage(), e);
			return;
		}
		Map<IProject, File> connections = new HashMap<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			// Skip closed projects
			if (!project.isAccessible()) {
				continue;
			}
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);
			if (provider != null) {
				continue;
			}
			IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			// In case the project is not inside the working directory, don't
			// even search for a mapping.
			if (!workingDirPath.isPrefixOf(location)) {
				continue;
			}
			RepositoryFinder f = new RepositoryFinder(project);
			f.setFindInChildren(false);
			try {
				List<RepositoryMapping> mappings = f
						.find(new NullProgressMonitor());
				if (!mappings.isEmpty()) {
					// Connect to the first one; it's the innermost.
					IPath gitDir = mappings.get(0).getGitDirAbsolutePath();
					if (gitDir != null) {
						connections.put(project, gitDir.toFile());
					}
				}
			} catch (CoreException e) {
				// Ignore this project in that case
				continue;
			}
		}
		if (!connections.isEmpty()) {
			ConnectProviderOperation operation = new ConnectProviderOperation(
					connections);
			operation.setRefreshResources(false);
			JobUtil.scheduleUserJob(operation,
					CoreText.Activator_AutoShareJobName, JobFamilies.AUTO_SHARE);
		}
	}

	private boolean doAutoShare() {
		IEclipsePreferences d = DefaultScope.INSTANCE.getNode(Activator
				.getPluginId());
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		return p.getBoolean(GitCorePreferences.core_autoShareProjects,
				d.getBoolean(GitCorePreferences.core_autoShareProjects, true));
	}

}
