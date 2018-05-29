/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.egit.internal.mylyn.ui.CommitHyperlinkDetector;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class CommitHyperlinkDetectorTest {

	private static final String OTHER_EXAMPLE_ID = "3de38c8898c74b867cb6f06f7907e0719d9d4c0c";
	private static final String EXAMPLE_ID = "2de0ab486c66566ae1ad36b73bfc9d99e14eb195";
	private TextViewer textViewer;
	private CommitHyperlinkDetector detector;

	@Test
	public void testNoDocument() {
		textViewer.setDocument(null);
		IHyperlink[] hyperlinks = detectHyperlinks(0,0);
		assertNull(hyperlinks);
	}

	@Test
	public void testBadLocation() {
		textViewer.setDocument(null);
		IHyperlink[] hyperlinks = detectHyperlinks(10,0);
		assertNull(hyperlinks);
	}

	@Test
	public void testEmpty() {
		setText("");
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertNull(hyperlinks);
	}

	@Test
	public void testSimpleId() {
		setText(EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testMultiId() {
		setText(EXAMPLE_ID + " and " + OTHER_EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(2, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
		assertEquals(OTHER_EXAMPLE_ID, hyperlinks[1].getHyperlinkText());
	}

	@Test
	public void testEndLine() {
		setText("Merged as " + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testMiddleLine() {
		setText("Merged as " + EXAMPLE_ID + " and something else");
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testBeginSentence() {
		setText("end of sentence." + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testEndSentence() {
		setText("Merged as " + EXAMPLE_ID + ".");
		IHyperlink[] hyperlinks = detectHyperlinks();
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testOffsetMiddle() {
		setText(EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks(3,0);
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
	}

	@Test
	public void testOffsetOff() {
		setText("some bla " + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks(3,0);
		assertNull(hyperlinks);
	}

	@Test
	public void testMultiLine() {
		setText("Test multi-line text\n" + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks(0,textViewer.getDocument().getLength());
		assertEquals(1, hyperlinks.length);
		assertEquals(EXAMPLE_ID, hyperlinks[0].getHyperlinkText());
		assertEquals(new Region(21, EXAMPLE_ID.length()), hyperlinks[0].getHyperlinkRegion());
	}

	@Test
	public void testGerritId() {
		setText("I" + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks(0,textViewer.getDocument().getLength());
		assertNull(hyperlinks);
	}

	@Test
	public void testGerritIdWithinText() {
		setText("abc I" + EXAMPLE_ID);
		IHyperlink[] hyperlinks = detectHyperlinks(5,textViewer.getDocument().getLength());
		assertNull(hyperlinks);
	}


	private IHyperlink[] detectHyperlinks() {
		return detectHyperlinks(0, textViewer.getDocument().getLength());
	}

	private IHyperlink[] detectHyperlinks(int offset, int length) {
		return detector.detectHyperlinks(textViewer,
				new Region(offset, length), false);
	}

	private void setText(String text) {
		textViewer.getDocument().set(text);
	}

	@Before
	public void setUp() throws Exception {
		detector = new CommitHyperlinkDetector();
		Shell shell = new Shell();
		textViewer = new TextViewer(shell, SWT.NONE);
		textViewer.setDocument(new Document());
	}

}
