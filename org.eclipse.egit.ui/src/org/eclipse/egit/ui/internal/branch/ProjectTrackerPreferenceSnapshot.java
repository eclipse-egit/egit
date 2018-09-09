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

import java.util.List;

import org.eclipse.jgit.lib.Repository;

/**
 * Class that contains the properties that are required to be persisted and
 * afterwards made available for the project tracking on checkout feature
 *
 * @see BranchProjectTracker
 */
class ProjectTrackerPreferenceSnapshot {

	private Repository repo;
	private String branch;
	private List<String> associatedProjects;

	/**
	 * @param repo
	 * @param branch
	 * @param associatedProjects
	 */
	ProjectTrackerPreferenceSnapshot(Repository repo, String branch,
			List<String> associatedProjects) {
		this.repo = repo;
		this.branch = branch;
		this.associatedProjects = associatedProjects;
	}

	Repository getRepository() {
		return repo;
	}

	String getBranch() {
		return branch;
	}

	List<String> getAssociatedProjects() {
		return associatedProjects;
	}
}
