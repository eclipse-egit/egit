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

import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * The repository type for Git
 */
public class GitRepositoryProviderType extends RepositoryProviderType {

	/**
	 * The repository type for Git
	 */
	public GitRepositoryProviderType() {
	}

	public Subscriber getSubscriber() {
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		return new GitResourceVariantTreeSubscriber(set);
	}

}
