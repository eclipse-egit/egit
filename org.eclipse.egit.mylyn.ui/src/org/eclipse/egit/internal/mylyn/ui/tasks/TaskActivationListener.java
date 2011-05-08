/*******************************************************************************
 * Copyright (c) 2010 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel.doninger@googlemail.com> - Branch checkout/creation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.mylyn.context.core.AbstractContextStructureBridge;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.CompositeContextElement;
import org.eclipse.mylyn.internal.context.core.CompositeInteractionContext;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContext;
import org.eclipse.mylyn.internal.context.core.InteractionContextElement;
import org.eclipse.mylyn.internal.resources.ui.ResourceStructureBridge;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.monitor.core.InteractionEvent.Kind;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivationListener;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class TaskActivationListener implements ITaskActivationListener {

	List<Repository> usedRepositories = new ArrayList<Repository>();

	public void preTaskDeactivated(ITask task) {
		for (Repository repo : usedRepositories) {
			try {
				BranchOperation operation = new BranchOperation(repo,
						Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME
								+ "/" + Constants.MASTER); //$NON-NLS-1$
				operation.execute(null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("restriction")
	public void taskActivated(ITask task) {
		usedRepositories = new ArrayList<Repository>();
		List<RepositoryQuery> queriesContainingActivatedTask = new LinkedList<RepositoryQuery>();
		Collection<RepositoryQuery> allQueries = TasksUiPlugin.getTaskList()
				.getQueries();
		Iterator<RepositoryQuery> it = allQueries.iterator();
		if (it.hasNext()) {
			RepositoryQuery query = it.next();
			Collection<ITask> tasksInQuery = query.getChildren();
			if (tasksInQuery.contains(task))
				queriesContainingActivatedTask.add(query);
		}

		CompositeInteractionContext activeContext = (CompositeInteractionContext) ContextCore
				.getContextManager().getActiveContext();
		List<IInteractionElement> allElements = activeContext.getAllElements();
		List<Repository> repositoriesInContext = new ArrayList<Repository>();
		List<IProject> projectsInContext = new ArrayList<IProject>();

		AbstractContextStructureBridge bridge = ContextCore
				.getStructureBridge(ResourceStructureBridge.CONTENT_TYPE);

		for (IInteractionElement element : allElements) {
			Object o = bridge.getObjectForHandle(element.getHandleIdentifier());
			IResource r = null;
			if (o instanceof IResource)
				r = (IResource) o;
			if (r != null)
				projectsInContext.add(r.getProject());
		}

		for (IProject project : projectsInContext) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (repositoryMapping != null)
				repositoriesInContext.add(repositoryMapping.getRepository());
		}

		String branch = task.getTaskKey() != null ? task.getTaskKey() : task
				.getTaskId();
		String branchFullName = Constants.R_HEADS + branch;

		if (repositoriesInContext.isEmpty()) {
			RepositoryAndBranchSelectionDialog dialog = new RepositoryAndBranchSelectionDialog(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), branchFullName);
			if (dialog.open() == Window.OK) {
				List<Repository> repos = dialog.getSelectedRepositories();
				performBranchCheckout(branch, repos);
			}
		} else
			performBranchCheckout(branch, repositoriesInContext);

		InteractionContext localContext = activeContext.getContextMap().get(
				"local-1"); //$NON-NLS-1$

		List<InteractionContextElement> nodes = new ArrayList<InteractionContextElement>();
		InteractionContextElement node = new InteractionContextElement(
				"meta/setting", "egitbranchsetting", localContext); //$NON-NLS-1$ //$NON-NLS-2$
		nodes.add(node);
		IInteractionElement newElement = new CompositeContextElement(
				"egitbranchsetting", nodes, activeContext.getScaling()); //$NON-NLS-1$

		ContextCorePlugin.getContextManager().processInteractionEvent(
				newElement, Kind.PROPAGATION, InteractionEvent.ID_UNKNOWN,
				activeContext);

	}

	private void performBranchCheckout(String branch, List<Repository> repos) {
		usedRepositories.addAll(repos);
		try {
			for (Repository repo : repos) {
				// Create new branch, if branch with proposed name doesn't
				// exist, otherwise checkout
				if (repo.getRefDatabase().getRef(branch) == null) {
					CreateLocalBranchOperation createOperation = new CreateLocalBranchOperation(
							repo, branch, repo.getRef(Constants.R_HEADS
									+ Constants.MASTER), null);
					createOperation.execute(null);
				}

				BranchOperation operation = new BranchOperation(repo,
						Constants.R_HEADS + branch);
				operation.execute(null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void taskDeactivated(ITask task) {
		// TODO Auto-generated method stub
	}

	public void preTaskActivated(ITask task) {
		// TODO Auto-generated method stub

	}
}
