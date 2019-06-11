/******************************************************************************
 *  Copyright (c) 2011, 2015 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jgit.annotations.Nullable;

/**
 * Utilities for working with objects that implement {@link IAdaptable}
 */
public class AdapterUtils {

	private AdapterUtils() {
		// Cannot be instantiated
	}

	/**
	 * Adapt object to one interface from list: {@link IResource},
	 * {@link IContainer}, {@link IFile} or {@link IProject}.
	 *
	 * @param object
	 * @return adapted resource
	 */
	@Nullable
	public static IResource adaptToAnyResource(Object object) {
		if (object == null) {
			return null;
		}
		IResource resource = Adapters.adapt(object, IResource.class);
		if (resource != null) {
			return resource;
		}
		resource = Adapters.adapt(object, IFile.class);
		if (resource != null) {
			return resource;
		}
		resource = Adapters.adapt(object, IProject.class);
		if (resource != null) {
			return resource;
		}
		resource = Adapters.adapt(object, IContainer.class);
		if (resource != null) {
			return resource;
		}
		return null;
	}
}
