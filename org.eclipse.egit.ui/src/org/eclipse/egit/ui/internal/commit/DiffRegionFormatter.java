/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Tobias Pfeifer (SAP AG) - customizable font and color for the first header line - https://bugs.eclipse.org/397723
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion.Type;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Diff region formatter class that builds up a list of
 * {@link DiffRegion} instances as each {@link FileDiff} is being written to
 * an {@link IDocument}.
 */
public class DiffRegionFormatter extends DiffFormatter {

	/**
	 * A text {@link Region} describing an interesting region in a unified diff.
	 */
	public static class DiffRegion extends Region {

		/** Constant {@value} indicating that no line number exists. */
		public static final int NO_LINE = -1;

		/**
		 * The type of a {@link DiffRegion}.
		 */
		public enum Type {

			/** Added line. */
			ADD,

			/** Removed line. */
			REMOVE,

			/** Hunk line. */
			HUNK,

			/** Headline. */
			HEADLINE,

			/** Header (after HEADLINE). */
			HEADER,

			/** A context line in a hunk. */
			CONTEXT,

			/** Other line. */
			OTHER,

		}

		private final @NonNull Type type;

		private final int aLine;

		private final int bLine;

		/**
		 * @param offset
		 * @param length
		 */
		public DiffRegion(int offset, int length) {
			this(offset, length, NO_LINE, NO_LINE, Type.OTHER);
		}

		/**
		 * @param offset
		 * @param length
		 * @param aLine
		 * @param bLine
		 * @param type
		 */
		public DiffRegion(int offset, int length, int aLine, int bLine,
				@NonNull Type type) {
			super(offset, length);
			this.type = type;
			this.aLine = aLine;
			this.bLine = bLine;
		}

		/**
		 * @return the {@link Type} of the region
		 */
		public @NonNull Type getType() {
			return type;
		}

