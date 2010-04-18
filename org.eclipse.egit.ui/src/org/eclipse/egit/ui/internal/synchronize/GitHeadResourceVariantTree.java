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
package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.Constants;

class GitHeadResourceVariantTree extends GitResourceVariantTree {

	GitHeadResourceVariantTree(IResource[] roots) {
		super(roots);
	}

	@Override
	String getRevString(IResource resource) {
		return Constants.HEAD;
	}

}
