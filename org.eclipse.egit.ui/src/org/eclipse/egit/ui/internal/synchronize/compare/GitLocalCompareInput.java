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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.LocalResourceTypedElement;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingFile;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;

/**
 * Specific implementation of {@link ISynchronizationCompareInput} for showing
 * locally changed files
 */
public class GitLocalCompareInput extends GitCompareInput {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	/**
	 * @param object
	 */
	public GitLocalCompareInput(GitModelWorkingFile object) {
		super(object);
	}

	public ITypedElement getLeft() {
		String absoluteFilePath = resource.getRepository().getWorkTree()
				.getAbsolutePath()
				+ "/" + resource.getGitPath(); //$NON-NLS-1$
		IFile file = ROOT.getFileForLocation(new Path(absoluteFilePath));

		if (file == null)
			return new LocalNonWorkspaceTypedElement(absoluteFilePath);

		return new LocalResourceTypedElement(file);
	}

	@Override
	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		super.prepareInput(configuration, monitor);

		// enable edition on both sides
		configuration.setLeftEditable(true);
		configuration.setRightEditable(true);
	}

	public ITypedElement getRight() {
		return getFileCachedRevisionTypedElement(resource.getGitPath(), resource.getRepository());
	}

	@Override
	public ITypedElement getAncestor() {
		return getRight();
	}

}
