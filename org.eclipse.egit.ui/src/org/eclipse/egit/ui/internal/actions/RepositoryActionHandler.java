/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * A helper class for Team Actions on Git controlled projects
 */
abstract class RepositoryActionHandler extends AbstractHandler {
	private IEvaluationContext evaluationContext;

	private IStructuredSelection mySelection;

	/**
	 * Set the selection when used by {@link RepositoryAction} as
	 * {@link IWorkbenchWindowActionDelegate}
	 *
	 * @param selection
	 *            the new selection
	 */
	public void setSelection(ISelection selection) {
		mySelection = convertSelection(null, selection);
	}

	/**
	 * @param selection
	 * @return the projects hosting the selected resources
	 */
	private IProject[] getProjectsForSelectedResources(
			IStructuredSelection selection) {
		Set<IProject> ret = new HashSet<IProject>();
		for (IResource resource : (IResource[]) getSelectedAdaptables(
				selection, IResource.class))
			ret.add(resource.getProject());
		ret.addAll(extractProjectsFromMappings(selection));

		return ret.toArray(new IProject[ret.size()]);
	}

	private Set<IProject> extractProjectsFromMappings(IStructuredSelection selection) {
		Set<IProject> ret = new HashSet<IProject>();
		for (ResourceMapping mapping : (ResourceMapping[]) getSelectedAdaptables(
				selection, ResourceMapping.class)) {
			IProject[] projects = mapping.getProjects();
			ret.addAll(Arrays.asList(projects));
		}
		return ret;
	}

	/**
	 * @param event
	 * @return the projects hosting the selected resources
	 * @throws ExecutionException
	 */
	protected IProject[] getProjectsForSelectedResources(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return getProjectsForSelectedResources(selection);
	}

	protected IProject[] getProjectsForSelectedResources() {
		IStructuredSelection selection = getSelection();
		return getProjectsForSelectedResources(selection);
	}

