/*******************************************************************************
 * Copyright (c) 2017 EclipseSource Services GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Martin Fleck - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An unregistered resource mapping used for testing the decoration of resource
 * mappings.
 *
 * @author Martin Fleck <mfleck@eclipsesource.com>
 */
public class TestResourceMapping extends ResourceMapping {

	private IProject project;

	private IFile mainFile;

	private IFile[] mappedFiles;

	/**
	 * Creates a new resource mapping whose model object is null.
	 *
	 * @param project
	 *            project used in {@link #getProjects()}
	 * @param mappedFiles
	 *            files used for
	 *            {@link #getTraversals(ResourceMappingContext, IProgressMonitor)
	 *            traversal}.
	 */
	public TestResourceMapping(IProject project, IFile... mappedFiles) {
		this.project = project;
		this.mappedFiles = mappedFiles;
	}

	/**
	 * Creates a new resource mapping whose model object is the given main file.
	 *
	 * @param mainFile
	 *            file used as {@link #getModelObject() model object}
	 * @param mappedFiles
	 *            files used for
	 *            {@link #getTraversals(ResourceMappingContext, IProgressMonitor)
	 *            traversal}.
	 */
	public TestResourceMapping(IFile mainFile, IFile... mappedFiles) {
		this.mainFile = mainFile;
		this.project = mainFile.getProject();
		this.mappedFiles = mappedFiles;
	}

	@Override
	public Object getModelObject() {
		if (mainFile != null) {
			return mainFile;
		}
		return null;
	}

	@Override
	public String getModelProviderId() {
		return null;
	}

	@Override
	public IProject[] getProjects() {
		return new IProject[] { project };
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		return new ResourceTraversal[] {
				new ResourceTraversal(mappedFiles,
						IResource.DEPTH_INFINITE, IResource.NONE) };
	}
}
