/*******************************************************************************
 * Copyright (C) 2015 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import static java.util.Arrays.asList;
import static org.eclipse.egit.core.internal.indexdiff.IndexDiffData.isAnyPrefixOf;
import static org.eclipse.egit.core.internal.indexdiff.IndexDiffData.mergeIgnored;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.egit.core.test.GitTestCase;
import org.junit.Test;

public class IndexDiffDataTest extends GitTestCase {

	@Test
	public void testIsAnyPrefixOf() {
		Collection<String> possiblePrefixes = asList("", "/");
		assertTrue(isAnyPrefixOf("", possiblePrefixes));

		possiblePrefixes = asList("", "/");
		assertTrue(isAnyPrefixOf("/", possiblePrefixes));

		possiblePrefixes = asList("a");
		assertTrue(isAnyPrefixOf("a", possiblePrefixes));

		possiblePrefixes = asList("a/");
		assertTrue(isAnyPrefixOf("a", possiblePrefixes));

		possiblePrefixes = asList("b");
		assertFalse(isAnyPrefixOf("a", possiblePrefixes));

		possiblePrefixes = asList("b", "ab", "b/", "aa");
		assertFalse(isAnyPrefixOf("a", possiblePrefixes));
	}

	@Test
	public void testMergeIgnored() {
		Set<String> result;
		Set<String> expected = new HashSet<String>();
		Set<String> oldIgnoredPaths = new HashSet<String>();
		Collection<String> changedPaths = new HashSet<String>();
		Set<String> newIgnoredPaths = new HashSet<String>();

		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		newIgnoredPaths.add("a");
		changedPaths.add("a");
		expected.add("a");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		newIgnoredPaths.add("b");
		expected.add("b");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		changedPaths.add("b");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		oldIgnoredPaths.add("b");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		oldIgnoredPaths.add("c");
		expected.add("c");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);

		newIgnoredPaths.add("b");
		expected.add("b");
		result = mergeIgnored(oldIgnoredPaths, changedPaths, newIgnoredPaths);
		assertEquals(expected, result);
	}
}