		/**
		 * Returns the first logical line number of the region.
		 *
		 * @param side
		 *            to get the line number of
		 * @return the line number; -1 indicates that the range has no line
		 *         number for the given side.
		 */
		public int getLine(@NonNull DiffEntry.Side side) {
			if (DiffEntry.Side.NEW.equals(side)) {
				return bLine;
			}
			return aLine;
		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	/**
	 * Region giving access to the {@link FileDiff} that generated the content.
	 */
	public static class FileDiffRegion extends Region {

		private final @NonNull FileDiff diff;

		/**
		 * Creates a new {@link FileDiffRegion}.
		 *
		 * @param fileDiff
		 *            the range belongs to
		 * @param start
		 *            of the range
		 * @param length
		 *            of the range
		 */
		public FileDiffRegion(@NonNull FileDiff fileDiff,
				int start, int length) {
			super(start, length);
			this.diff = fileDiff;
		}

		/**
		 * Retrieves the {@link FileDiff}.
		 *
		 * @return the {@link FileDiff}
		 */
		@NonNull
		public FileDiff getDiff() {
			return diff;
		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public String toString() {
			return "[FileDiffRange " + diff.getPath() //$NON-NLS-1$
					+ ' ' + super.toString() + ']';
		}
	}

	private static class DocumentOutputStream extends OutputStream {

		private String charset;

		private IDocument document;

		private int offset;

		private StringBuilder lineBuffer = new StringBuilder();

		public DocumentOutputStream(IDocument document, int offset) {
			this.document = document;
			this.offset = offset;
		}

		private void write(String content) throws IOException {
			try {
				this.document.replace(this.offset, 0, content);
				this.offset += content.length();
			} catch (BadLocationException e) {
				throw new IOException(e.getMessage());
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (charset == null)
				lineBuffer.append(new String(b, off, len, "UTF-8")); //$NON-NLS-1$
			else
				lineBuffer.append(new String(b, off, len, charset));
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b });
		}

		@Override
		public void flush() throws IOException {
			flushLine();
		}

		protected void flushLine() throws IOException {
			if (lineBuffer.length() > 0) {
				write(lineBuffer.toString());
				lineBuffer.setLength(0);
			}
		}
	}

	private DocumentOutputStream stream;

	private List<DiffRegion> regions = new ArrayList<>();

	private List<FileDiffRegion> fileRegions = new ArrayList<>();

	private final int maxLines;

	private int linesWritten;

	private int lastNewLine;

	private int[] maximumLineNumbers = new int[] { DiffRegion.NO_LINE,
			DiffRegion.NO_LINE };

	/**
	 * @param document
	 * @param offset
	 */
	public DiffRegionFormatter(IDocument document, int offset) {
		this(document, offset, -1);
	}

	/**
	 * @param document
	 */
	public DiffRegionFormatter(IDocument document) {
		this(document, document.getLength(), -1);
	}

	/**
	 * @param document
	 * @param offset
	 * @param maxLines
	 */
	public DiffRegionFormatter(IDocument document, int offset,
			int maxLines) {
		super(new DocumentOutputStream(document, offset));
		this.stream = (DocumentOutputStream) getOutputStream();
		this.maxLines = maxLines;
		this.lastNewLine = DiffRegion.NO_LINE;
	}

	/**
	 * Write diff
	 *
	 * @param diff
	 * @return this formatter
	 * @throws IOException
	 */
	public DiffRegionFormatter write(FileDiff diff)
			throws IOException {
		Repository repository = diff.getRepository();
		this.stream.charset = CompareCoreUtils.getResourceEncoding(repository,
				diff.getPath());
		int start = stream.offset;
		diff.outputDiff(null, repository, this, true);
		flush();
		fileRegions.add(new FileDiffRegion(diff, start, stream.offset - start));
		return this;
	}

	/**
	 * Get diff regions, sorted by offset
	 *
	 * @return non-null but possibly empty array
	 */
	public DiffRegion[] getRegions() {
		return this.regions.toArray(new DiffRegion[this.regions.size()]);
	}

	/**
	 * Gets the file diff regions, sorted by offset.
	 *
	 * @return the regions; non-null but possibly empty
	 */
	public FileDiffRegion[] getFileRegions() {
		return this.fileRegions
				.toArray(new FileDiffRegion[this.fileRegions.size()]);
	}

	/**
	 * Retrieves the maximum line numbers for hunk lines.
	 *
	 * @return an array with two elements, index 0 being the maximum old line
	 *         number and index 1 the maximum new line number
	 */
	public int[] getMaximumLineNumbers() {
		return maximumLineNumbers.clone();
	}

	/**
	 * Create and add a new {@link DiffRegion} without line number information,
	 * coalescing it with the previous region,if any, if that has the same type
	 * and the two regions are adjacent.
	 *
	 * @param type
	 *            the {@link Type}
	 * @param start
	 *            start offset
	 * @param end
	 *            end offset
	 * @return added range
	 */
	protected DiffRegion addRegion(@NonNull Type type, int start, int end) {
		return addRegion(type, start, end, DiffRegion.NO_LINE,
				DiffRegion.NO_LINE);
	}

	/**
	 * Create and add a new {@link DiffRegion}, coalescing it with the previous
	 * region,if any, if that has the same type and the two regions are
	 * adjacent.
	 *
	 * @param type
	 *            the {@link Type}
	 * @param start
	 *            start offset
	 * @param end
	 *            end offset
	 * @param aLine
	 *            line number in the old version, or {@link DiffRegion#NO_LINE}
	 * @param bLine
	 *            line number in the new version, or {@link DiffRegion#NO_LINE}
	 * @return added range
	 */
	protected DiffRegion addRegion(@NonNull Type type, int start, int end,
			int aLine, int bLine) {
		maximumLineNumbers[0] = Math.max(aLine, maximumLineNumbers[0]);
		maximumLineNumbers[1] = Math.max(bLine, maximumLineNumbers[1]);
		if (bLine != DiffRegion.NO_LINE) {
			lastNewLine = bLine;
		}
		if (!regions.isEmpty()) {
			DiffRegion last = regions.get(regions.size() - 1);
			if (last.getType().equals(type)
					&& start == last.getOffset() + last.getLength()) {
				regions.remove(regions.size() - 1);
				start = last.getOffset();
				aLine = last.getLine(DiffEntry.Side.OLD);
				bLine = last.getLine(DiffEntry.Side.NEW);
			}
		}
		DiffRegion range = new DiffRegion(start, end - start, aLine, bLine,
				type);
		regions.add(range);
		return range;
	}

	@Override
	protected void writeHunkHeader(int aStartLine, int aEndLine,
			int bStartLine, int bEndLine) throws IOException {
		int start = stream.offset;
		if (!regions.isEmpty()) {
			DiffRegion last = regions.get(regions.size() - 1);
			int lastEnd = last.getOffset() + last.getLength();
			if (last.getType().equals(Type.HEADLINE) && lastEnd < start) {
				addRegion(Type.HEADER, lastEnd, start);
			}
		}
		super.writeHunkHeader(aStartLine, aEndLine, bStartLine, bEndLine);
		stream.flushLine();
		addRegion(Type.HUNK, start, stream.offset);
		lastNewLine = bStartLine - 1;
	}

	@Override
	protected void writeLine(char prefix, RawText text, int cur)
			throws IOException {
		if (maxLines > 0 && linesWritten > maxLines) {
			if (linesWritten == maxLines + 1) {
				int start = stream.offset;
				stream.flushLine();
				stream.write(
						NLS.bind(UIText.DiffStyleRangeFormatter_diffTruncated,
								Integer.valueOf(maxLines)));
				stream.write("\n"); //$NON-NLS-1$
				addRegion(Type.HEADLINE, start, stream.offset);
				linesWritten++;
			}
			return;
		}

		int start = stream.offset;
		super.writeLine(prefix, text, cur);
		stream.flushLine();
		if (prefix == ' ') {
			addRegion(Type.CONTEXT, start, stream.offset, cur, ++lastNewLine);
		} else if (prefix == '+') {
			addRegion(Type.ADD, start, stream.offset, DiffRegion.NO_LINE, cur);

		} else {
			addRegion(Type.REMOVE, start, stream.offset, cur,
					DiffRegion.NO_LINE);
		}
		linesWritten++;
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#formatGitDiffFirstHeaderLine(ByteArrayOutputStream
	 *      o, ChangeType type, String oldPath, String newPath)
	 */
	@Override
	protected void formatGitDiffFirstHeaderLine(ByteArrayOutputStream o,
			final ChangeType type, final String oldPath, final String newPath)
			throws IOException {
		stream.flushLine();
		int offset = stream.offset;
		int start = o.size();
		super.formatGitDiffFirstHeaderLine(o, type, oldPath, newPath);
		int end = o.size();
		addRegion(Type.HEADLINE, offset + start, offset + end);
	}

	@Override
	public void format(final EditList edits, final RawText a, final RawText b)
			throws IOException {
		// Flush header before formatting of edits begin
		stream.flushLine();
		super.format(edits, a, b);
	}
}
