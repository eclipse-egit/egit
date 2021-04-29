/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.efs;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Tests for {@link OursVersionInputStream}.
 */
public class OursVersionInputStreamTest {

	private void assertTransformation(String expected, String input,
			boolean diff3) throws IOException {
		// Read in chunks
		try (ByteArrayInputStream in = new ByteArrayInputStream(
				input.getBytes(US_ASCII));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream ours = new OursVersionInputStream(in, 7, diff3)) {
			byte[] buf = new byte[4000];
			int r;
			while ((r = ours.read(buf)) >= 0) {
				out.write(buf, 0, r);
			}
			assertEquals(expected, new String(out.toByteArray(), US_ASCII));
		}
		// Read single bytes
		try (ByteArrayInputStream in = new ByteArrayInputStream(
				input.getBytes(US_ASCII));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream ours = new OursVersionInputStream(in, 7, diff3)) {
			int r;
			while ((r = ours.read()) >= 0) {
				out.write(r);
			}
			assertEquals(expected, new String(out.toByteArray(), US_ASCII));
		}
	}

	private void assertTransformation(String expected, String input)
			throws IOException {
		assertTransformation(expected, input, false);
	}

	private String longLine(int n) {
		StringBuilder b = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			b.append('a' + (i % 26));
		}
		return b.toString();
	}

	@Test
	public void testNoMarker() throws IOException {
		String input = "line 1\nline 2\nlast";
		assertTransformation(input, input);
	}

	@Test
	public void testEmpty() throws IOException {
		assertTransformation("", "");
	}

	@Test
	public void testSimple() throws IOException {
		String input = "<<<<<<<\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>>\n";
		String expected = "line 1\n";
		assertTransformation(expected, input);
	}

	@Test
	public void testSimpleMissingFinalLf() throws IOException {
		String input = "<<<<<<<\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>>";
		String expected = "line 1\n";
		assertTransformation(expected, input);
	}

	@Test
	public void testSimple2() throws IOException {
		String input = "<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n";
		String expected = "line 1\n";
		assertTransformation(expected, input);
	}

	@Test
	public void testSimple2MissingFinalLf() throws IOException {
		String input = "<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar";
		String expected = "line 1\n";
		assertTransformation(expected, input);
	}

	@Test
	public void testLongLines() throws IOException {
		for (int l = 8180; l < 8210; l++) {
			String longLine = longLine(l);
			String input = longLine + "0\n"
					+ "<<<<<<< " + longLine + '\n'
					+ longLine + "1\n"
					+ "=======\n" + longLine + "2\n"
					+ ">>>>>>> " + longLine + '\n';
			String expected = longLine + "0\n" + longLine + "1\n";
			assertTransformation(expected, input);
		}
	}

	@Test
	public void testSimpleWithMerged() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last\n";
		String expected = "line 0\nline 1\nlast\n";
		assertTransformation(expected, input);
	}

	@Test
	public void testSimpleWithMergedNoLfAtEnd() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last";
		String expected = "line 0\nline 1\nlast";
		assertTransformation(expected, input);
	}

	@Test
	public void testSimpleWithLessThan() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "<<<<<<<\n"
				+ "=======\n"
				+ "<<<<<<<\n"
				+ ">>>>>>> bar\n"
				+ "last";
		String expected = "line 0\n<<<<<<<\nlast";
		assertTransformation(expected, input);
	}

	@Test
	public void testDiff3Simple() throws IOException {
		String input = "<<<<<<< foo\n"
				+ "line 1\n"
				+ "|||||||base\n"
				+ "line base\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n";
		String expected = "line 1\n";
		assertTransformation(expected, input, true);
	}

	@Test
	public void testDiff3WithMerged() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "line 1\n"
				+ "|||||||\n"
				+ "line base\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last\n";
		String expected = "line 0\nline 1\nlast\n";
		assertTransformation(expected, input, true);
	}

	@Test
	public void testBinary() throws IOException {
		String original = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last\n";
		for (int i = 0; i < original.length(); i++) {
			String input = original.substring(0, i) + '\000' + original.substring(i);
			assertTransformation(input, input);
		}
	}

	@Test
	public void testUnexpectedOursMarker() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last";
		assertTransformation(input, input);
	}

	@Test
	public void testShortOursMarker() throws IOException {
		String input = "line 0\n"
				+ "<<<<<< foo\n"
				+ "line 1\n"
				+ "=======\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last";
		assertTransformation(input, input);
	}

	@Test
	public void testUnexpectedEqualsMarker() throws IOException {
		String input = "line 0\n"
				+ "<<<<<<< foo\n"
				+ "line 1\n"
				+ "========\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last";
		String expected = "line 0\n"
				+ "line 1\n"
				+ "========\n"
				+ "line 2\n"
				+ ">>>>>>> bar\n"
				+ "last";
		assertTransformation(expected, input);
	}
}
