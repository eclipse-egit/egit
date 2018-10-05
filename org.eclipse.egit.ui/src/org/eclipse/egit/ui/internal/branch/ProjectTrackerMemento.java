/*******************************************************************************
 * Copyright (C) 2018, Luís Copetti <lhcopetti@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An opaque class that can be retrieved by
 * {@link BranchProjectTracker#snapshot()} and later saved by calling
 * {@link BranchProjectTracker#save(ProjectTrackerMemento)}. For more
 * information, look at the docs for {{@link BranchProjectTracker}
 */
class ProjectTrackerMemento {

	private final Collection<ProjectTrackerPreferenceSnapshot> snapshots = new ArrayList<>();

	/**
	 * @param snapshot
	 */
	void addSnapshot(ProjectTrackerPreferenceSnapshot snapshot) {
		snapshots.add(snapshot);
	}

	/**
	 * @return the snapshot pairs previously added by
	 *         {@link #addSnapshot(ProjectTrackerPreferenceSnapshot)}
	 */
	Collection<ProjectTrackerPreferenceSnapshot> getSnapshots() {
		return snapshots;
	}
}
