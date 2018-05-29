/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;

class SWTCommitList extends PlotCommitList<SWTCommitList.SWTLane> implements DisposeListener {

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

	private final Control control;

	SWTCommitList(final Control control, final ResourceManager resources) {
		this.control = control;
		allColors = new ArrayList<>(COMMIT_RGB.length);
		for (RGB rgb : COMMIT_RGB)
			allColors.add(resources.createColor(rgb));
		availableColors = new LinkedList<>();
		repackColors();
		control.addDisposeListener(this);
	}

	public void dispose() {
		Job clearJob = new Job("Clearing commit list") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				synchronized (SWTCommitList.this) {
					clear();
				}
				return Status.OK_STATUS;
			}
		};
		clearJob.setSystem(true);
		clearJob.schedule();

		if (!control.isDisposed())
			control.removeDisposeListener(this);
	}

	private void repackColors() {
		availableColors.addAll(allColors);
	}

	@Override
	protected SWTLane createLane() {
		if (availableColors.isEmpty())
			repackColors();
		return new SWTLane(availableColors.removeFirst());
	}

	@Override
	protected void recycleLane(final SWTLane lane) {
		availableColors.add(lane.color);
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		dispose();
	}

	static class SWTLane extends PlotLane {
		private static final long serialVersionUID = 1L;

		final Color color;

		public SWTLane(final Color color) {
			this.color = color;
		}
	}
}
