/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.actions.PullFromUpstreamActionHandler;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the enablement of "Pull" for selections mixing shared, unshared, and
 * closed projects.
 */
public class PullFromUpstreamActionHandlerTest
		extends LocalRepositoryTestCase {

	private static final String UNSHARED = "UnsharedProject";

	private IProject shared;

	private IProject closed;

	private IProject unshared;

	@Before
	public void setup() throws Exception {
		createProjectAndCommitToRepository();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		shared = root.getProject(PROJ1);
		closed = root.getProject(PROJ2);
		closed.close(null);
		unshared = root.getProject(UNSHARED);
		unshared.create(null);
		unshared.open(null);
	}

	@After
	public void teardown() throws Exception {
		unshared.delete(true, true, null);
		closed.open(null);
	}

	@Test
	public void mixedSelectionIsEnabled() {
		assertEnabled(true, shared, unshared, closed);
		assertEnabled(true, closed, shared);
		assertEnabled(true, shared);
	}

	@Test
	public void selectionWithoutRepositoryIsDisabled() {
		assertEnabled(false, unshared, closed);
		assertEnabled(false, closed);
		assertEnabled(false);
	}

	private static void assertEnabled(boolean expected, Object... selected) {
		PullFromUpstreamActionHandler handler = new PullFromUpstreamActionHandler();
		handler.setSelection(new StructuredSelection(selected));
		assertEquals(expected, handler.isEnabled());
	}
}
