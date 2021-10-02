/*******************************************************************************
 * Copyright (c) 2016, 2020 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.egit.ui.internal.fetch.FetchGerritChangePage.GerritChange;
import org.junit.Test;
/**
 * Tests for determining a Gerrit change number from a string.
 */
public class FetchGerritChangeTest {

	@Test
	public void testChangeStringNull() {
		assertNull(FetchGerritChangePage.fromString(null));
	}

	@Test
	public void testChangeStringEmpty() {
		assertNull(FetchGerritChangePage.fromString(""));
	}

	@Test
	public void testChangeStringNoDigits() {
		assertNull(FetchGerritChangePage
				.fromString("Just some string"));
	}

	@Test
	public void testChangeStringZero() {
		assertNull(FetchGerritChangePage.fromString("0"));
	}

	@Test
	public void testChangeStringNegative() {
		assertNull(FetchGerritChangePage.fromString("-17"));
	}

	@Test
	public void testChangeStringOverflow() {
		assertNull(FetchGerritChangePage
				.fromString("4444444444444444/4"));
		assertNull(FetchGerritChangePage
				.fromString("4/4444444444444444/"));
	}

	@Test
	public void testChangeStringUriNonsense() {
		assertNull(FetchGerritChangePage
				.fromString("http://www.example.org/foo/bar"));
		assertNull(FetchGerritChangePage.fromString(
				"https://git example.org/r/#/c/65510/"));
	}

	@Test
	public void testChangeStringMultiline() {
		assertNull(FetchGerritChangePage
				.fromString("/10/\n65510/6"));
	}

	@Test
	public void testChangeStringUri() {
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/6"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/6/"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/6/some.path/some/File.txt"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/4..5"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/4..5/"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org/r/#/c/65510/4..5/some.path/some/File.txt"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString(
				"https://git.example.org:8080/r/#/c/65510"));
	}

	@Test
	public void testChangeStringSingleNumber() {
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("65510"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("/65510"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("65510/"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("/65510/"));
	}

	@Test
	public void testChangeStringTwoNumbers() {
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("65510/6"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("/65510/6"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("65510/6/"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("/65510/6/"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("10/65510"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("10/65510/"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("/10/65510"));
		assertEquals(new GerritChange(65510),
				FetchGerritChangePage.fromString("/10/65510/"));
		assertEquals(new GerritChange(10),
				FetchGerritChangePage.fromString("/10/10"));
		assertEquals(new GerritChange(10, 9),
				FetchGerritChangePage.fromString("/10/9"));
	}

	@Test
	public void testChangeStringThreeNumbers() {
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("10/65510/6"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("/10/65510/6"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromString("10/65510/6/"));
		assertEquals(new GerritChange(65510, 6), FetchGerritChangePage
				.fromString("/10/65510/6/"));
		assertEquals(new GerritChange(10, 6),
				FetchGerritChangePage.fromString("/10/10/6"));
		assertEquals(new GerritChange(10, 6),
				FetchGerritChangePage.fromString("/65510/10/6"));
	}

	@Test
	public void testChangeRefs() {
		assertEquals(new GerritChange(65510, 6), FetchGerritChangePage
				.fromString("refs/changes/10/65510/6"));
		assertEquals(new GerritChange(65510), FetchGerritChangePage
				.fromString("refs/changes/10/65510/"));
		assertEquals(new GerritChange(65510), FetchGerritChangePage
				.fromString("refs/changes/10/65510"));
		assertNull(FetchGerritChangePage
				.fromString("refs/changes/10/"));
		assertNull(FetchGerritChangePage
				.fromString("refs/changes/1/1/1"));
		assertEquals(new GerritChange(1, 1), FetchGerritChangePage
				.fromString("refs/changes/01/1/1"));
		assertEquals(new GerritChange(65510, 6), FetchGerritChangePage
				.fromString("refs/changes/42/65510/6"));
	}

	@Test
	public void testFromRef() {
		assertNull(FetchGerritChangePage.fromRef("refs/changes/42/65510/6"));
		assertNull(FetchGerritChangePage.fromRef("refs/changes/10/65510/6..7"));
		assertNull(FetchGerritChangePage.fromRef("refs/changes/10/65510/6/7"));
		assertEquals(new GerritChange(65510, 6),
				FetchGerritChangePage.fromRef("refs/changes/10/65510/6"));
	}

	@Test
	public void testRefFromChange() {
		assertEquals("refs/changes/00/98000/2",
				new GerritChange(98000, 2).getRefName());
		assertEquals("refs/changes/01/98001/2",
				new GerritChange(98001, 2).getRefName());
		assertEquals("refs/changes/01/1/1",
				new GerritChange(1, 1).getRefName());
		assertEquals("refs/changes/10/65510/6",
				new GerritChange(65510, 6).getRefName());
	}
}
