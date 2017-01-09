/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, 2013 François Rey <eclipse.org_@_francois_._rey_._name>
 * Copyright (C) 2013 Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 * Copyright (C) 2016, Stefan Dirix <sdirix@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * A helper class for Team Actions on Git controlled projects
 */
abstract class RepositoryActionHandler extends AbstractHandler {

	private IEvaluationContext evaluationContext;

	IStructuredSelection mySelection;

	/**
	 * Set the selection when used by {@link RepositoryAction} as
	 * {@link IWorkbenchWindowActionDelegate}
	 *
	 * @param selection
	 *            the new selection
	 */
	public void setSelection(ISelection selection) {
		mySelection = SelectionUtils.getStructuredSelection(selection);
	}

	/**
	 * Retrieve the list of projects that contains the given resources. All
	 * resources must actually map to a project shared with egit, otherwise an
	 * empty array is returned. In case of a linked resource, the project
	 * returned is the one that contains the link target and is shared with
	 * egit, if any, otherwise an empty array is also returned.
	 *
	 * @param selection
	 * @return the projects hosting the selected resources
	 */
	private IProject[] getProjectsForSelectedResources(
			IStructuredSelection selection) {
		Set<IProject> ret = new LinkedHashSet<>();
		for (IResource resource : getSelectedAdaptables(selection,
				IResource.class)) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping != null && (mapping.getContainer() instanceof IProject))
				ret.add((IProject) mapping.getContainer());
			else
				return new IProject[0];
		}
		ret.addAll(extractProjectsFromMappings(selection));

		return ret.toArray(new IProject[ret.size()]);
	}

	private Set<IProject> extractProjectsFromMappings(
			IStructuredSelection selection) {
		Set<IProject> ret = new LinkedHashSet<>();
		for (ResourceMapping mapping : getSelectedAdaptables(selection,
				ResourceMapping.class)) {
			IProject[] mappedProjects = mapping.getProjects();
			if (mappedProjects != null && mappedProjects.length != 0) {
				// Some mappings (WorkingSetResourceMapping) return the projects
				// in unpredictable order. Sort them like the navigator to
				// correspond to the order the user usually sees.
				List<IProject> projects = new ArrayList<>(
						Arrays.asList(mappedProjects));
				Collections
						.sort(projects, CommonUtils.RESOURCE_NAME_COMPARATOR);
				ret.addAll(projects);
			}
		}
		return ret;
	}

	/**
	 * Retrieve the list of projects that contains the selected resources. All
	 * resources must actually map to a project shared with egit, otherwise an
	 * empty array is returned. In case of a linked resource, the project
	 * returned is the one that contains the link target and is shared with
	 * egit, if any, otherwise an empty array is also returned.
	 *
	 * @param event
	 * @return the projects hosting the selected resources
	 * @throws ExecutionException
	 */
	protected IProject[] getProjectsForSelectedResources(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return getProjectsForSelectedResources(selection);
	}

	/**
	 * Retrieve the list of projects that contains the selected resources. All
	 * resources must actually map to a project shared with egit, otherwise an
	 * empty array is returned. In case of a linked resource, the project
	 * returned is the one that contains the link target and is shared with
	 * egit, if any, otherwise an empty array is also returned.
	 *
	 * @return the projects hosting the selected resources
	 */
	protected IProject[] getProjectsForSelectedResources() {
		IStructuredSelection selection = getSelection();
		return getProjectsForSelectedResources(selection);
	}

	/**
	 * @param projects
	 *            a list of projects
	 * @return the repositories that projects map to if all projects are mapped
	 */
	protected Repository[] getRepositoriesFor(final IProject[] projects) {
		Set<Repository> ret = new LinkedHashSet<>();
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
	 * Determines whether the selection contains only resources that are in some
	 * git repository.
	 *
	 * @return {@code true} if all resources in the selection belong to a git
	 *         repository known to EGit.
	 */
	protected boolean haveSelectedResourcesWithRepository() {
		IStructuredSelection selection = getSelection();
		if (selection != null) {
			IResource[] resources = SelectionUtils
					.getSelectedResources(selection);
			if (resources.length > 0) {
				for (IResource resource : resources) {
					if (resource == null
							|| RepositoryMapping.getMapping(resource) == null) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
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
		if (warn) {
			Shell shell = getShell(event);
			return SelectionUtils.getRepositoryOrWarn(selection, shell);
		} else {
			return SelectionUtils.getRepository(selection);
		}
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @return repository for current project, or null
	 */
	protected Repository getRepository() {
		IStructuredSelection selection = getSelection();
		return SelectionUtils.getRepository(selection);
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
		IProject[] selectedProjects = getProjectsForSelectedResources(event);
		if (selectedProjects.length > 0) {
			return getRepositoriesFor(selectedProjects);
		}
		IStructuredSelection selection = getSelection(event);
		if (!selection.isEmpty()) {
			Set<Repository> repos = new LinkedHashSet<>();
			for (Object o : selection.toArray()) {
				Repository repo = AdapterUtils.adapt(o, Repository.class);
				if (repo != null) {
					repos.add(repo);
				}
			}
			return repos.toArray(new Repository[repos.size()]);
		}
		return new Repository[0];
	}

	/**
	 * Get the currently selected repositories. All selected projects must map
	 * to a repository.
	 *
	 * @return repositories for selection, or an empty array
	 */
	public Repository[] getRepositories() {
		IProject[] selectedProjects = getProjectsForSelectedResources();
		if (selectedProjects.length > 0)
			return getRepositoriesFor(selectedProjects);
		IStructuredSelection selection = getSelection();
		if (!selection.isEmpty()) {
			Set<Repository> repos = new LinkedHashSet<>();
			for (Object o : selection.toArray()) {
				Repository repo = AdapterUtils.adapt(o, Repository.class);
				if (repo != null) {
					repos.add(repo);
				} else {
					// no repository found for one of the objects!
					return new Repository[0];
				}
			}
			return repos.toArray(new Repository[repos.size()]);
		}
		return new Repository[0];
	}

	/**
	 * @param event
	 *            the execution event, must not be null
	 * @return the current selection
	 * @throws ExecutionException
	 *             if the selection can't be determined
	 */
	protected static IStructuredSelection getSelection(ExecutionEvent event)
			throws ExecutionException {
		if (event == null)
			throw new IllegalArgumentException("event must not be NULL"); //$NON-NLS-1$
		Object context = event.getApplicationContext();
		if (context instanceof IEvaluationContext)
			return SelectionUtils.getSelection((IEvaluationContext) context);
		return StructuredSelection.EMPTY;
	}

	/**
	 * @return the current selection
	 */
	protected IStructuredSelection getSelection() {
		// if the selection was set explicitly, use it
		if (mySelection != null)
			return mySelection;
		return SelectionUtils.getSelection(evaluationContext);
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		this.evaluationContext = (IEvaluationContext) evaluationContext;
	}

	/**
	 * Creates an array of the given class type containing all the objects in
	 * the selection that adapt to the given class.
	 *
	 * @param selection
	 * @param c
	 * @return the selected adaptables
	 */
	private <T> List<T> getSelectedAdaptables(ISelection selection,
			Class<T> c) {
		List<T> result;
		if (selection != null && !selection.isEmpty()) {
			result = new ArrayList<>();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				T adapter = AdapterUtils.adapt(elements.next(), c);
				if (adapter != null) {
					result.add(adapter);
				}
			}
		} else {
			result = Collections.emptyList();
		}
		return result;
	}

	/**
	 * @param event
	 * @return the resources in the selection
	 * @throws ExecutionException
	 */
	protected IResource[] getSelectedResources(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return SelectionUtils.getSelectedResources(selection);
	}

	protected IPath[] getSelectedLocations(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		return SelectionUtils.getSelectedLocations(selection);
	}

	/**
	 * @return the resources in the selection
	 */
	protected IResource[] getSelectedResources() {
		IStructuredSelection selection = getSelection();
		return SelectionUtils.getSelectedResources(selection);
	}

	/**
	 * @return the locations in the selection
	 */
	protected IPath[] getSelectedLocations() {
		IStructuredSelection selection = getSelection();
		return SelectionUtils.getSelectedLocations(selection);
	}

	/**
	 * @return true if all selected items map to the same repository, false otherwise.
	 */
	protected boolean selectionMapsToSingleRepository() {
		return getRepository() != null;
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
	 *
	 * @param repository
	 *            the repository to check
	 * @return {@code true} when {@link Constants#HEAD} can be resolved,
	 *         {@code false} otherwise
	 */
	protected boolean containsHead(Repository repository) {
		try {
			return repository != null ? repository.resolve(Constants.HEAD) != null
					: false;
		} catch (Exception e) {
			// do nothing
		}

		return false;
	}

	protected boolean isLocalBranchCheckedout(Repository repository) {
		try {
			String fullBranch = repository.getFullBranch();
			return fullBranch != null
					&& fullBranch.startsWith(Constants.R_HEADS);
		} catch (Exception e) {
			// do nothing
		}

		return false;
	}

	protected String getPreviousPath(Repository repository,
			ObjectReader reader, RevCommit headCommit,
			RevCommit previousCommit, String path) throws IOException {
		DiffEntry diffEntry = CompareCoreUtils.getChangeDiffEntry(repository, path,
				headCommit, previousCommit, reader);
		if (diffEntry != null)
			return diffEntry.getOldPath();
		else
			return path;
	}

	protected RevCommit getHeadCommit(IResource resource) throws IOException {
		Repository repository = getRepository();
		if (resource == null) {
			return null;
		}
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping == null) {
			return null;
		}
		String path = mapping.getRepoRelativePath(resource);
		if (path == null) {
			return null;
		}
		try (RevWalk rw = new RevWalk(repository)) {
			rw.sort(RevSort.COMMIT_TIME_DESC, true);
			rw.sort(RevSort.BOUNDARY, true);
			if (path.length() > 0) {
				DiffConfig diffConfig = repository.getConfig().get(
						DiffConfig.KEY);
				FollowFilter filter = FollowFilter.create(path, diffConfig);
				rw.setTreeFilter(filter);
			}

			Ref head = repository.findRef(Constants.HEAD);
			if (head == null) {
				return null;
			}
			RevCommit headCommit = rw.parseCommit(head.getObjectId());
			rw.close();
			return headCommit;
		}
	}

	/**
	 * Returns the previous commit of the given resources.
	 *
	 * @param resources
	 *            The {@link IResource} for which the previous commit shall be
	 *            determined.
	 * @return The second to last commit which touched any of the given
	 *         resources.
	 * @throws IOException
	 *             When the commit can not be parsed.
	 */
	protected List<RevCommit> findPreviousCommits(
			Collection<IResource> resources) throws IOException {
		List<RevCommit> result = new ArrayList<>();
		Repository repository = getRepository();
		RepositoryMapping mapping = RepositoryMapping.getMapping(resources
				.iterator().next()
				.getProject());
		if (mapping == null) {
			return result;
		}
		try (RevWalk rw = new RevWalk(repository)) {
			rw.sort(RevSort.COMMIT_TIME_DESC, true);
			rw.sort(RevSort.BOUNDARY, true);

			List<TreeFilter> filters = new ArrayList<>();
			DiffConfig diffConfig = repository.getConfig().get(DiffConfig.KEY);
			for (IResource resource : resources) {
				String path = mapping.getRepoRelativePath(resource);

				if (path != null && path.length() > 0) {
					filters.add(FollowFilter.create(path, diffConfig));
				}
			}

			if (filters.size() >= 2) {
				TreeFilter filter = OrTreeFilter.create(filters);
				rw.setTreeFilter(filter);
			} else if (filters.size() == 1) {
				rw.setTreeFilter(filters.get(0));
			}

			Ref head = repository.findRef(Constants.HEAD);
			if (head == null) {
				return result;
			}
			RevCommit headCommit = rw.parseCommit(head.getObjectId());
			rw.markStart(headCommit);
			headCommit = rw.next();

			if (headCommit == null)
				return result;
			List<RevCommit> directParents = Arrays.asList(headCommit
					.getParents());

			RevCommit previousCommit = rw.next();
			while (previousCommit != null && result.size() < directParents.size()) {
				if (directParents.contains(previousCommit)) {
					result.add(previousCommit);
				}
				previousCommit = rw.next();
			}
			rw.dispose();
		}
		return result;
	}

	// keep track of the path of an ancestor (for following renames)
	protected static final class PreviousCommit {
		final RevCommit commit;
		final String path;
		PreviousCommit(final RevCommit commit, final String path) {
			this.commit = commit;
			this.path = path;
		}
	}

	/**
	 * By default egit operates only on resources that map to a project shared
	 * with egit. For linked resources the project that contains the link
	 * target, if any, must be shared with egit.
	 *
	 * @return the projects hosting the selected resources
	 */
	@Override
	public boolean isEnabled() {
		return getProjectsForSelectedResources().length > 0;
	}

	/**
	 * Determines whether the enablement state shall always be recomputed or
	 * only when the selection changes. This default implementation returns
	 * {@code false}.
	 *
	 * @return {@code false} if the enablement state depends solely on the
	 *         selection, {@code true} if the enablement must be recomputed even
	 *         if the selection did not change.
	 */
	protected boolean alwaysCheckEnabled() {
		return false;
	}
}
