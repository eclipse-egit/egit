/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;

public final class StagingUtil {

	private StagingUtil() {
		// Utility class shall not be instantiated
	}

	public static void assertStaging(String projectName, String filePath,
			boolean expected) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		IResource resource = project.findMember(filePath);
		assertNotNull(filePath + " should exist", resource);
		IResourceState state = ResourceStateFactory.getInstance().get(resource);
		if (expected) {
			assertTrue(projectName + '/' + filePath + " should be staged",
					state.isStaged());
		} else {
			assertFalse(projectName + '/' + filePath + " should be unstaged",
					state.isStaged());
		}
	}

}
