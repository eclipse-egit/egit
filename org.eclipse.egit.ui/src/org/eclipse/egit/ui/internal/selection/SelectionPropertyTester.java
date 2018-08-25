/*******************************************************************************
 * Copyright (C) 2014, 2015 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Bossert <anb0s@anbos.de> - Bug 496356
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkingSet;

/**
 * Property tester for whole selections.
 */
public class SelectionPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		Collection<?> collection = (Collection<?>) receiver;
		if (collection.isEmpty())
			return false;
		if ("projectsSingleRepository".equals(property)) { //$NON-NLS-1$

			Repository repository = getRepositoryOfProjects(collection, true);
			return testRepositoryProperties(repository, args);

		} else if ("projectsWithRepositories".equals(property)) { //$NON-NLS-1$
			Repository repository = getRepositoryOfProjects(collection, false);
			return repository != null;

		} else if ("selectionSingleRepository".equals(property)) { //$NON-NLS-1$
			return SelectionUtils
					.getRepository(getStructuredSelection(collection)) != null;

		} else if ("resourcesMultipleRepositories".equals(property)) { //$NON-NLS-1$
			return resourceSelectionContainsMoreThanOneRepository(collection,
					args);

		} else if ("selectionMultipleRepositories".equals(property)) { //$NON-NLS-1$
			return selectionContainsMoreThanOneRepository(collection,
					args);

		} else if ("resourcesSingleRepository".equals(property)) { //$NON-NLS-1$
			IStructuredSelection selection = getStructuredSelection(collection);

			// It may seem like we could just use SelectionUtils.getRepository
			// here. The problem: It would also return a repository for a node
			// in the repo view. But this property is just for resources.
			IResource[] resources = SelectionUtils
					.getSelectedResources(selection);
			Repository repository = getRepositoryOfResources(resources);
			return testRepositoryProperties(repository, args);

		} else if ("fileOrFolderInRepository".equals(property)) { //$NON-NLS-1$
			if (collection.size() != 1)
				return false;

			IStructuredSelection selection = getStructuredSelection(collection);
			if (selection.size() != 1)
				return false;

			Object firstElement = selection.getFirstElement();

			IResource resource = AdapterUtils.adaptToAnyResource(firstElement);
			if ((resource != null) && (resource instanceof IFile
					|| resource instanceof IFolder)) {
				RepositoryMapping m = RepositoryMapping.getMapping(resource);
				if (m != null) {
					if ((resource instanceof IFolder)
							&& resource.equals(m.getContainer())) {
						return false;
					} else {
						return testRepositoryProperties(m.getRepository(),
								args);
					}
				}
			}
		} else if ("resourcesAllInRepository".equals(property)) { //$NON-NLS-1$
			IStructuredSelection selection = getStructuredSelection(collection);

			IResource[] resources = SelectionUtils
					.getSelectedResources(selection);
			Collection<Repository> repositories = getRepositories(resources);
			if (repositories.isEmpty()) {
				return false;
			}
			if (args != null && args.length > 0) {
				for (Repository repository : repositories) {
					if (!testRepositoryProperties(repository, args)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	private boolean resourceSelectionContainsMoreThanOneRepository(
			Collection<?> collection, Object[] args) {
		IStructuredSelection selection = getStructuredSelection(collection);
		IResource[] resources = SelectionUtils.getSelectedResources(selection);
		Set<Repository> repos = Stream.of(resources) //
				.map(SelectionPropertyTester::getRepositoryOfMapping) //
				.collect(Collectors.toSet());
		return testMultipleRepositoryProperties(repos, args);
	}

	private boolean selectionContainsMoreThanOneRepository(
			Collection<?> collection, Object[] args) {
		IStructuredSelection selection = getStructuredSelection(collection);
		Repository[] repos = SelectionUtils.getRepositories(selection);
		return testMultipleRepositoryProperties(Arrays.asList(repos), args);
	}

	private boolean testMultipleRepositoryProperties(
			Collection<Repository> repos, Object[] args) {
		if (repos.size() < 2)
			return false;

		return repos.stream().allMatch(
				r -> SelectionPropertyTester.testRepositoryProperties(r, args));
	}

	private static @NonNull IStructuredSelection getStructuredSelection(
			Collection<?> collection) {
		Object firstElement = collection.iterator().next();
		if (collection.size() == 1 && firstElement instanceof ITextSelection)
			return SelectionUtils
					.getStructuredSelection((ITextSelection) firstElement);
		else
			return new StructuredSelection(new ArrayList<>(collection));
	}

	private static boolean testRepositoryProperties(Repository repository,
			Object[] properties) {
		if (repository == null)
			return false;

		for (Object arg : properties) {
			String s = (String) arg;
			if (!ResourcePropertyTester.testRepositoryState(repository, s))
				return false;
		}
		return true;
	}

	/**
	 * @param collection
	 *            the selected elements
	 * @param single
	 *            <code>true</code> if only a single repository is allowed
	 * @return the repository if any was found, <code>null</code> otherwise
	 */
	private static Repository getRepositoryOfProjects(Collection<?> collection,
			boolean single) {
		Repository repo = null;
		for (Object element : collection) {
			IContainer container = AdapterUtils.adapt(element,
					IProject.class);
			RepositoryMapping mapping = null;
			if (container != null) {
				mapping = RepositoryMapping.getMapping(container);
			} else {
				container = AdapterUtils.adapt(element, IContainer.class);
				if (container != null) {
					mapping = RepositoryMapping.getMapping(container);
				}
			}
			if (container != null && mapping != null
					&& container.equals(mapping.getContainer())) {
				Repository r = mapping.getRepository();
				if (single && r != null && repo != null && r != repo) {
					return null;
				} else if (r != null) {
					repo = r;
				}
			} else {
				IWorkingSet workingSet = AdapterUtils.adapt(element,
						IWorkingSet.class);
				if (workingSet != null) {
					for (IAdaptable adaptable : workingSet.getElements()) {
						Repository r = getRepositoryOfProject(adaptable);
						if (single && r != null && repo != null && r != repo) {
							return null;
						} else if (r != null) {
							repo = r;
						}
					}
				}
			}
		}
		return repo;
	}

	/**
	 * @param resources
	 *            the resources
	 * @return the repository that all the mapped resources map to,
	 *         <code>null</code> otherwise
	 */
	private static Repository getRepositoryOfResources(IResource[] resources) {
		Repository repo = null;
		for (IResource resource : resources) {
			Repository r = getRepositoryOfMapping(resource);
			if (r != null && repo != null && r != repo)
				return null;
			else if (r != null)
				repo = r;
		}
		return repo;
	}

	/**
	 * @param resources
	 *            the resources
	 * @return a collection containing all the repositories the given resources
	 *         belong to, or an empty collection if a resource is not in a git
	 *         repository
	 */
	private static Collection<Repository> getRepositories(
			IResource[] resources) {
		Set<Repository> result = new HashSet<>();
		for (IResource resource : resources) {
			Repository r = getRepositoryOfMapping(resource);
			if (r == null) {
				return Collections.emptySet();
			}
			result.add(r);
		}
		return result;
	}

	private static Repository getRepositoryOfProject(Object object) {
		IProject project = AdapterUtils.adapt(object, IProject.class);
		if (project != null)
			return getRepositoryOfMapping(project);
		return null;
	}

	private static Repository getRepositoryOfMapping(IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping != null)
			return mapping.getRepository();
		return null;
	}
}
