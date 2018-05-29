/*******************************************************************************
 * Copyright (C) 2016, Andrey Rodionov <rodionovamp@mail.ru>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

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
			// Skip further iterating. Root delta has at least one interesting
			// change
			return false;
		}

		if (GitResourceDeltaVisitor.isInteresting(delta)) {
			atLeastOneInterestingDelta = true;
			return false;
		}

		// No interesting changes found. Continue iterating over delta
		return true;
	}

	public boolean hasAtLeastOneInterestingDelta() {
		return atLeastOneInterestingDelta;
	}

}
