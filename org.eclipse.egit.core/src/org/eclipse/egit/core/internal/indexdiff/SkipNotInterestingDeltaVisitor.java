package org.eclipse.egit.core.internal.indexdiff;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * while analyzing ResourceDelta, try to determine if delta contains at least 1
 * interesting change. If not, short-cut {@link GitResourceDeltaVisitor}
 *
 * @author rodionovamp@mail.ru
 *
 */
class SkipNotInterestingDeltaVisitor implements IResourceDeltaVisitor {

	private boolean atLeastOneInterestingDelta = false;

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		final IResource resource = delta.getResource();
		if (resource.getType() == IResource.ROOT
				|| resource.getType() == IResource.PROJECT
				|| resource.getType() == IResource.FOLDER) {
			return true;
		}

		if (!GitResourceDeltaVisitor.isInteresting(delta)) {
			return false;
		}

		if (!atLeastOneInterestingDelta) {
			atLeastOneInterestingDelta = true;
		}

		return true;
	}

	public boolean hasAtLeastOneInterestingDelta() {
		return atLeastOneInterestingDelta;
	}

}
