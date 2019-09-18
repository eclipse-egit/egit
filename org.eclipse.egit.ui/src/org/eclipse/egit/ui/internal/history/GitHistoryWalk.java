/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;

/**
 * An {@link SWTWalk} that sets its start points on the first call to next()
 * depending on the git history preferences. The point of doing so is to be sure
 * that potentially expensive operations are done in the GerenrateHistoryJob and
 * not in the UI thread.
 */
class GitHistoryWalk extends SWTWalk {

	private boolean initialized = false;

	private final ObjectId toShow;

	GitHistoryWalk(@NonNull Repository repository, ObjectId toShow) {
		super(repository);
		this.toShow = toShow;
	}

	@Override
	public RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (!initialized) {
			initialize();
		}
		return super.next();
	}

	@Override
	protected void reset(int retainFlags) {
		super.reset(retainFlags);
		initialized = false;
	}

	/**
	 * Set the walk's start points as defined by the git history preferences.
	 *
	 * @throws IOException
	 *             on errors
	 */
	private void initialize() throws IOException {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		RefDatabase db = getRepository().getRefDatabase();
		try {
			markStartAllRefs(new RefFilterHelper(getRepository())
					.getMatchingRefsForSelectedRefFilters());
			if (store.getBoolean(
					UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS)) {
				markStartAdditionalRefs(db);
			}
			if (store.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES)) {
				markStartAllRefs(db, Constants.R_NOTES);
			} else {
				markUninteresting(db, Constants.R_NOTES);
			}
			if (toShow != null) {
				markStart(toShow);
			}
		} catch (IOException e) {
			throw new IOException(MessageFormat.format(
					UIText.GitHistoryPage_errorSettingStartPoints,
					Activator.getDefault().getRepositoryUtil()
							.getRepositoryName(getRepository())),
					e);
		}
		initialized = true;
	}

	private void markStartAllRefs(RefDatabase db, String prefix)
			throws IOException, IncorrectObjectTypeException {
		for (Ref ref : db.getRefsByPrefix(prefix)) {
			if (!ref.isSymbolic()) {
				markStartRef(ref);
			}
		}
	}

	private void markStartAllRefs(Set<Ref> refs)
			throws IOException, IncorrectObjectTypeException {
		for (Ref ref : refs) {
			markStartRef(ref);
		}
	}

	private void markStartAdditionalRefs(RefDatabase db)
			throws IOException, IncorrectObjectTypeException {
		for (Ref ref : db.getAdditionalRefs()) {
			markStartRef(ref);
		}
	}

	private void markStart(ObjectId id)
			throws IOException, IncorrectObjectTypeException {
		try {
			RevObject peeled = peel(parseAny(id));
			if (peeled instanceof RevCommit) {
				markStart((RevCommit) peeled);
			}
		} catch (MissingObjectException e) {
			// If there is a ref which points to Nirvana then we should simply
			// ignore this ref. We should not let a corrupt ref cause that the
			// history view is not filled at all.
		}
	}

	private void markStartRef(Ref ref)
			throws IOException, IncorrectObjectTypeException {
		markStart(ref.getLeaf().getObjectId());
	}

	private void markUninteresting(RefDatabase db, String prefix)
			throws IOException, IncorrectObjectTypeException {
		for (Ref ref : db.getRefsByPrefix(prefix)) {
			if (!ref.isSymbolic()) {
				try {
					RevObject refTarget = parseAny(ref.getLeaf().getObjectId());
					if (refTarget instanceof RevCommit) {
						markUninteresting((RevCommit) refTarget);
					}
				} catch (MissingObjectException e) {
					// See above.
				}
			}
		}
	}

}
