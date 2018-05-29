/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private GitModelObject[] children;

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
		return getChildrenImpl();
	}

	/**
	 *  Disposes all nested resources
	 */
	public void dispose() {
		disposeOldChildren();
	}

	private GitModelObject[] getChildrenImpl() {
		List<GitModelObject> result = new ArrayList<>();
		try {
			if (gsds.size() == 1) {
				GitSynchronizeData gsd = gsds.iterator().next();
				GitModelRepository repoModel = new GitModelRepository(gsd);

				return repoModel.getChildren();
			} else
				for (GitSynchronizeData data : gsds) {
					GitModelRepository repoModel = new GitModelRepository(data);
					if (repoModel.getChildren().length > 0)
						result.add(repoModel);
				}
		} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
		}
		disposeOldChildren();
		children = result.toArray(new GitModelObject[result.size()]);

		return children;
	}

	private void disposeOldChildren() {
		if (children == null)
			return;
		for (GitModelObject child : children)
			child.dispose();
	}

}
