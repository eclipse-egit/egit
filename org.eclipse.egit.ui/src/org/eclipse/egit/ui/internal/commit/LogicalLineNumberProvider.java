/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry;

/**
 * An {@link ILogicalLineNumberProvider} that uses a viewer's
 * {@link DiffDocument} to translate from physical to logical line numbers.
 */
public class LogicalLineNumberProvider implements ILogicalLineNumberProvider {

	private final @NonNull DiffEntry.Side side;

	private final @NonNull ITextViewer viewer;

	/**
	 * Creates a new {@link LogicalLineNumberProvider}.
	 *
	 * @param side
	 *            of the diff to report line numbers for
	 * @param viewer
	 *            to get the document from
	 */
	public LogicalLineNumberProvider(@NonNull DiffEntry.Side side,
			@NonNull ITextViewer viewer) {
		this.side = side;
		this.viewer = viewer;
	}

	@Override
	public int getLogicalLine(int lineNumber) {
		IDocument document = viewer.getDocument();
		if (document instanceof DiffDocument) {
			return ((DiffDocument) document).getLogicalLine(lineNumber, side);
		}
		return lineNumber;
	}

	@Override
	public int getMaximum() {
		IDocument document = viewer.getDocument();
		if (document instanceof DiffDocument) {
			return ((DiffDocument) document).getMaximumLineNumber(side);
		}
		return -1;
	}
}
