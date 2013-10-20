/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import static org.eclipse.egit.ui.internal.repository.CreateBranchPage.getProposedTargetName;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests of {@link CreateBranchPage}
 */
public class CreateBranchPageTest {

	@Test
	public void computeTargetName() {
		assertNull(getProposedTargetName(null));
		assertEquals("", getProposedTargetName(""));
		assertEquals("a/b", getProposedTargetName(R_REMOTES + "origin/a/b"));
		assertEquals("r1", getProposedTargetName(R_REMOTES + "review/r1"));
		assertEquals("v1", getProposedTargetName(R_TAGS + "v1"));
	}
}
