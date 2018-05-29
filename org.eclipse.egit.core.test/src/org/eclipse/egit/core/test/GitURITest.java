/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tomasz Zarna (IBM) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.eclipse.egit.core.GitProjectSetCapability;
import org.eclipse.egit.core.internal.GitURI;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.junit.Before;
import org.junit.Test;

public class GitURITest {

	private GitProjectSetCapability capability;

	@Before
	public void setUp() {
		capability = new GitProjectSetCapability();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidScmUriWithQuotationMarks() throws Exception {
		URI.create("scm:git:git://git.eclipse.org/gitroot/platform/eclipse.platform.team.git;path=\"bundles/org.eclipse.team.core\"");
		// expected IAE, " are not allowed in a URI reference
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidScmUriWithoutPath() throws Exception {
		new GitURI(URI
				.create("scm:git:git://git.eclipse.org/gitroot/cdo/cdo.git"));
		// expected IAE, it doesn't contain semicolon and path part
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidScmUriForCVS() throws Exception {
		new GitURI(URI.create("scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.compare"));
		// expected IAE, it's a CVS SCM URL
	}

	// ScmUrlImportDescription can handle " in Strings expected to be URI refs
	@Test
	public void testScmUriWithPath() throws Exception {
		ScmUrlImportDescription description = new ScmUrlImportDescription(
				"scm:git:git://git.eclipse.org/gitroot/platform/eclipse.platform.team.git;path=\"bundles/org.eclipse.team.core\"",
				null);
		URI uri = description.getUri();
		GitURI gitUri = new GitURI(uri);
		assertEquals("bundles/org.eclipse.team.core", gitUri.getPath()
				.toString());
		URIish uriish = new URIish(
				"git://git.eclipse.org/gitroot/platform/eclipse.platform.team.git");
		assertEquals(uriish, gitUri.getRepository());
		assertEquals(Constants.MASTER, gitUri.getTag());

		String refString = capability.asReference(uri, "org.eclipse.team.core");
		assertEquals(
				"1.0,git://git.eclipse.org/gitroot/platform/eclipse.platform.team.git,master,bundles/org.eclipse.team.core",
				refString);
	}

	@Test
	public void testScmUriWithPathAndTag() throws Exception {
		ScmUrlImportDescription description = new ScmUrlImportDescription(
				"scm:git:git://git.eclipse.org/gitroot/platform/eclipse.platform.ui.git;path=\"bundles/org.eclipse.jface\";tag=v20111107-2125",
				null);
		URI uri = description.getUri();
		GitURI gitUri = new GitURI(uri);
		assertEquals("bundles/org.eclipse.jface", gitUri.getPath().toString());
		URIish uriish = new URIish(
				"git://git.eclipse.org/gitroot/platform/eclipse.platform.ui.git");
		assertEquals(uriish, gitUri.getRepository());
		assertEquals("v20111107-2125", gitUri.getTag());

		String refString = capability.asReference(uri, "org.eclipse.jface");
		assertEquals(
				"1.0,git://git.eclipse.org/gitroot/platform/eclipse.platform.ui.git,v20111107-2125,bundles/org.eclipse.jface",
				refString);
	}

	@Test
	public void testScmUriWithPathProjectAndTag() throws Exception {
		ScmUrlImportDescription description = new ScmUrlImportDescription(
				"scm:git:git://git.eclipse.org/gitroot/equinox/rt.equinox.bundles.git;path=\"bundles/org.eclipse.equinox.http.jetty6\";project=\"org.eclipse.equinox.http.jetty\";tag=v20111010-1614",
				null);
		URI uri = description.getUri();
		GitURI gitUri = new GitURI(uri);
		assertEquals("bundles/org.eclipse.equinox.http.jetty6", gitUri
				.getPath().toString());
		URIish uriish = new URIish(
				"git://git.eclipse.org/gitroot/equinox/rt.equinox.bundles.git");
		assertEquals(uriish, gitUri.getRepository());
		assertEquals("v20111010-1614", gitUri.getTag());
		assertEquals("org.eclipse.equinox.http.jetty", gitUri.getProjectName());

		String refString = capability.asReference(uri,
				"org.eclipse.equinox.http.jetty");
		assertEquals(
				"1.0,git://git.eclipse.org/gitroot/equinox/rt.equinox.bundles.git,v20111010-1614,bundles/org.eclipse.equinox.http.jetty6",
				refString);
	}
}
