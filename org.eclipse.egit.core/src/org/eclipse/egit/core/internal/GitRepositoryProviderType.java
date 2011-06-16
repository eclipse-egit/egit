/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * The repository type for Git
 */
public class GitRepositoryProviderType extends RepositoryProviderType {

	public Subscriber getSubscriber() {
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		try {
			Repository[] repositories = Activator.getDefault()
					.getRepositoryCache().getAllRepositories();
			for (int i = 0; i < repositories.length; i++) {
				GitSynchronizeData data = new GitSynchronizeData(
						repositories[i], Constants.HEAD, Constants.HEAD, true);
				set.add(data);
			}
		} catch (IOException e) {
			// do nothing
		}

		return new GitResourceVariantTreeSubscriber(set);
	}

}
