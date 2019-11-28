/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.history.SWTCommitList.SWTLane;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revplot.AbstractPlotRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

class SWTPlotRenderer extends AbstractPlotRenderer<SWTLane, Color> {

	private static final RGB OUTER_HEAD = new RGB(0, 128, 0);

	private static final RGB INNER_HEAD = new RGB(188, 220, 188);

	private static final RGB OUTER_TAG = new RGB(121, 120, 13);

	private static final RGB INNER_TAG = new RGB(249, 255, 199);

	private static final RGB OUTER_ANNOTATED = new RGB(104, 78, 0);

	private static final RGB INNER_ANNOTATED = new RGB(255, 239, 192);

	private static final RGB OUTER_REMOTE = new RGB(80, 80, 80);

	private static final RGB INNER_REMOTE = new RGB(225, 225, 225);

	private static final RGB OUTER_OTHER = new RGB(30, 30, 30);

	private static final RGB INNER_OTHER = new RGB(250, 250, 250);

	private static final int MAX_LABEL_LENGTH = 18;

	private static final String ELLIPSIS = "\u2026"; // ellipsis "..." (in UTF-8) //$NON-NLS-1$

	private final Color sys_black;

	private final Color sys_gray;

	private final Color sys_white;

	private final Color commitDotFill;

	private final Color commitDotOutline;

	/**
	 * Map from ref name to its label coordinates
	 */
	private final Map<String, Point> labelCoordinates = new HashMap<>();

	/**
	 * Set of ref names that are shown as an ellipsis
	 */
	private final Set<String> ellipsisTags = new HashSet<>();

	private int textHeight;

	private final ResourceManager resources;

	GC g;

	int cellX;

	int cellY;

	Color cellFG;

	Color cellBG;

	private Ref headRef;

	/**
	 * Number of tags of the current commit being rendered. Reset after each
	 * commit.
	 */
	private int tagCount = 0;

	SWTPlotRenderer(final Display d, final ResourceManager resources) {
		this.resources = resources;
		sys_black = d.getSystemColor(SWT.COLOR_BLACK);
		sys_gray = d.getSystemColor(SWT.COLOR_GRAY);
		sys_white = d.getSystemColor(SWT.COLOR_WHITE);

		commitDotFill = resources.createColor(new RGB(220, 220, 220));
		commitDotOutline = resources.createColor(new RGB(110, 110, 110));
	}

	void paint(final Event event) {
		g = event.gc;

		cellX = event.x;
		cellY = event.y;
		cellFG = g.getForeground();
		cellBG = g.getBackground();
		if (textHeight == 0)
			textHeight = g.stringExtent("/").y; //$NON-NLS-1$

		final TableItem ti = (TableItem) event.item;
		SWTCommit commit = (SWTCommit) ti.getData();
		try {
			commit.parseBody();
			headRef = commit.getHead();
		} catch (IOException e) {
			Activator.error("Error parsing body", e); //$NON-NLS-1$
			return;
		}
		paintCommit(commit , event.height);
	}

	@Override
	protected void drawLine(final Color color, final int x1, final int y1,
			final int x2, final int y2, final int width) {
		g.setForeground(color);
		g.setLineWidth(width);
		g.drawLine(cellX + x1, cellY + y1, cellX + x2, cellY + y2);
	}

	protected void drawDot(final Color outline, final Color fill, final int x,
			final int y, final int w, final int h) {
		int dotX = cellX + x + 2;
		int dotY = cellY + y + 1;
		int dotW = w - 2;
		int dotH = h - 2;
		g.setBackground(fill);
		g.fillOval(dotX, dotY, dotW, dotH);
		g.setForeground(outline);
		g.setLineWidth(2);
		g.drawOval(dotX, dotY, dotW, dotH);
	}

	@Override
	protected void drawCommitDot(final int x, final int y, final int w,
			final int h) {
		drawDot(commitDotOutline, commitDotFill, x, y, w, h);
	}

	@Override
	protected void drawBoundaryDot(final int x, final int y, final int w,
			final int h) {
		drawDot(sys_gray, sys_white, x, y, w, h);
	}

	@Override
	protected void drawText(final String msg, final int x, final int y) {
		String text = msg.replace('\t', ' ');
		final Point textsz = g.textExtent(text);
		final int texty = (y - textsz.y) / 2;
		g.setForeground(cellFG);
		g.setBackground(cellBG);
		g.drawString(text, cellX + x, cellY + texty, true);

		tagCount = 0;
	}

