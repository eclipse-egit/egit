/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.jgit.lib.Repository;

/**
 * {@link IDecoratableResource} implements for a {@link Repository} object
 */
public class DecoratableRepository implements IDecoratableResource {

	private final String name;

	private final boolean dirty;

	private final boolean conflicts;

	private final Staged staged;

	/**
	 * Create decoratable repository
	 *
	 * @param status
	 * @param repository
	 * @throws IOException
	 */
	public DecoratableRepository(IndexDiffData status, Repository repository)
			throws IOException {
		name = DecoratableResourceHelper.getRepositoryName(repository);

		if (status != null) {
			dirty = !status.getModified().isEmpty()
					|| !status.getUntracked().isEmpty();
			conflicts = !status.getConflicting().isEmpty();
			if (!status.getAdded().isEmpty() || !status.getRemoved().isEmpty())
				staged = Staged.MODIFIED;
			else
				staged = Staged.NOT_STAGED;
		} else {
			dirty = false;
			conflicts = false;
			staged = Staged.NOT_STAGED;
		}
	}

	public int getType() {
		return IResource.ROOT;
	}

	public String getName() {
		return name;
	}

	public String getRepositoryName() {
		return name;
	}

	public String getBranch() {
		return null;
	}

	public String getBranchStatus() {
		return null;
	}

	public boolean isTracked() {
		return true;
	}

	public boolean isIgnored() {
		return false;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Staged staged() {
		return staged;
	}

	public boolean hasConflicts() {
		return conflicts;
	}

	public boolean isAssumeValid() {
		return false;
	}
}
