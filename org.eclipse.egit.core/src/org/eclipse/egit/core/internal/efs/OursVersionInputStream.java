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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.io.UnionInputStream;

/**
 * An {@link java.io.InputStream} that auto-resolves all conflicts as identified
 * by git conflict markers to the 'ours' version.
 */
public class OursVersionInputStream extends InputStream {

	private static final int BUFFER_SIZE = 8192;

	private final int conflictMarkerSize;

	private final boolean diff3Style;

	private InputStream in;

	private boolean initialized;

	private boolean binary;

	private byte[] buffer = new byte[BUFFER_SIZE];

	/** Current position in the buffer. */
	private int pos;

	/** Position after the last valid byte in the buffer. */
	private int end;

	/** Position after the next LF, or 'end' if there is none. */
	private int lineEnd;

	/** Whether there is a LF before 'lineEnd'. */
	private boolean isFullLine = true;

	/** Current processing state. */
	private State state = State.MERGED;

	/** Set to the next state if we have only a partial MARKER line. */
	private State nextState;

	private enum State {
		/**
		 * Within a marker; can occur only if we didn't have the full marker
		 * line in the buffer.
		 */
		MARKER,

		/** Outside of any conflict. */
		MERGED,

		/** In the 'ours' section of a conflict. */
		OURS,

		/** In the 'base' section of a conflict in diff3 style. */
		BASE,

		/** In the 'theirs' section of a conflict. */
		THEIRS
	}

	/**
	 * Creates a new {@link OursVersionInputStream}. If the given
	 * {@link InputStream} {@code in} is detected as a binary stream (with the
	 * usual git heuristic of looking for a zero byte in the first 8kB), the
	 * stream passes through all bytes unchanged. For text input, it passes
	 * through all bytes from merged and 'ours' lines while swallowing all bytes
	 * from conflict markers and base (if {@code diff3Style == true}) and
	 * 'theirs' lines.
	 *
	 * @param in
	 *            {@link InputStream} to filter
	 * @param conflictMarkerSize
	 *            size of the expected conflict markers, should be git attribute
	 *            {@code conflict-marker-size}
	 * @param diff3Style
	 *            whether to expect diff3Style markers, should be {@code true}
	 *            if git config {@code merge.conflictstyle} is {@code diff3}
	 */
	public OursVersionInputStream(InputStream in, int conflictMarkerSize,
			boolean diff3Style) {
		this.conflictMarkerSize = conflictMarkerSize;
		this.diff3Style = diff3Style;
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		if (!initialized) {
			initialize();
		}
		if (binary) {
			return in.read();
		}
		if (pos < 0) {
			return -1;
		}
		if (pos == lineEnd) {
			getLine();
			if (pos < 0) {
				return -1;
			}
		}
		return buffer[pos++] & 0xFF;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (!initialized) {
			initialize();
		}
		if (binary) {
			return in.read(b, off, len);
		}
		if (pos < 0) {
			return -1;
		}
		if (pos == lineEnd) {
			getLine();
			if (pos < 0) {
				return -1;
			}
		}
		int l = Math.min(lineEnd - pos, len);
		System.arraycopy(buffer, pos, b, off, l);
		pos += l;
		return l;
	}

	@Override
	public void close() throws IOException {
		buffer = null;
		super.close();
	}

	private void initialize() throws IOException {
		byte[] buf = new byte[BUFFER_SIZE];
		int len = IO.readFully(in, buf, 0);
		if (len <= 0) {
			pos = -1;
		} else {
			for (int i = 0; i < len; i++) {
				if (buf[i] == 0) {
					binary = true;
					break;
				}
			}
			in = new UnionInputStream(new ByteArrayInputStream(buf, 0, len),
					in);
		}
		initialized = true;
	}

	private void getLine() throws IOException {
		for (;;) {
			if (pos == end) {
				pos = 0;
				end = IO.readFully(in, buffer, 0);
				if (end == 0) {
					pos = -1;
					break;
				}
			}
			// Find the next LF
			int lf = -1;
			for (int i = pos; i < end; i++) {
				if (buffer[i] == '\n') {
					lf = i;
					break;
				}
			}
			boolean wasFullLine = isFullLine;
			if (lf < 0) {
				isFullLine = false;
				lineEnd = end;
			} else {
				lineEnd = lf + 1;
				isFullLine = true;
			}
			if (!wasFullLine) {
				// We had a partial line before. Previous state determines fate.
				switch (state) {
				case MARKER:
					pos = lineEnd;
					if (isFullLine) {
						state = nextState;
						nextState = null;
					}
					continue;
				case MERGED:
				case OURS:
					return;
				case BASE:
				case THEIRS:
					// Skip it
					pos = lineEnd;
					continue;
				default:
					throw new IllegalStateException();
				}
			}
			// 'pos' is at the beginning of a new line.
			if (!isFullLine && end == buffer.length
					&& end - pos < conflictMarkerSize + 1 && pos > 0) {
				// Copy the partial stuff to the front and re-fill
				System.arraycopy(buffer, pos, buffer, 0, end - pos);
				end -= pos;
				pos = 0;
				int l = IO.readFully(in, buffer, end);
				int i = end;
				end += l;
				lf = -1;
				for (; i < end; i++) {
					if (buffer[i] == '\n') {
						lf = i;
						break;
					}
				}
				if (lf < 0) {
					lineEnd = end;
					isFullLine = false;
				} else {
					lineEnd = lf + 1;
					isFullLine = true;
				}
			}
			switch (state) {
			case MERGED:
				if (isMarker('<')) {
					pos = lineEnd;
					state = State.OURS;
				} else {
					return;
				}
				break;
			case OURS:
				if (diff3Style && isMarker('|')) {
					pos = lineEnd;
					state = State.BASE;
				} else if (isMarker('=')) {
					pos = lineEnd;
					state = State.THEIRS;
				} else {
					return;
				}
				break;
			case BASE:
				if (isMarker('=')) {
					pos = lineEnd;
					state = State.THEIRS;
				} else {
					pos = lineEnd;
					continue;
				}
				break;
			case THEIRS:
				if (isMarker('>')) {
					pos = lineEnd;
					state = State.MERGED;
				} else {
					pos = lineEnd;
					continue;
				}
				break;
			case MARKER:
			default:
				throw new IllegalStateException();
			}
			// We had a marker
			if (!isFullLine) {
				nextState = state;
				state = State.MARKER;
			}
		}
	}

	private boolean isMarker(char marker) {
		if (lineEnd - pos < conflictMarkerSize) {
			return false;
		}
		for (int i = pos + conflictMarkerSize - 1; i >= pos; i--) {
			if (buffer[i] != marker) {
				return false;
			}
		}
		if (lineEnd - pos > conflictMarkerSize) {
			return buffer[pos + conflictMarkerSize] != marker;
		}
		return true;
	}
}
