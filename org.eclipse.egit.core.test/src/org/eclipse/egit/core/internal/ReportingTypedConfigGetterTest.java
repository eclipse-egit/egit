/*******************************************************************************
 * Copyright (C) 2023 Thomas Wolf <twolf@apache.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static org.junit.Assert.assertNull;

import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class ReportingTypedConfigGetterTest {

	private final ReportingTypedConfigGetter getter = new ReportingTypedConfigGetter();

	@Test
	public void testGetEnumInvalidWithNullDefault() {
		Config cfg = new Config();
		cfg.setString("test", null, "wrongOnPurpose", "nonexisting");
		BranchConfig.BranchRebaseMode value = getter.getEnum(cfg,
				BranchConfig.BranchRebaseMode.values(),
				"test", null, "wrongOnPurpose", null);
		assertNull(value);
	}
}
