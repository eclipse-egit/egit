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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests for {@link CommonUtils}.
 */
public class CommonUtilsTest {
	@Test
	public void sortingNumbersBiggerThanIntegerShouldNotResultInNumberFormatException() {
		List<String> refs = Arrays.asList("refs/tags/stable-1-0-0_2011-05-25", "refs/tags/stable-1-0-0_2011-01-01");
		Collections.sort(refs, CommonUtils.STRING_ASCENDING_COMPARATOR);
		assertEquals(Arrays.asList("refs/tags/stable-1-0-0_2011-01-01", "refs/tags/stable-1-0-0_2011-05-25"), refs);
	}
}
