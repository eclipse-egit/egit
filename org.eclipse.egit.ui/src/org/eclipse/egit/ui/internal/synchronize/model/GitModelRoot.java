/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;

/**
 * Root of all model objects.
 */
public class GitModelRoot {

	private final GitSynchronizeDataSet gsds;

	/**
	 * @param gsds
	 */
	public GitModelRoot(GitSynchronizeDataSet gsds) {
		this.gsds = gsds;
	}

	/**
	 * @return git synchronization data
	 */
	public GitSynchronizeDataSet getGsds() {
		return gsds;
	}

	/**
	 * @return children
	 */
	public GitModelRepository[] getChildren() {
		List<GitModelRepository> restult = new ArrayList<GitModelRepository>();
		for (GitSynchronizeData data : gsds) {
			try {
				restult.add(new GitModelRepository(data));
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		return restult.toArray(new GitModelRepository[restult.size()]);
	}

}
