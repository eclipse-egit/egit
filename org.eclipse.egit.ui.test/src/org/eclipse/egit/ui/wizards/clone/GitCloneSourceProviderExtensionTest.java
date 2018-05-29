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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

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
		assertThat(repositoryImports.get(1).getLabel(),
				is("ServerWithoutPage1"));
		assertThat(repositoryImports.get(1).hasFixLocation(), is(true));
		assertThat(repositoryImports.get(1).getRepositoryServerProvider(),
				instanceOf(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getLabel(), is("TestServer"));
		assertThat(repositoryImports.get(2).hasFixLocation(), is(false));
		assertThat(repositoryImports.get(2).getRepositoryServerProvider(),
				instanceOf(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getRepositorySearchPage(),
				instanceOf(TestRepositorySearchPage.class));
		assertThat(repositoryImports.get(3).getLabel(),
				is("ServerWithoutPage2"));
		assertThat(repositoryImports.get(3).hasFixLocation(), is(false));
	}
}
