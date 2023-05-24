/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.junit.Test;

public class GitCloneSourceProviderExtensionTest {

	@SuppressWarnings("boxing")
	@Test
	public void testGetRepositoryImports() throws Exception {
		List<CloneSourceProvider> repositoryImports = GitCloneSourceProviderExtension
				.getCloneSourceProvider();
		assertThat(repositoryImports, is(notNullValue()));
		assertThat(repositoryImports.size(), is(4));

		for (CloneSourceProvider ri : repositoryImports) {
			String label = ri.getLabel();
			switch (label) {
			case "ServerWithoutPage1":
				assertThat(ri.hasFixLocation(), is(true));
				assertThat(ri.getRepositoryServerProvider(),
						instanceOf(TestRepositoryServerProvider.class));
				break;
			case "TestServer":
				assertThat(ri.hasFixLocation(), is(false));
				assertThat(ri.getRepositoryServerProvider(),
						instanceOf(TestRepositoryServerProvider.class));
				assertThat(ri.getRepositorySearchPage(),
						instanceOf(TestRepositorySearchPage.class));
				break;
			case "ServerWithoutPage2":
				assertThat(ri.hasFixLocation(), is(false));
				break;
			default:
				fail("unexpected CloneSourceProvider " + label);
			}
		}
	}
}
