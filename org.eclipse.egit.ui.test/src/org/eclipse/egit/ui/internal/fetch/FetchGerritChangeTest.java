/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.egit.ui.internal.fetch.FetchGerritChangePage.Change;
import org.junit.Test;
/**
 * Tests for determining a Gerrit change number from a string.
 */
public class FetchGerritChangeTest {

	@Test
	public void testChangeStringNull() {
		assertNull(FetchGerritChangePage.determineChangeFromString(null));
	}

	@Test
	public void testChangeStringEmpty() {
		assertNull(FetchGerritChangePage.determineChangeFromString(""));
	}

	@Test
	public void testChangeStringNoDigits() {
		assertNull(FetchGerritChangePage
				.determineChangeFromString("Just some string"));
	}

	@Test
	public void testChangeStringZero() {
		assertNull(FetchGerritChangePage.determineChangeFromString("0"));
	}

	@Test
	public void testChangeStringNegative() {
		assertNull(FetchGerritChangePage.determineChangeFromString("-17"));
	}

	@Test
	public void testChangeStringOverflow() {
		assertNull(FetchGerritChangePage
				.determineChangeFromString("4444444444444444/4"));
		assertNull(FetchGerritChangePage
				.determineChangeFromString("4/4444444444444444/"));
	}

	@Test
	public void testChangeStringUriNonsense() {
		assertNull(FetchGerritChangePage
				.determineChangeFromString("http://www.example.org/foo/bar"));
		assertNull(FetchGerritChangePage.determineChangeFromString(
				"https://git example.org/r/#/c/65510/"));
	}

	@Test
	public void testChangeStringMultiline() {
		assertNull(FetchGerritChangePage
				.determineChangeFromString("/10/\n65510/6"));
	}

	@Test
	public void testChangeStringUri() {
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/6"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/6/"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/6/some.path/some/File.txt"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/4..5"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/4..5/"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org/r/#/c/65510/4..5/some.path/some/File.txt"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString(
				"https://git.example.org:8080/r/#/c/65510"));
	}

	@Test
	public void testChangeStringSingleNumber() {
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("65510"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("/65510"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("65510/"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("/65510/"));
	}

	@Test
	public void testChangeStringTwoNumbers() {
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("65510/6"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("/65510/6"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("65510/6/"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("/65510/6/"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("10/65510"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("10/65510/"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("/10/65510"));
		assertEquals(Change.create(65510),
				FetchGerritChangePage.determineChangeFromString("/10/65510/"));
		assertEquals(Change.create(10),
				FetchGerritChangePage.determineChangeFromString("/10/10"));
		assertEquals(Change.create(10, 9),
				FetchGerritChangePage.determineChangeFromString("/10/9"));
	}

	@Test
	public void testChangeStringThreeNumbers() {
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("10/65510/6"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("/10/65510/6"));
		assertEquals(Change.create(65510, 6),
				FetchGerritChangePage.determineChangeFromString("10/65510/6/"));
		assertEquals(Change.create(65510, 6), FetchGerritChangePage
				.determineChangeFromString("/10/65510/6/"));
		assertEquals(Change.create(10, 6),
				FetchGerritChangePage.determineChangeFromString("/10/10/6"));
		assertEquals(Change.create(10, 6),
				FetchGerritChangePage.determineChangeFromString("/65510/10/6"));
	}

	@Test
	public void testChangeRefs() {
		assertEquals(Change.create(65510, 6), FetchGerritChangePage
				.determineChangeFromString("refs/changes/10/65510/6"));
		assertEquals(Change.create(65510), FetchGerritChangePage
				.determineChangeFromString("refs/changes/10/65510/"));
		assertEquals(Change.create(65510), FetchGerritChangePage
				.determineChangeFromString("refs/changes/10/65510"));
		assertNull(FetchGerritChangePage
				.determineChangeFromString("refs/changes/10/"));
		assertNull(FetchGerritChangePage
				.determineChangeFromString("refs/changes/1/1/1"));
		assertEquals(Change.create(1, 1), FetchGerritChangePage
				.determineChangeFromString("refs/changes/01/1/1"));
		assertEquals(Change.create(65510, 6), FetchGerritChangePage
				.determineChangeFromString("refs/changes/42/65510/6"));
	}

	@Test
	public void testFromRef() {
		assertNull(Change.fromRef("refs/changes/42/65510/6"));
		assertNull(Change.fromRef("refs/changes/10/65510/6..7"));
		assertNull(Change.fromRef("refs/changes/10/65510/6/7"));
		assertEquals(Change.create(65510, 6),
				Change.fromRef("refs/changes/10/65510/6"));
	}

	@Test
	public void testRefFromChange() {
		assertEquals("refs/changes/00/98000/2",
				Change.create(98000, 2).getRefName());
		assertEquals("refs/changes/01/98001/2",
				Change.create(98001, 2).getRefName());
		assertEquals("refs/changes/01/1/1", Change.create(1, 1).getRefName());
		assertEquals("refs/changes/10/65510/6",
				Change.create(65510, 6).getRefName());
	}
}
