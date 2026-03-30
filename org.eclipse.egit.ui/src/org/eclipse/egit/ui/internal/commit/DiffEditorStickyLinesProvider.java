/*******************************************************************************
 *  Copyright (c) 2026 Stephan Wahlbrink and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.commit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion.Type;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.texteditor.stickyscroll.IStickyLine;
import org.eclipse.ui.texteditor.stickyscroll.IStickyLinesProvider;
import org.eclipse.ui.texteditor.stickyscroll.StickyLine;

/**
 * A {@link IStickyLinesProvider} for the {@link DiffEditor}.
 */
public class DiffEditorStickyLinesProvider implements IStickyLinesProvider {

	private static class StickyLinesCollector {

		private final DiffViewer sourceViewer;

		private final DiffDocument document;

		private final ArrayList<IStickyLine> stickyLines = new ArrayList<>();

		private final int requestedLineStartOffset;

		private final int requestedLineEndOffset;

		private int lastLineNumber;

		protected StickyLinesCollector(final DiffViewer sourceViewer,
				final DiffDocument document, final int requestedLineNumber)
				throws BadLocationException {
			this.sourceViewer = sourceViewer;
			this.document = document;

			this.requestedLineStartOffset = document
					.getLineOffset(requestedLineNumber);
			this.requestedLineEndOffset = this.requestedLineStartOffset
					+ document.getLineLength(requestedLineNumber);
			this.lastLineNumber = -1;
		}

		public final int getRequestedLineStartOffset() {
			return this.requestedLineStartOffset;
		}

		public boolean addStickyLine(final int startOffset, final int endOffset,
				final String contentType) throws BadLocationException {
			final int lineNumber = this.document.getLineOfOffset(startOffset);
			if (startOffset < requestedLineStartOffset
					&& endOffset > this.requestedLineEndOffset
					&& lineNumber > this.lastLineNumber
					&& toWidgetLine(lineNumber) >= 0) {
				this.stickyLines.add(new DiffStickyLine(lineNumber,
						this.sourceViewer, contentType));
				this.lastLineNumber = lineNumber;
				return true;
			}
			return false;
		}

		public boolean addStickyLine(final IRegion region,
				final String contentType) throws BadLocationException {
			return addStickyLine(region.getOffset(),
					region.getOffset() + region.getLength(), contentType);
		}

		private int toWidgetLine(final int line) {
			if (this.sourceViewer instanceof final ITextViewerExtension5 extension) {
				return extension.modelLine2WidgetLine(line);
			}
			return line;
		}

	}

	@Override
	public List<IStickyLine> getStickyLines(ISourceViewer sourceViewer,
			int lineNumber, StickyLinesProperties properties) {
		if (sourceViewer instanceof final DiffViewer diffViewer && sourceViewer
				.getDocument() instanceof final DiffDocument diffDoc) {
			try {
				StickyLinesCollector collector = new StickyLinesCollector(
						diffViewer, diffDoc, lineNumber);

				FileDiffRegion fileRegion = diffDoc.findFileRegion(
						collector.getRequestedLineStartOffset());
				if (fileRegion != null && collector.addStickyLine(fileRegion,
						DiffDocument.HEADLINE_CONTENT_TYPE)) {

					int offset = collector.getRequestedLineStartOffset();
					while (offset > fileRegion.getOffset()) {
						DiffRegion diffRegion = diffDoc.findRegion(offset);
						switch (diffRegion.getType()) {
						case Type.HUNK:
							collector.addStickyLine(diffRegion.getOffset(),
									fileRegion.getOffset()
											+ fileRegion.getLength(),
									DiffDocument.HUNK_CONTENT_TYPE);
							break;
						case Type.HEADLINE:
						case Type.HEADER:
							break;
						default:
							offset = diffRegion.getOffset() - 1;
							continue;
						}
						break;
					}
				}

				return collector.stickyLines;
			} catch (BadLocationException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Sticky line for diffs.
	 *
	 * It adds full-width line background extending the sticky line with spaces.
	 */
	private static class DiffStickyLine extends StickyLine {

		private String contentType;

		DiffStickyLine(int lineNumber, DiffViewer sourceViewer,
				String contentType) {
			super(lineNumber, sourceViewer);
			this.contentType = contentType;
		}

		/* @Override */
		@Override
		public Color getBackgroundColor() {
			return ((DiffViewer) sourceViewer).getBackgroundColor(contentType);
		}

	}

}
