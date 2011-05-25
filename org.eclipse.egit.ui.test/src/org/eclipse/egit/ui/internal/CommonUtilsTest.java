/*******************************************************************************
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 * Tests for {@link CommonUtils}.
 */
public class CommonUtilsTest {
	@Test
	public void sortingShouldWorkForEasyStrings() {
		assertSortedLike("", "");
		assertSortedLike("", "a");
		assertSortedLike("a", "asdf");
		assertSortedLike("aaa", "bbb");
		assertSortedLike("1", "2");
	}

	@Test
	public void sortingShouldWorkForNumbers() {
		assertSortedLike("2", "10", "100");
	}

	@Test
	public void sortingShouldWorkForMixedParts() {
		assertSortedLike("v1c", "v2b", "v10a");
		assertSortedLike("asdf", "asdf2", "asdf10");
		assertSortedLike("1_1", "1_10");
		assertSortedLike("project-1-0-0-final", "project-1-0-1-beta");
		assertSortedLike("1-a", "01-b");
		assertSortedLike("1", "asdf-2", "asdf-10", "b20");
	}

	@Test
	public void sortingShouldWorkForBigNumbers() {
		assertSortedLike("100000000", "100000000000", "100000000000");
	}

	@Test
	public void sortingShouldIgnoreLeadingZeros() {
		assertSortedLike("00001", "2", "3");
		assertSortedLike("a-01", "a-002");
	}

	/**
	 * Assert that sorting the given strings keeps the same order as passed.
	 *
	 * @param inputs
	 */
	private void assertSortedLike(String... inputs) {
		List<String> expected = Arrays.asList(inputs);
		List<String> tmp = new ArrayList<String>(expected);
		Collections.shuffle(tmp, new Random(1));
		Collections.sort(tmp, CommonUtils.STRING_ASCENDING_COMPARATOR);
		assertEquals(expected, tmp);
	}
}
