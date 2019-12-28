/*******************************************************************************
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, Michael Keppler <michael.keppler@gmx.de>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.test.TestProject;
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
	public void sortingShouldWorkForEqualAndEmptyStrings() {
		assertEquals(0, CommonUtils.STRING_ASCENDING_COMPARATOR.compare("", ""));
		assertEquals(0,
				CommonUtils.STRING_ASCENDING_COMPARATOR.compare("a", "a"));
		assertTrue(CommonUtils.STRING_ASCENDING_COMPARATOR.compare("", "a") < 0);
		assertTrue(CommonUtils.STRING_ASCENDING_COMPARATOR.compare("a", "") > 0);
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

		assertNotEquals(0,
				CommonUtils.STRING_ASCENDING_COMPARATOR.compare("01", "1"));
		assertNotEquals(0,
				CommonUtils.STRING_ASCENDING_COMPARATOR.compare("1", "01"));
		assertTrue(CommonUtils.STRING_ASCENDING_COMPARATOR.compare("01x", "1") > 0);
		assertTrue(CommonUtils.STRING_ASCENDING_COMPARATOR.compare("01", "1x") < 0);
	}

	@Test
	public void sortingShouldIgnoreCase() {
		assertSortedLike("a", "b", "z");
		assertSortedLike("a", "B", "c", "D");

		assertNotEquals(0,
				CommonUtils.STRING_ASCENDING_COMPARATOR.compare("b1", "B1"));
	}

	/**
	 * Assert that sorting the given strings keeps the same order as passed.
	 *
	 * @param inputs
	 */
	private void assertSortedLike(String... inputs) {
		List<String> expected = Arrays.asList(inputs);
		List<String> tmp = new ArrayList<>(expected);
		Collections.shuffle(tmp, new Random(1));
		Collections.sort(tmp, CommonUtils.STRING_ASCENDING_COMPARATOR);
		assertEquals(expected, tmp);

		List<String> expectedWithoutDuplicates = new ArrayList<>(
				new LinkedHashSet<>(expected));
		List<String> shuffeled = new ArrayList<>(expected);
		Collections.shuffle(shuffeled, new Random(1));
		TreeSet<String> sortedSet = new TreeSet<>(
				CommonUtils.STRING_ASCENDING_COMPARATOR);
		sortedSet.addAll(shuffeled);
		assertEquals(expectedWithoutDuplicates,
				new ArrayList<>(sortedSet));
	}

	@Test
	public void testResourceCompare() throws Exception {
		// we just want to test that we can call the JFace resource name
		// comparator which itself is tested by JFace
		TestProject p = new TestProject();
		p.createFolder("test");
		IFile f1 = p.createFile("test/z.txt", "z".getBytes("UTF-8"));
		IFile f2 = p.createFile("test/d.txt", "d".getBytes("UTF-8"));
		IFile f3 = p.createFile("test/a.txt", "a".getBytes("UTF-8"));
		List<IResource> expected = Arrays
				.asList(new IResource[] { f3, f2,
				f1 });
		List<IResource> tmp = new ArrayList<>(expected);
		Collections.shuffle(tmp, new Random(1));
		Collections.sort(tmp, CommonUtils.RESOURCE_NAME_COMPARATOR);
		assertEquals(expected, tmp);

		// Clean up our mess
		IProgressMonitor monitor = new NullProgressMonitor();
		f1.delete(false, monitor);
		f2.delete(false, monitor);
		f3.delete(false, monitor);
		p.getProject().getFolder("test").delete(false, monitor);
		p.getProject().delete(false, monitor);
	}

	@Test
	public void testFooterOffsetNoFooter() {
		assertEquals(-1, CommonUtils.getFooterOffset(""));
		assertEquals(-1, CommonUtils.getFooterOffset("line 1"));
		assertEquals(-1, CommonUtils.getFooterOffset("line 1\nFoobar"));
		assertEquals(-1, CommonUtils.getFooterOffset("line 1\n\nFoobar"));
		assertEquals(-1, CommonUtils.getFooterOffset("line 1\nFoo:bar"));
		assertEquals(-1, CommonUtils.getFooterOffset("line 1\n_\nFoo:bar"));
	}

	@Test
	public void testFooterOffset() {
		assertEquals(8, CommonUtils.getFooterOffset("line 1\n\nFoo:bar"));
		assertEquals(8, CommonUtils.getFooterOffset("line 1\n\nFoo:bar   "));
		assertEquals(8, CommonUtils.getFooterOffset("line 1\n\nFoo:bar\n   "));
		assertEquals(8, CommonUtils.getFooterOffset("line 1\n\nFoo:bar\n  \n"));
		assertEquals(8, CommonUtils
				.getFooterOffset("line 1\n\nFoo:bar\nFoobar: barbar"));
		assertEquals(8, CommonUtils
				.getFooterOffset("line 1\n\nFoo:bar\nFoobar: barbar   "));
		assertEquals(8, CommonUtils
				.getFooterOffset("line 1\n\nFoo:bar\nFoobar: barbar\n   "));
		assertEquals(10,
				CommonUtils.getFooterOffset("line 1\n  \nFoo:bar\n  \n"));
		assertEquals(17, CommonUtils
				.getFooterOffset("line 1\n\nFoo:bar\n\nFoobar: barbar\n   "));
	}
}
