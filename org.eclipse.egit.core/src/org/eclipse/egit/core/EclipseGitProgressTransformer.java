/*******************************************************************************
 * Copyright (C) 2008, 2017 Shawn O. Pearce <spearce@spearce.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Thomas Wolf <thomas.wolf@paranor.ch> - use SubMonitor and infinite progress
 *******************************************************************************/
package org.eclipse.egit.core;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * A JGit {@link ProgressMonitor} that reports progress and cancellation via an
 * Eclipse {@link IProgressMonitor}.
 */
public class EclipseGitProgressTransformer implements ProgressMonitor {

	private static long UPDATE_INTERVAL = TimeUnit.MILLISECONDS.toMillis(100);

	// Because of the inconsistencies of JGit progress management (sometimes
	// start() not called, sometimes more beginTask() calls than advertised
	// in a previous start()), the best we can do to give some reasonable
	// visual feedback in the progress bar is an infinite progress
	// implementation.

	private final SubMonitor root;

	private String msg;

	private int lastWorked;

	private int lastShown;

	private int totalWork;

	private long lastUpdatedAt;

	/**
	 * Create a new progress monitor.
	 *
	 * @param eclipseMonitor
	 *            the Eclipse monitor we update.
	 */
	public EclipseGitProgressTransformer(final IProgressMonitor eclipseMonitor) {
		root = SubMonitor.convert(eclipseMonitor);
	}

	@Override
	public void start(final int totalTasks) {
		// Nothing to do
	}

	@Override
	public void beginTask(final String name, final int total) {
		msg = name;
		lastWorked = 0;
		lastShown = 0;
		lastUpdatedAt = 0;
		totalWork = total <= 0 ? UNKNOWN : total;
		root.subTask(msg);
	}

	@Override
	public void update(final int work) {
		if (work <= 0) {
			return;
		}
		final int cmp = lastWorked + work;
		if (totalWork == UNKNOWN) {
			long now = System.currentTimeMillis();
			if (now < lastUpdatedAt || now - lastUpdatedAt > UPDATE_INTERVAL) {
				root.subTask(msg + ", " + cmp); //$NON-NLS-1$
				root.setWorkRemaining(100);
				root.worked(1);
				lastUpdatedAt = now;
			}
		} else if (lastShown == 0
				|| cmp * 100 / totalWork != lastShown * 100 / totalWork) {
			// Percentage changed: update the subTask message
			final StringBuilder m = new StringBuilder();
			m.append(msg);
			m.append(": "); //$NON-NLS-1$
			while (m.length() < 25) {
				m.append(' ');
			}
			final String twstr = String.valueOf(totalWork);
			String cmpstr = String.valueOf(cmp);
			while (cmpstr.length() < twstr.length()) {
				cmpstr = ' ' + cmpstr;
			}
			final int pcnt = (cmp * 100 / totalWork);
			if (pcnt < 100) {
				m.append(' ');
			}
			if (pcnt < 10) {
				m.append(' ');
			}
			m.append(pcnt);
			m.append("% ("); //$NON-NLS-1$
			m.append(cmpstr);
			m.append('/');
			m.append(twstr);
			m.append(')');

			root.subTask(m.toString());
			root.setWorkRemaining(100);
			root.worked(1);
			lastShown = cmp;
		}
		lastWorked = cmp;
	}

	@Override
	public void endTask() {
		// Nothing to do
	}

	@Override
	public boolean isCancelled() {
		return root.isCanceled();
	}
}
