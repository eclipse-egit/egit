/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

public class DiffEditorOutlinePageSortingTest {

	@Test
	public void testSort1() {
		String[] input = { "/", "org.eclipse.egit", "org.eclipse.egit.core",
				"org.eclipse.egit-feature", "org.eclipse.egit/META-INF",
				"org.eclipse.egit.core/META-INF" };
		String[] expected = { "/", "org.eclipse.egit",
				"org.eclipse.egit/META-INF", "org.eclipse.egit-feature",
				"org.eclipse.egit.core", "org.eclipse.egit.core/META-INF" };
		Arrays.sort(input, DiffEditorOutlinePage.CMP);
		assertArrayEquals(expected, input);
	}

}
