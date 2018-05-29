/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.ZERO_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.junit.Test;

public class ChangeTest {

	private static final AbbreviatedObjectId MISC_ID = AbbreviatedObjectId
			.fromString("63448b851ae8831a1ad007f588508d3246ec7ace");

	@Test
	public void shouldNotBeEqualWithNullRefference() {
		// given
		Change change = new Change();

		// when
		boolean result = change.equals(null);

		// then
		assertFalse(result);
	}

	@Test
	public void shouldNotBeEqualWithDifferentType() {
		// given
		Change change = new Change();

		// when
		boolean result = change.equals(new Object());

		// then
		assertFalse(result);
	}

	@Test
	public void shouldBeEqualWhenBothIdsAreNull() {
		// given
		Change change = new Change();

		// when
		boolean result = change.equals(new Change());

		// then
		assertTrue(result);
	}

	@Test
	public void shouldNotBeEqualWhenOneObjectIdIsNull() {
		// given
		Change change = new Change();
		change.objectId = ZERO_ID;

		// when
		boolean result = change.equals(new Change());

		// then
		assertFalse(result);
	}

	@Test
	public void shouldBeEqualWhenBothObjectIdsAreTheSame() {
		// given
		Change c1 = new Change();
		Change c2 = new Change();
		c1.objectId = c2.objectId = MISC_ID;

		// when
		boolean result = c1.equals(c2);

		// then
		assertTrue(result);
		assertEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void shouldNotBeEqualWhenObjectIdsAreDifferent() {
		// given
		Change c1 = new Change();
		Change c2 = new Change();
		c1.objectId = ZERO_ID;
		c2.objectId = MISC_ID;

		// when
		boolean result = c1.equals(c2);

		// then
		assertFalse(result);
		assertFalse(c1.hashCode() == c2.hashCode());
	}

	@Test
	public void shouldNotBeEqualWhenOneRemoteObjectIsNull() {
		// given
		Change c1 = new Change();
		Change c2 = new Change();
		c1.objectId = c2.commitId = ZERO_ID;
		c1.remoteObjectId = MISC_ID;

		// when
		boolean result = c1.equals(c2);

		// then
		assertFalse(result);
		assertFalse(c1.hashCode() == c2.hashCode());
	}

	@Test
	public void shouldBeEqualWhenBothObjectIdsAndRemoteIdsAreSame() {
		// given
		Change c1 = new Change();
		Change c2 = new Change();
		c1.objectId = c2.objectId = ZERO_ID;
		c1.remoteObjectId = c2.remoteObjectId = MISC_ID;

		// when
		boolean result = c1.equals(c2);

		// then
		assertTrue(result);
		assertEquals(c1.hashCode(), c2.hashCode());
	}

}
