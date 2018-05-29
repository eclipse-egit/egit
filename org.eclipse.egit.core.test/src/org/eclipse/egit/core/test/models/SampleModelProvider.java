/*******************************************************************************
 * Copyright (C) 2014 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.models;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This model provider will make all files of a given extension to be part of
 * the same model. In this test, this will be used on files with extension
 * {@link #SAMPLE_FILE_EXTENSION}.
 */
public class SampleModelProvider extends ModelProvider {
	public static final String SAMPLE_PROVIDER_ID = "org.eclipse.egit.core.tests.sampleModelProvider";

	public static final String SAMPLE_FILE_EXTENSION = "sample";

	@Override
	public ResourceMapping[] getMappings(IResource resource,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		if (resource instanceof IFile
				&& SAMPLE_FILE_EXTENSION.equals(resource.getFileExtension())) {
			return new ResourceMapping[] { new SampleResourceMapping(
					(IFile) resource, SAMPLE_PROVIDER_ID), };
		}
		return super.getMappings(resource, context, monitor);
	}
}
