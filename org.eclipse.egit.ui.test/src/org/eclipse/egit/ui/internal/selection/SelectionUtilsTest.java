/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Test;

public class SelectionUtilsTest {

	@Test
	public void getSelectedResourcesShouldUseAllTraversals() throws Exception {
		IResource resource1 = mock(IResource.class);
		IResource resource2 = mock(IResource.class);

		ResourceTraversal traversal1 = new ResourceTraversal(
				new IResource[] { resource1 }, 1, 0);
		ResourceTraversal traversal2 = new ResourceTraversal(
				new IResource[] { resource2 }, 1, 0);

		ResourceMapping mapping = mock(ResourceMapping.class);
		when(mapping.getTraversals(null, null)).thenReturn(
				new ResourceTraversal[] { traversal1, traversal2 });
		StructuredSelection selection = new StructuredSelection(mapping);

		IResource[] result = SelectionUtils.getSelectedResources(selection);

		assertEquals(2, result.length);
		assertEquals(resource1, result[0]);
		assertEquals(resource2, result[1]);
	}
}
