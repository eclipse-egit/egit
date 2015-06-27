/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.externaltools;

/**
 * @author anb0s
 *
 */
public class DiffToolManager extends BaseToolManager {

	private static DiffToolManager instance = null;

	/**
	 * @return the instance of ToolManager
	 */
	public static synchronized DiffToolManager getInstance() {
		if (DiffToolManager.instance == null) {
			DiffToolManager.instance = new DiffToolManager();
		}
		return DiffToolManager.instance;
	}

	private DiffToolManager() {
		/*
		 * diff tools defined in this git version, are added here...
		 *
		 * $ git --version git version 1.9.5.msysgit.1
		 *
		 * $ git difftool --tool-help 'git difftool --tool=<tool>' may be set to
		 * one of the following: vimdiff vimdiff2
		 *
		 * user-defined: diffmerge diffmerge diffwrap diffwrap meld meld
		 * s7p-diff s7p-merge
		 *
		 * The following tools are valid, but not currently available: araxis
		 * bc3 codecompare defaults deltawalker diffmerge diffuse ecmerge emerge
		 * gvimdiff gvimdiff2 kdiff3 kompare meld opendiff p4merge tkdiff vim
		 * xxdiff
		 */
		addPreDefinedTool("kdiff3", "kdiff3", "\"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
