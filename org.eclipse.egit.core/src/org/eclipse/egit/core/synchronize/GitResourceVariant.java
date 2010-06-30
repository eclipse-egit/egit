/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.egit.core.resource.GitResource;
import org.eclipse.team.core.variants.IResourceVariant;

abstract class GitResourceVariant implements IResourceVariant {

	private GitResource resource;

	GitResourceVariant(GitResource resource) {
		this.resource = resource;
	}

	public GitResource getResource() {
		return resource;
	}

	public String getName() {
		String name = resource.getName();
		return name != null ? name : ""; //$NON-NLS-1$
	}

}
