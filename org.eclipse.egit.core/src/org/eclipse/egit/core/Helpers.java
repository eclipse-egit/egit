/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.core.resources.IResource;

/**
 * Common helper functions
 */
public class Helpers {

	private Helpers() {
		// non instantiable helper class
	}

	/**
	 * Determinate does given resource is imported into workspace or not.
	 *
	 * @param resource
	 * @return {@code true} when given resource is not imported into workspace,
	 *         {@code false} otherwise
	 */
	public static boolean isNonWorksapce(IResource resource) {
		return resource.getLocation() == null;
	}

}
