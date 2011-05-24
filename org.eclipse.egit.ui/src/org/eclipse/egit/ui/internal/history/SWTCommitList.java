/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

class SWTCommitList extends PlotCommitList<SWTCommitList.SWTLane> {

	private static final RGB[] COMMIT_RGB = new RGB[] { new RGB(133, 166, 214),
			new RGB(221, 205, 93), new RGB(199, 134, 57),
			new RGB(131, 150, 98), new RGB(197, 123, 127),
			new RGB(139, 136, 140), new RGB(48, 135, 144),
			new RGB(190, 93, 66), new RGB(143, 163, 54), new RGB(180, 148, 74),
			new RGB(101, 101, 217), new RGB(72, 153, 119),
			new RGB(23, 101, 160), new RGB(132, 164, 118),
			new RGB(255, 230, 59), new RGB(136, 176, 70), new RGB(255, 138, 1),
			new RGB(123, 187, 95), new RGB(233, 88, 98), new RGB(93, 158, 254),
			new RGB(175, 215, 0), new RGB(140, 134, 142),
			new RGB(232, 168, 21), new RGB(0, 172, 191), new RGB(251, 58, 4),
			new RGB(63, 64, 255), new RGB(27, 194, 130), new RGB(0, 104, 183) };

	private final ArrayList<Color> allColors;

	private final LinkedList<Color> availableColors;

	SWTCommitList(final Display d) {
		allColors = new ArrayList<Color>(COMMIT_RGB.length);
		for (RGB rgb : COMMIT_RGB)
			allColors.add(new Color(d, rgb));
		availableColors = new LinkedList<Color>();
		repackColors();
	}

	public void dispose() {
		for (Color color : allColors)
			color.dispose();
	}

	private void repackColors() {
		availableColors.addAll(allColors);
	}

	@Override
	protected SWTLane createLane() {
		final SWTLane lane = new SWTLane();
		if (availableColors.isEmpty())
			repackColors();
		lane.color = availableColors.removeFirst();
		return lane;
	}

	@Override
	protected void recycleLane(final SWTLane lane) {
		availableColors.add(lane.color);
	}

	static class SWTLane extends PlotLane {
		Color color;
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && color.equals(((SWTLane)o).color);
		}

		@Override
		public int hashCode() {
			return super.hashCode() ^ color.hashCode();
		}
	}
}
