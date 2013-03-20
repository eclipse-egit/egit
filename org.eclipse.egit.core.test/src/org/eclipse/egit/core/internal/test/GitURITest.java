/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tomasz Zarna (IBM) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.HashMap;

import org.eclipse.egit.core.internal.GitProjectSetCapability;
import org.eclipse.egit.core.internal.GitURI;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
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

	// TODO remove this copy of org.eclipse.team.core.ScmUrlImportDescription
	// when we drop support for Galileo
	/**
	 * Copy of org.eclipse.team.core.ScmUrlImportDescription to support tests until
	 * we drop support for Galileo. DON'T USE OUTSIDE OF TEST BUNDLE!
	 *
	 * Describes how a bundle import will be executed. A bundle importer delegate
	 * creates bundle import descriptions when it validates bundle manifests for
	 * importing. The result, a set of bundle import descriptions is then passed to
	 * TeamUI, which basing on the info from the descriptions instantiate and
	 * initialize IScmUrlImportWizardPage pages. The pages can be used to alter the
	 * default import configuration e.g. for bundles stored in a CVS repository the
	 * user may want to check out HEAD rather than a specific version.
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This class has been added as part of a work in
	 * progress. There is no guarantee that this API will work or that it will
	 * remain the same. Please do not use this API without consulting with the Team
	 * team.
	 *
	 * @since 3.6
	 */
	protected static class ScmUrlImportDescription {
		private String url;
		private String project;
		private HashMap properties;

		public ScmUrlImportDescription(String url, String project) {
			this.url = url;
			this.project = project;
		}

		/**
		 * @return project name
		 */
		public String getProject() {
			return project;
		}

		/**
		 * SCM URL
		 *
		 * @return a string representation of the SCM URL
		 */
		public String getUrl() {
			return url;
		}

		public URI getUri() {
			return URI.create(url.replaceAll("\"", "")); //$NON-NLS-1$//$NON-NLS-2$
		}

		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * Sets or removes a client property.
		 *
		 * @param key
		 *            property key
		 * @param value
		 *            property value or <code>null</code> to remove the property
		 */
		public synchronized void setProperty(String key, Object value) {
			if (properties == null) {
				properties = new HashMap();
			}
			if (value == null) {
				properties.remove(key);
			} else {
				properties.put(key, value);
			}

		}

		/**
		 * Returns the specified client property, or <code>null</code> if none.
		 *
		 * @param key
		 *            property key
		 * @return property value or <code>null</code>
		 */
		public synchronized Object getProperty(String key) {
			if (properties == null) {
				return null;
			}
			return properties.get(key);
		}
	}

}
