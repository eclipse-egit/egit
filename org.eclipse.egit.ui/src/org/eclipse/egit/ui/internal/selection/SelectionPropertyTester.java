/*******************************************************************************
 * Copyright (C) 2014, 2015 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.ArrayList;
import java.util.Collection;

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
			IResource resource = AdapterUtils.adapt(firstElement,
					IResource.class);
			if ((resource != null) && (resource instanceof IFile
					|| resource instanceof IFolder)) {
				RepositoryMapping m = RepositoryMapping.getMapping(resource);
				if (m != null) {
					return testRepositoryProperties(m.getRepository(), args);
				}
			}
		} else if ("resourcesAllInRepository".equals(property)) { //$NON-NLS-1$
			IStructuredSelection selection = getStructuredSelection(collection);

			IResource[] resources = SelectionUtils
					.getSelectedResources(selection);
			return haveRepositories(resources);
		}
		return false;
	}

	private static IStructuredSelection getStructuredSelection(
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
			IProject project = AdapterUtils.adapt(element, IProject.class);
			if (project != null) {
				Repository r = getRepositoryOfMapping(project);
				if (single && r != null && repo != null && r != repo)
					return null;
				else if (r != null)
					repo = r;
			} else {
				IContainer container = AdapterUtils.adapt(element, IContainer.class);
				RepositoryMapping mapping = null;
				if (container != null) {
					mapping = RepositoryMapping.getMapping(container);
				}
				if (container != null && mapping != null
						&& container.equals(mapping.getContainer())) {
					Repository r = mapping.getRepository();
					if (single && r != null && repo != null && r != repo)
						return null;
					else if (r != null)
						repo = r;
				} else {
					IWorkingSet workingSet = AdapterUtils.adapt(element,
							IWorkingSet.class);
					if (workingSet != null) {
						for (IAdaptable adaptable : workingSet.getElements()) {
							Repository r = getRepositoryOfProject(adaptable);
							if (single && r != null && repo != null && r != repo)
								return null;
							else if (r != null)
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
	 * @return {@code true} when all {@code resources} map to a repository,
	 *         {@code false} otherwise.
	 */
	private static boolean haveRepositories(IResource[] resources) {
		for (IResource resource : resources) {
			Repository r = getRepositoryOfMapping(resource);
			if (r == null) {
				return false;
			}
		}
		return true;
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
