/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import static org.eclipse.egit.ui.internal.CompareUtils.getFileCachedRevisionTypedElement;
import static org.eclipse.egit.ui.internal.CompareUtils.getFileRevisionTypedElement;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCacheFile;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Specific implementation of {@link ISynchronizationCompareInput} for showing
 * cached files
 */
public class GitCacheCompareInput extends GitCompareInput {

	/**
	 * @param object
	 */
	public GitCacheCompareInput(GitModelCacheFile object) {
		super(object);
	}

	public ITypedElement getLeft() {
		return getFileCachedRevisionTypedElement(resource.getGitPath(),
				resource.getRepository());
	}

	public ITypedElement getRight() {
		return getFileRevisionTypedElement(resource.getGitPath(),
				resource.getBaseCommit(), resource.getRepository());
	}

	@Override
	public ITypedElement getAncestor() {
		return getRight();
	}

	@Override
	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		super.prepareInput(configuration, monitor);

		// allow only modify left side (staged version)
		configuration.setLeftEditable(true);
	}

}
