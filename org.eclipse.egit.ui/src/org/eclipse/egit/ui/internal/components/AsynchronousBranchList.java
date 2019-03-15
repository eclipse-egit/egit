/*******************************************************************************
 * Copyright (c) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.Repository;

/**
 * An {@code AsynchonousBranchList} is loaded asynchronously from an upstream
 * repository. {@link Ref}s obtained are filtered to contain only branches.
 */
public class AsynchronousBranchList extends AsynchronousListOperation<Ref> {

	/**
	 * Short name of a local branch to add as a suggestion if not {@code null}.
	 */
	private final String localBranchName;

	/**
	 * Creates a new {@link AsynchronousBranchList}.
	 *
	 * @param repository
	 *            local repository for which to run the operation
	 * @param uriText
	 *            upstream URI
	 * @param localBranchName
	 *            short name of a local branch to add as suggestion if not
	 *            {@code null}
	 */
	public AsynchronousBranchList(Repository repository, String uriText,
			String localBranchName) {
		super(repository, uriText);
		this.localBranchName = localBranchName;
	}

	/**
	 * Sorts the branches in ascending order by name and inserts a new ref for
	 * the given {@link #localBranchName} if not {@code null} and not present in
	 * {@code refs}.
	 *
	 * @param refs
	 *            obtained from the upstream repository
	 * @return the sorted list of branches, possibly with the suggestion in
	 *         front.
	 */
	@Override
	protected Collection<Ref> convert(Collection<Ref> refs) {
		List<Ref> filtered = new ArrayList<>();
		String localFullName = localBranchName != null
				? Constants.R_HEADS + localBranchName
				: null;
		boolean localBranchFound = false;
		// Restrict to branches
		for (Ref ref : refs) {
			String name = ref.getName();
			if (name.startsWith(Constants.R_HEADS)) {
				filtered.add(ref);
				if (localFullName != null
						&& localFullName.equalsIgnoreCase(name)) {
					localBranchFound = true;
				}
			}
		}
		// Sort them
		Collections.sort(filtered, CommonUtils.REF_ASCENDING_COMPARATOR);
		// Add a new remote ref for localBranchName in front if it doesn't
		// exist
		if (localFullName != null && !localBranchFound) {
			List<Ref> newRefs = new ArrayList<>(filtered.size() + 1);
			newRefs.add(new ObjectIdRef.Unpeeled(Storage.NEW, localFullName,
					ObjectId.zeroId()));
			newRefs.addAll(filtered);
			filtered = newRefs;
		}
		return filtered;
	}

}
