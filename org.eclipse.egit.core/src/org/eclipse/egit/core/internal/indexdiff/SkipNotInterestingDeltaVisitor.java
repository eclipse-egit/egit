/*******************************************************************************
 * Copyright (C) 2016, Andrey Rodionov <rodionovamp@mail.ru>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * While analyzing ResourceDelta, try to determine if delta contains at least 1
 * interesting change. If not, short-cut {@link GitResourceDeltaVisitor}
 */
class SkipNotInterestingDeltaVisitor implements IResourceDeltaVisitor {

	private boolean atLeastOneInterestingDelta = false;

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (atLeastOneInterestingDelta) {
			// Skip further iterating. root delta has at least one interesting
			// change
			return false;
		}
		final IResource resource = delta.getResource();
		if (resource.getType() == IResource.ROOT) {
			return true;
		}

		if (resource.getType() == IResource.PROJECT
				&& (delta.getKind() == IResourceDelta.REMOVED)) {
			atLeastOneInterestingDelta = true;
			return false;
		}

		if (resource.getType() == IResource.FOLDER
				&& delta.getKind() == IResourceDelta.ADDED) {
			atLeastOneInterestingDelta = true;
			return false;
		}

		if (GitResourceDeltaVisitor.isInteresting(delta)) {
			atLeastOneInterestingDelta = true;
			return false;
		}

		// no interesting changes found. continue iterating over delta
		return true;
	}

	public boolean hasAtLeastOneInterestingDelta() {
		return atLeastOneInterestingDelta;
	}

}
