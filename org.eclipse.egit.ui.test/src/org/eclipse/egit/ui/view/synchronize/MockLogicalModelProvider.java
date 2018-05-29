/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Mock Model Provider that requires a {@link RemoteResourceMappingContext} to
 * perform correctly in synchronizations.
 */
public class MockLogicalModelProvider extends ModelProvider {
	public static final String ID = "org.eclipse.egit.ui.test.mockLogicalModelProvider";

	public static final String MOCK_LOGICAL_FILE_EXTENSION = "mocklogical";

	@Override
	public ResourceMapping[] getMappings(IResource resource,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		if (resource instanceof IFile && MOCK_LOGICAL_FILE_EXTENSION
				.equals(resource.getFileExtension())) {
			return new ResourceMapping[] { new MockLogicalResourceMapping(
					(IFile) resource, ID), };
		}
		return super.getMappings(resource, context, monitor);
	}
}
