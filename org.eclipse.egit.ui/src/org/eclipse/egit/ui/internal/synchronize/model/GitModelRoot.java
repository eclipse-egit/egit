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
	public GitModelObject[] getChildren() {
		List<GitModelObject> restult = new ArrayList<GitModelObject>();
		try {
			if (gsds.size() == 1) {
				GitSynchronizeData gsd = gsds.iterator().next();
				GitModelRepository repoModel = new GitModelRepository(gsd);

				for (GitModelObject obj : repoModel.getChildren())
					restult.add(obj);
			} else
				for (GitSynchronizeData data : gsds) {
					GitModelRepository repoModel = new GitModelRepository(data);
					if (repoModel.getChildren().length > 0)
						restult.add(repoModel);
				}
		} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
		}

		return restult.toArray(new GitModelObject[restult.size()]);
	}

}
