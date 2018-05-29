/*******************************************************************************
 * Copyright (C) 2011, Manuel Doninger <manuel.doninger@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;

import org.eclipse.egit.core.ProjectReference;
import org.junit.Before;
import org.junit.Test;

public class ProjectReferenceTest {

	private String version = "1.0";
	private String url = "git://egit.eclipse.org/egit.git";
	private String branch = "master";
	private String project = "org.eclipse.egit.core";
	private ProjectReference projectReference;

	@Before
	public void createProjectReferenceFromString() throws IllegalArgumentException, URISyntaxException {
		String reference = version + "," + url + "," + branch + "," + project;
		projectReference = new ProjectReference(reference);
		assertNotNull(projectReference);
	}

	@Test
	public void checkUrl() {
		assertEquals(url, projectReference.getRepository().toString());
	}

	@Test
	public void checkBranch() {
		assertEquals(branch, projectReference.getBranch());
	}

	@Test
	public void checkProject() {
		assertEquals(project, projectReference.getProjectDir());
	}
}
