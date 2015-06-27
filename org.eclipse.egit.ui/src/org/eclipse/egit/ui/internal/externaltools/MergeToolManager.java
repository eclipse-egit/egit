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
public class MergeToolManager extends BaseToolManager {

	private static MergeToolManager instance = null;

	/**
	 * @return the instance of ToolManager
	 */
	public static synchronized MergeToolManager getInstance() {
		if (MergeToolManager.instance == null) {
			MergeToolManager.instance = new MergeToolManager();
		}
		return MergeToolManager.instance;
	}

	private MergeToolManager() {
		/*
		 * merge tools defined in this git version, are added here...
		 *
		 * $ git --version git version 1.9.5.msysgit.1
		 *
		 * $ git mergetool --tool-help 'git mergetool --tool=<tool>' may be set
		 * to one of the following: vimdiff vimdiff2
		 *
		 * user-defined: diffmerge diffwrap meld s7p-merge
		 *
		 * The following tools are valid, but not currently available: araxis
		 * bc3 codecompare defaults deltawalker diffmerge diffuse ecmerge emerge
		 * gvimdiff gvimdiff2 kdiff3 meld opendiff p4merge tkdiff tortoisemerge
		 * vim xxdiff
		 *
		 * Some of the tools listed above only work in a windowed environment.
		 * If run in a terminal-only session, they will fail.
		 */
		addPreDefinedTool("kdiff3", "kdiff3", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$BASE\" \"$LOCAL\" \"$REMOTE\" -o \"$MERGED\""); //$NON-NLS-1$
	}

}
