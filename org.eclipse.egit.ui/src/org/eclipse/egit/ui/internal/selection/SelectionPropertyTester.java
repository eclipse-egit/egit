/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.Collection;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkingSet;

/**
 * Property tester for whole selections.
 */
public class SelectionPropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		Collection<?> collection = (Collection<?>) receiver;
		if ("projectSingleRepository".equals(property)) { //$NON-NLS-1$
			if (collection.size() != 1)
				return false;

			Repository repository = getRepository(collection, true);
			if (repository == null)
				return false;

			for (Object arg : args) {
				String s = (String) arg;
				if (!ResourcePropertyTester.testRepositoryState(repository, s))
					return false;
			}
			return true;

		} else if ("projectsWithRepositories".equals(property)) { //$NON-NLS-1$
			Repository repository = getRepository(collection, false);
			return repository != null;
		}
		return false;
	}

	/**
	 * @param collection
	 *            the selected elements
	 * @param single
	 *            <code>true</code> if only a single repository is allowed
	 * @return the repository if any was found, <code>null</code> otherwise
	 */
	private static Repository getRepository(Collection<?> collection,
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
		return repo;
	}

	private static Repository getRepositoryOfMapping(IProject project) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping != null)
			return mapping.getRepository();
		return null;
	}

	private static Repository getRepositoryOfProject(Object object) {
		IProject project = AdapterUtils.adapt(object, IProject.class);
		if (project != null)
			return getRepositoryOfMapping(project);
		return null;
	}

}
