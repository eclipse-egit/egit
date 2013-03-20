/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.egit.core.internal.Utils;
import org.junit.Test;

public class UtilsTest {

	@Test
	public void testLineEndingNormalization() {
		String str = "";
		String result = Utils.normalizeLineEndings(str);
		assertEquals(result, "");

		str = "Line 1";
		result = Utils.normalizeLineEndings(str);
		assertEquals("Line 1", result);

		str = "Line 1\r\nLine 2";
		result = Utils.normalizeLineEndings(str);
		assertEquals("Line 1\nLine 2", result);

		str = "Line 1\r\nLine 2\r";
		result = Utils.normalizeLineEndings(str);
		assertEquals("Line 1\nLine 2\n", result);

		str = "Line 1\r\nLine 2\nLine 3\rLine 4\r\nLine 5\rLine 6\r\n Line 7\r";
		result = Utils.normalizeLineEndings(str);
		assertEquals(
				"Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\n Line 7\n",
				result);
	}

}
