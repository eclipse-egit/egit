/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Shell;

/**
 * Wrapper for ReleaseStartHandler
 */
public final class ReleaseStartFromCommitHandler extends SelectionAdapter {
	private ReleaseStartHandler releaseStartHandler;
	private GitFlowRepository gfRepo;
	private String startCommitSha1;
	private Shell activeShell;

	/**
	 * @param activeShell
	 * @param startCommitSha1
	 * @param gfRepo
	 *
	 */
	public ReleaseStartFromCommitHandler(GitFlowRepository gfRepo, String startCommitSha1, Shell activeShell) {
		this.gfRepo = gfRepo;
		this.startCommitSha1 = startCommitSha1;
		this.activeShell = activeShell;
		releaseStartHandler = new ReleaseStartHandler();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		releaseStartHandler.doExecute(gfRepo, startCommitSha1, activeShell);
	}
}