	@Override
	protected int drawLabel(int x, int y, Ref ref) {
		String txt;
		String name = ref.getName();
		boolean tag = false;
		boolean branch = false;
		RGB labelOuter;
		RGB labelInner;
		if (name.startsWith(Constants.R_HEADS)) {
			branch = true;
			labelOuter = OUTER_HEAD;
			labelInner = INNER_HEAD;
			txt = name.substring(Constants.R_HEADS.length());
		} else if (name.startsWith(Constants.R_REMOTES)) {
			branch = true;
			labelOuter = OUTER_REMOTE;
			labelInner = INNER_REMOTE;
			txt = name.substring(Constants.R_REMOTES.length());
		} else if (name.startsWith(Constants.R_TAGS)) {
			tagCount++;

			tag = true;
			if (ref.getPeeledObjectId() != null) {
				labelOuter = OUTER_ANNOTATED;
				labelInner = INNER_ANNOTATED;
			} else {
				labelOuter = OUTER_TAG;
				labelInner = INNER_TAG;
			}

			int maxNumberOfTags = 1;
			if (tagCount == maxNumberOfTags) {
				txt = name.substring(Constants.R_TAGS.length());
			} else if (tagCount == maxNumberOfTags + 1) {
				txt = ELLIPSIS;
				ellipsisTags.add(name);
			} else {
				// Don't draw additional tags, they are shown when hovering the
				// ellipsis
				return 0;
			}
		} else {
			labelOuter = OUTER_OTHER;
			labelInner = INNER_OTHER;

			if (name.startsWith(Constants.R_REFS))
				txt = name.substring(Constants.R_REFS.length());
			else
				txt = name; // HEAD and such
		}

		int maxLength;
		if (tag)
			maxLength = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.HISTORY_MAX_TAG_LENGTH);
		else if (branch)
			maxLength = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.HISTORY_MAX_BRANCH_LENGTH);
		else
			maxLength = MAX_LABEL_LENGTH;
		if (txt.length() > maxLength) {
			// Account for the ellipsis length
			int textLength = maxLength - 3;
			if (Activator.getDefault().getPreferenceStore()
					.getBoolean(UIPreferences.HISTORY_CUT_AT_START))
				txt = ELLIPSIS + txt.substring(txt.length() - textLength);
			else
				txt = txt.substring(0, textLength) + ELLIPSIS;
		}

		// highlight checked out branch
		Font oldFont = g.getFont();
		boolean isHead = isHead(name);
		if (isHead)
			g.setFont(CommitGraphTable.highlightFont());

		Point textsz = g.stringExtent(txt);
		int arc = textsz.y / 2;
		final int texty = (y * 2 - textsz.y) / 2;
		final int outerWidth = textsz.x + 7;

		// Draw backgrounds
		g.setLineWidth(1);

		g.setBackground(sys_white);
		g.fillRoundRectangle(cellX + x + 1, cellY + texty, textsz.x + 6,
				textsz.y + 1, arc, arc);

		g.setBackground(resources.createColor(labelInner));
		g.fillRoundRectangle(cellX + x + 2, cellY + texty + 1, textsz.x + 4,
				textsz.y - 2, arc - 1, arc - 1);

		g.setForeground(resources.createColor(labelOuter));
		g.drawRoundRectangle(cellX + x, cellY + texty - 1, outerWidth,
				textsz.y + 1, arc, arc);

		g.setForeground(sys_black);

		// Draw text
		g.drawString(txt, cellX + x + 4, cellY + texty, true);

		if (isHead)
			g.setFont(oldFont);

		labelCoordinates.put(name, new Point(x, x + outerWidth));
		return 10 + textsz.x;
	}

	private boolean isHead(String name) {
		boolean isHead = false;
		if (headRef != null) {
			String headRefName = headRef.getLeaf().getName();
			if (name.equals(headRefName))
				isHead = true;
		}
		return isHead;
	}

	@Override
	protected Color laneColor(final SWTLane myLane) {
		return myLane != null ? myLane.color : sys_black;
	}

	/**
	 * Obtain the horizontal span of {@link Ref} label in pixels
	 *
	 * For example, let's assume the SWTCommit has two {@link Ref}s (see
	 * {@link SWTCommit#getRef(int)}, which are rendered as two labels. The
	 * first label may span from 15 to 54 pixels in x direction, while the
	 * second one may span from 58 to 76 pixels.
	 *
	 * This can be used to determine if the mouse is positioned over a label.
	 *
	 * @param ref
	 * @return a Point where x and y designate the start and end x position of
	 *         the label
	 */
	Point getRefHSpan(Ref ref) {
		return labelCoordinates.get(ref.getName());
	}

	boolean isShownAsEllipsis(Ref ref) {
		return ellipsisTags.contains(ref.getName());
	}
}
