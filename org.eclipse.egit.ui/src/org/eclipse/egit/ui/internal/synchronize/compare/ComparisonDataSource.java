/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Simple entry that connects {@link RevCommit} and {@link ObjectId} to reduce
 * number of parameters in {@link GitCompareInput} constructor.
 */
public class ComparisonDataSource {

	private final RevCommit commit;

	private final ObjectId objectId;

	/**
	 * Create instance of {@link ComparisonDataSource}
	 *
	 * @param commit
	 *            that should be associated with this comparison data source
	 * @param objectId
	 *            that should be associated with this comparison data source
	 */
	public ComparisonDataSource(RevCommit commit, ObjectId objectId) {
		this.commit = commit;
		this.objectId = objectId;
	}

	/**
	 * @return {@link RevCommit} instance associated with this comparison data
	 *         source
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	/**
	 * @return {@link ObjectId} instance associated with object that is compared
	 */
	public ObjectId getObjectId() {
		return objectId;
	}

}