	/**
	 * @param projects
	 *            a list of projects
	 * @return the repositories that projects map to iff all projects are mapped
	 */
	protected Repository[] getRepositoriesFor(final IProject[] projects) {
		Set<Repository> ret = new HashSet<Repository>();
		for (IProject project : projects) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (repositoryMapping == null)
				return new Repository[0];
			ret.add(repositoryMapping.getRepository());
		}
		return ret.toArray(new Repository[ret.size()]);
	}

	/**
	 * List the projects with selected resources, if all projects are connected
	 * to a Git repository.
	 *
	 * @param event
	 *
	 * @return the tracked projects affected by the current resource selection
	 * @throws ExecutionException
	 */
	protected IProject[] getProjectsInRepositoryOfSelectedResources(
			ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return getProjectsInRepositoryOfSelectedResources(selection);
	}

	/**
	 * List the projects with selected resources, if all projects are connected
	 * to a Git repository.
	 *
	 * @return the tracked projects affected by the current resource selection
	 */
	protected IProject[] getProjectsInRepositoryOfSelectedResources() {
		IStructuredSelection selection = getSelection();
		return getProjectsInRepositoryOfSelectedResources(selection);
	}

	/**
	 * List the projects with selected resources, if all projects are connected
	 * to a Git repository.
	 *
	 * @param selection
	 *
	 * @return the tracked projects affected by the current resource selection
	 */
	private IProject[] getProjectsInRepositoryOfSelectedResources(
			IStructuredSelection selection) {
		Set<IProject> ret = new HashSet<IProject>();
		Repository[] repositories = getRepositoriesFor(getProjectsForSelectedResources(selection));
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			for (Repository repository : repositories) {
				if (mapping != null && mapping.getRepository() == repository) {
					ret.add(project);
					break;
				}
			}
		}
		return ret.toArray(new IProject[ret.size()]);
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @param warn
	 *            Put up a message dialog to warn why a resource was not
	 *            selected
	 * @param event
	 * @return repository for current project, or null
	 * @throws ExecutionException
	 */
	protected Repository getRepository(boolean warn, ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		Shell shell = getShell(event);
		return getRepository(warn, selection, shell);
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @return repository for current project, or null
	 */
	protected Repository getRepository() {
		IStructuredSelection selection = getSelection();
		return getRepository(false, selection, null);
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @param warn
	 *            Put up a message dialog to warn why a resource was not
	 *            selected
	 * @param selection
	 * @param shell
	 *            must be provided if warn = true
	 * @return repository for current project, or null
	 */
	private Repository getRepository(boolean warn,
			IStructuredSelection selection, Shell shell) {
		RepositoryMapping mapping = null;
		for (IProject project : getProjectsForSelectedResources(selection)) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (mapping == null)
				mapping = repositoryMapping;
			if (repositoryMapping == null)
				return null;
			if (mapping.getRepository() != repositoryMapping.getRepository()) {
				if (warn)
					MessageDialog.openError(shell,
							UIText.RepositoryAction_multiRepoSelectionTitle,
							UIText.RepositoryAction_multiRepoSelection);
				return null;
			}
		}
		if (mapping == null) {
			if (warn)
				MessageDialog.openError(shell,
						UIText.RepositoryAction_errorFindingRepoTitle,
						UIText.RepositoryAction_errorFindingRepo);
			return null;
		}

		final Repository repository = mapping.getRepository();
		return repository;
	}

	/**
	 * Figure out which repositories to use. All selected resources must map to
	 * a Git repository.
	 *
	 * @param event
	 *
	 * @return repositories for selection, or an empty array
	 * @throws ExecutionException
	 */
	protected Repository[] getRepositories(ExecutionEvent event)
			throws ExecutionException {
		IProject[] selectedProjects = getSelectedProjects(event);
		return getRepositoriesFor(selectedProjects);
	}

	/**
	 * Get the currently selected repositories. All selected projects must map
	 * to a repository.
	 *
	 * @return repositories for selection, or an empty array
	 */
	protected Repository[] getRepositories() {
		IProject[] selectedProjects = getSelectedProjects(getSelection());
		return getRepositoriesFor(selectedProjects);
	}

	/**
	 * @param event
	 *            the execution event, must not be null
	 * @return the current selection
	 * @throws ExecutionException
	 *             if the selection can't be determined
	 */
	protected IStructuredSelection getSelection(ExecutionEvent event)
			throws ExecutionException {
		if (event == null)
			throw new IllegalArgumentException("event must not be NULL"); //$NON-NLS-1$
		Object selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection == null)
			selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof TextSelection) {
			IResource resource = ResourceUtil.getResource(HandlerUtil
					.getVariable(event, ISources.ACTIVE_EDITOR_INPUT_NAME));
			if (resource != null)
				return new StructuredSelection(resource);
		}
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		return StructuredSelection.EMPTY;
	}

	/**
	 * @return the current selection
	 */
	protected IStructuredSelection getSelection() {
		// if the selection was set explicitly, use it
		if (mySelection != null)
			return mySelection;
		return convertSelection(evaluationContext, null);
	}

	private IStructuredSelection convertSelection(IEvaluationContext aContext,
			Object aSelection) {
		IEvaluationContext ctx;
		if (aContext == null && aSelection == null)
			return StructuredSelection.EMPTY;
		else
			ctx = aContext;
		Object selection;
		if (aSelection == null && ctx != null) {
			selection = ctx.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
			if (selection == null)
				selection = ctx
						.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		} else if (aSelection != null)
			selection = aSelection;
		else
			return StructuredSelection.EMPTY;
		if (selection instanceof TextSelection) {
			if (ctx == null)
				ctx = getEvaluationContext();
			IResource resource = ResourceUtil.getResource(ctx
					.getVariable(ISources.ACTIVE_EDITOR_INPUT_NAME));
			if (resource != null)
				return new StructuredSelection(resource);
		}
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		return StructuredSelection.EMPTY;
	}

	public void setEnabled(Object evaluationContext) {
		this.evaluationContext = (IEvaluationContext) evaluationContext;
	}

	private IEvaluationContext getEvaluationContext() {
		IEvaluationContext ctx;
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		// no active window during Eclipse shutdown
		if (activeWorkbenchWindow == null)
			return null;
		IHandlerService hsr = (IHandlerService) activeWorkbenchWindow
				.getService(IHandlerService.class);
		ctx = hsr.getCurrentState();
		return ctx;
	}

	/**
	 * Creates an array of the given class type containing all the objects in
	 * the selection that adapt to the given class.
	 *
	 * @param selection
	 * @param c
	 * @return the selected adaptables
	 */
	@SuppressWarnings("unchecked")
	private Object[] getSelectedAdaptables(ISelection selection, Class c) {
		ArrayList result = null;
		if (selection != null && !selection.isEmpty()) {
			result = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object adapter = getAdapter(elements.next(), c);
				if (c.isInstance(adapter)) {
					result.add(adapter);
				}
			}
		}
		if (result != null && !result.isEmpty()) {
			return result.toArray((Object[]) Array
					.newInstance(c, result.size()));
		}
		return (Object[]) Array.newInstance(c, 0);
	}

	private Object getAdapter(Object adaptable, Class c) {
		if (c.isInstance(adaptable)) {
			return adaptable;
		}
		if (adaptable instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) adaptable;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter)) {
				return adapter;
			}
		}
		return null;
	}

	private IProject[] getSelectedProjects(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return getSelectedProjects(selection);
	}

	private IProject[] getSelectedProjects(IStructuredSelection selection) {
		IResource[] selectedResources = getSelectedResources(selection);
		if (selectedResources.length == 0)
			return new IProject[0];
		ArrayList<IProject> projects = new ArrayList<IProject>();
		for (int i = 0; i < selectedResources.length; i++) {
			IResource resource = selectedResources[i];
			if (resource.getType() == IResource.PROJECT) {
				projects.add((IProject) resource);
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * @param event
	 * @return the resources in the selection
	 * @throws ExecutionException
	 */
	protected IResource[] getSelectedResources(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return getSelectedResources(selection);
	}

	/**
	 * @return the resources in the selection
	 */
	protected IResource[] getSelectedResources() {
		IStructuredSelection selection = getSelection();
		return getSelectedResources(selection);
	}

	/**
	 * @return true if selection contains one or more linked resources, false otherwise.
	 */
	protected boolean selectionContainsLinkedResources() {
		IResource[] selectedResources = getSelectedResources();
		for (IResource res: selectedResources)
			if (res.isLinked(IResource.CHECK_ANCESTORS))
				return true;
		return false;
	}

	/**
	 * @param selection
	 * @return the resources in the selection
	 */
	private IResource[] getSelectedResources(IStructuredSelection selection) {
		Set<IResource> result = new HashSet<IResource>();
		for (Object o : selection.toList()) {
			IResource resource = (IResource) getAdapter(o, IResource.class);
			if (resource != null)
				result.add(resource);
			else
				extractResourcesFromMapping(result, o);
		}
		return result.toArray(new IResource[result.size()]);
	}

	private void extractResourcesFromMapping(Set<IResource> result, Object o) {
		ResourceMapping mapping = (ResourceMapping) getAdapter(o, ResourceMapping.class);
		if (mapping != null) {
			ResourceTraversal[] traversals;
			try {
				traversals = mapping.getTraversals(null, null);
				for (ResourceTraversal traversal : traversals) {
					IResource[] resources = traversal.getResources();
					result.addAll(Arrays.asList(resources));
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param event
	 * @return the shell
	 * @throws ExecutionException
	 */
	protected Shell getShell(ExecutionEvent event) throws ExecutionException {
		return HandlerUtil.getActiveShellChecked(event);
	}

	/**
	 * @param event
	 * @return the page
	 * @throws ExecutionException
	 */
	protected IWorkbenchPage getPartPage(ExecutionEvent event)
			throws ExecutionException {
		return getPart(event).getSite().getPage();
	}

	/**
	 * @param event
	 * @return the page
	 * @throws ExecutionException
	 */
	protected IWorkbenchPart getPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}

	/**
	 * Checks if merge is possible:
	 * <ul>
	 * <li>HEAD must point to a branch</li>
	 * <li>Repository State must be SAFE</li>
	 * </ul>
	 *
	 * @param repository
	 * @param event
	 * @return a boolean indicating if merge is possible
	 * @throws ExecutionException
	 */
	protected boolean canMerge(final Repository repository, ExecutionEvent event)
			throws ExecutionException {
		String message = null;
		try {
			Ref head = repository.getRef(Constants.HEAD);
			if (head == null || !head.isSymbolic())
				message = UIText.MergeAction_HeadIsNoBranch;
			else if (!repository.getRepositoryState().equals(
					RepositoryState.SAFE))
				message = NLS.bind(UIText.MergeAction_WrongRepositoryState,
						repository.getRepositoryState());
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
			message = e.getMessage();
		}

		if (message != null) {
			MessageDialog.openError(getShell(event),
					UIText.MergeAction_CannotMerge, message);
		}
		return (message == null);
	}

	/**
	 *
	 * @return {@code true} when {@link Constants#HEAD} can be resolved,
	 *         {@code false} otherwise
	 */
	protected boolean containsHead() {
		try {
			return getRepository().resolve(Constants.HEAD) != null;
		} catch (Exception e) {
			// do nothing
		}

		return false;
	}
}
