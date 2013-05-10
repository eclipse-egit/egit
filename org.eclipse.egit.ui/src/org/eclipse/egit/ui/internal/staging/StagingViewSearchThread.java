/*******************************************************************************
 * Copyright (C) 2013, Stephen Elsemore <selsemore@collab.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

/**
 * Thread for file search
 */
public class StagingViewSearchThread extends Thread {
	private StagingView stagingView;

	private static final Object EXEC_LOCK = new Object();

	private volatile static int globalThreadIx = 0;

	private int currentThreadIx;

	/**
	 * @param stagingView
	 */
	public StagingViewSearchThread(StagingView stagingView) {
		super("staging_view_search_thread" + ++globalThreadIx); //$NON-NLS-1$
		this.stagingView = stagingView;
		currentThreadIx = globalThreadIx;
	}

	public void run() {
		synchronized (EXEC_LOCK) {
			execFind();
		}
	}

	private void execFind() {
		if (currentThreadIx < globalThreadIx) {
			return;
		}
		stagingView.refreshViewers();
	}
}
