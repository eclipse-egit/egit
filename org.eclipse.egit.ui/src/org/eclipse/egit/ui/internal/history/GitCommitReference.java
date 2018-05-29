/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Represents an {@link IRegion} in some text that references a
 * {@link RevCommit}.
 */
public class GitCommitReference {

	private final RevCommit target;

	private final IRegion region;

	/**
	 * Creates a new instance.
	 *
	 * @param target
	 *            the {@link RevCommit} referenced
	 * @param region
	 *            the {@link IRegion} of the text referencing the commit
	 */
	public GitCommitReference(@NonNull RevCommit target, @NonNull IRegion region) {
		this.target = target;
		this.region = region;
	}

	/**
	 * Retrieves the text region of this reference.
	 *
	 * @return the region
	 */
	public IRegion getRegion() {
		return region;
	}

	/**
	 * Retrieves the referenced commit.
	 *
	 * @return the commit
	 */
	public RevCommit getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof GitCommitReference) {
			GitCommitReference other = (GitCommitReference) obj;
			return target.equals(other.target) && region.equals(other.region);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return target.hashCode() ^ region.hashCode();
	}

}
