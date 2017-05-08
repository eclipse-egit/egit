/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry;

/**
 * A {@link LineNumberRulerColumn} that uses an
 * {@link ILogicalLineNumberProvider} to determine the line numbers to show.
 */
public class LogicalLineNumberRulerColumn extends LineNumberRulerColumn {

	private ILogicalLineNumberProvider logicalLineNumberProvider;

	private final @NonNull DiffEntry.Side side;

	/**
	 * Zoom level for workaround for HiDPI line number rulers on macOS retina.
	 *
	 * @see <a href="https://bugs.eclipse.org/516322">Bug 516322</a>
	 */
	int zoom = 100;

	/**
	 * @param side
	 */
	public LogicalLineNumberRulerColumn(@NonNull DiffEntry.Side side) {
		this.side = side;
	}

	/**
	 * @return the {@link DiffEntry} side
	 */
	protected @NonNull DiffEntry.Side getSide() {
		return side;
	}

	/**
	 * Retrieves the {@link ILogicalLineNumberProvider} to use. This
	 * implementation returns a {@link LogicalLineNumberProvider}.
	 *
	 * @return the {@link ILogicalLineNumberProvider}
	 */
	protected ILogicalLineNumberProvider getLogicalLineNumberProvider() {
		if (logicalLineNumberProvider == null) {
			ITextViewer viewer = getParentRuler().getTextViewer();
			Assert.isNotNull(viewer);
			logicalLineNumberProvider = new LogicalLineNumberProvider(getSide(),
					viewer);
		}
		return logicalLineNumberProvider;
	}

	@Override
	protected String createDisplayString(int line) {
		int logicalLine = getLogicalLineNumberProvider().getLogicalLine(line);
		return logicalLine < 0 ? "" : Integer.toString(logicalLine + 1); //$NON-NLS-1$
	}

	@Override
	protected int computeNumberOfDigits() {
		int max = getLogicalLineNumberProvider().getMaximum();
		int digits = 2;
		while (max > Math.pow(10, digits) - 1) {
			++digits;
		}
		return digits;
	}

	/**
	 * Enable workaround for HiDPI line number rulers on macOS retina. This
	 * method will override an internal method in Oxygen (4.7).
	 *
	 * @return true
	 * @see <a href="https://bugs.eclipse.org/516322">Bug 516322</a>
	 */
	protected boolean internalSupportsZoomedPaint() {
		return true;
	}

	/**
	 * Store zoom level for workaround for HiDPI line number rulers on macOS
	 * retina. This method will override an internal method in Oxygen (4.7).
	 *
	 * @param zoomLevel
	 *            the zoom level to use for drawing operations
	 * @see <a href="https://bugs.eclipse.org/516322">Bug 516322</a>
	 */
	protected void internalSetZoom(int zoomLevel) {
		this.zoom = zoomLevel;
	}
}
