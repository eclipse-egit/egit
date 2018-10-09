/*******************************************************************************
 * Copyright (C) 2018, Markus Duft <markus.duft@ssi-schaefer.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.eclipse.egit.core.op.ConfigureGerritAfterCloneTask.isGerritVersion;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConfigureGerritAfterCloneMatcherTest {

	@Test
	public void testMatches() {
		assertTrue(isGerritVersion(
				"gerrit version 2.15.3-5016-g7169461907-dirty"));
		assertTrue(isGerritVersion("gerrit version 2.14.6"));
		assertTrue(isGerritVersion("gerrit version 2.14.6-6-g7f3d26c65a"));
		assertTrue(isGerritVersion("gerrit version 2.16-rc1-81-gfbf1af9b73"));
	}

}
