/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.test.GitTestCase;
import org.junit.Test;

public class ResourceUtilTest extends GitTestCase {

	@Test
	public void getResourceForLocationShouldReturnFile() throws Exception {
		IFile file = project.createFile("file", new byte[] {});
		IResource resource = ResourceUtil.getResourceForLocation(file.getLocation());
		assertThat(resource, instanceOf(IFile.class));
	}

	@Test
	public void getResourceForLocationShouldReturnFolder() throws Exception {
		IFolder folder = project.createFolder("folder");
		IResource resource = ResourceUtil.getResourceForLocation(folder.getLocation());
		assertThat(resource, instanceOf(IFolder.class));
	}

	@Test
	public void getResourceForLocationShouldReturnNullForInexistentFile() throws Exception {
		IPath location = project.getProject().getLocation().append("inexistent");
		IResource resource = ResourceUtil.getResourceForLocation(location);
		assertThat(resource, nullValue());
	}
}
