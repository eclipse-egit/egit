/*******************************************************************************
 * Copyright (C) 2014 Andreas Hermann <a.v.hermann@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class NaturalStringComparatorTest {

	private NaturalStringComparator comparator;

	@Before
	public void setUp() {
		comparator = NaturalStringComparator.INSTANCE;
	}

	@Test
	public void shouldCompareNormally() {
		assertTrue(comparator.compare("a", "a") == 0);
		assertTrue(comparator.compare("1", "1") == 0);
		assertTrue(comparator.compare("a", "b") < 0);
		assertTrue(comparator.compare("a", "1") > 0);
		assertTrue(comparator.compare("1", "2") < 0);
		assertTrue(comparator.compare("2", "11") < 0);
		assertTrue(comparator.compare("v2", "v1") > 0);
		assertTrue(comparator.compare("v1", "v1") == 0);
		assertTrue(comparator.compare("v2.0b", "v2.0a") > 0);
	}

	@Test
	public void shouldCompareNumbers() {
		assertTrue(comparator.compare("v2", "v12") < 0);
		assertTrue(comparator.compare("v2.0.1", "v2.0.12") < 0);
		assertTrue(comparator.compare("refs/tags/v0.10.1", "refs/tags/v0.9.1") > 0);
	}

}
