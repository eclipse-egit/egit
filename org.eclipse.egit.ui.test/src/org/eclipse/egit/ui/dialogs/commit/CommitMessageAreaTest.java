/*******************************************************************************
 * Copyright (C) 2010, Robin Stocker
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.dialogs.commit;

import static org.junit.Assert.assertEquals;

import org.eclipse.egit.ui.internal.dialogs.CommitMessageArea;
import org.junit.Test;

public class CommitMessageAreaTest {

	@Test
	public void dontWrapShortText() {
		String input = "short message";
		String wrapped = wrap(input);
		assertEquals(input, wrapped);
	}

	@Test
	public void dontWrapAlreadyWrappedText() {
		String input = "This is a test of wrapping\n\nDid it work?\n\nHm?";
		String wrapped = wrap(input);
		assertEquals(input, wrapped);
	}

	@Test
	public void dontWrapMaximumLengthText() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.";
		String wrapped = wrap(input);
		assertEquals(input, wrapped);
	}

	@Test
	public void wrapOverlengthText() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789. 123";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.\n123";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	@Test
	public void wrapOverlengthTextByOne() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789ab";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789\n123456789ab";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	@Test
	public void wrapOverlengthText2() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789. 12345678901234";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789.\n12345678901234";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	public void wrapOverlengthTextTwice() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789. "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789. "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	@Test
	public void dontWrapWordLongerThanOneLineAtStart() {
		String input = "12345678901234567890123456789012345678901234567890123456789012345678901234567890 the previous was longer than a line";
		String expected = "12345678901234567890123456789012345678901234567890123456789012345678901234567890\nthe previous was longer than a line";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	@Test
	public void dontWrapWordLongerThanOneLine() {
		String input = "This has to be on its own line: 12345678901234567890123456789012345678901234567890123456789012345678901234567890 this not";
		String expected = "This has to be on its own line:\n12345678901234567890123456789012345678901234567890123456789012345678901234567890\nthis not";
		String wrapped = wrap(input);
		assertEquals(expected, wrapped);
	}

	@Test
	public void keepExistingNewlines() {
		String input = "This\n\nis\nall\nok\n123456789 123456789 123456789 123456789 123456789 123456789 123456789.";
		String wrapped = wrap(input);
		assertEquals(input, wrapped);
	}

	private static String wrap(String text) {
		return CommitMessageArea.wrap(text, 70);
	}

}
