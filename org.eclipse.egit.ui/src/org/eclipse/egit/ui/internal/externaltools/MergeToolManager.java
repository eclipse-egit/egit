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
		/* @formatter:off
		 *
		 * merge tools defined in this git version, are added here...
		 *
		 * $ git --version git version 1.9.5.msysgit.1
		 *
		 * $ git mergetool --tool-help 'git mergetool --tool=<tool>' may be set
		 * to one of the following: vimdiff vimdiff2
		 *
		 * --- tool list ---
		 *
		 * copied from Msys-Git git-core/mergetools
		 *
		 * nr: 23
		 *
		 * araxis
		 * bc
		 * bc3
		 * codecompare
		 * deltawalker
		 * diffmerge
		 * diffuse
		 * ecmerge
		 * emerge
		 * gvimdiff
		 * gvimdiff2
		 * gvimdiff3
		 * kdiff3
		 * kompare
		 * meld
		 * opendiff
		 * p4merge
		 * tkdiff
		 * tortoisemerge
		 * vimdiff
		 * vimdiff2
		 * vimdiff3
		 * xxdiff
		 *
		 * name 	  		path         options
		 *
		 * @formatter:on
		 */
		// name path options

		addPreDefinedTool("araxis", "compare", //$NON-NLS-1$ //$NON-NLS-2$
				"-wait -merge -3 -a1 \"$BASE\" \"$LOCAL\" \"$REMOTE\" \"$MERGED\"", //$NON-NLS-1$
				"-wait -2 \"$LOCAL\" \"$REMOTE\" \"$MERGED\""); //$NON-NLS-1$

		String bcOptionsWithBase = "\"$LOCAL\" \"$REMOTE\" \"$BASE\" -mergeoutput=\"$MERGED\""; //$NON-NLS-1$
		String bcOptionsWithOutBase = "\"$LOCAL\" \"$REMOTE\" -mergeoutput=\"$MERGED\""; //$NON-NLS-1$
		addPreDefinedTool("bc", "bcomp", //$NON-NLS-1$ //$NON-NLS-2$
				bcOptionsWithBase, bcOptionsWithOutBase);

		addPreDefinedTool("bc3", "bcompare", //$NON-NLS-1$ //$NON-NLS-2$
				bcOptionsWithBase, bcOptionsWithOutBase);

		addPreDefinedTool("codecompare", "CodeCompare", //$NON-NLS-1$ //$NON-NLS-2$
				"-MF=\"$LOCAL\" -TF=\"$REMOTE\" -BF=\"$BASE\" -RF=\"$MERGED\"", //$NON-NLS-1$
				"-MF=\"$LOCAL\" -TF=\"$REMOTE\" -RF=\"$MERGED\""); //$NON-NLS-1$

		/*@formatter:off
		    # Adding $(pwd)/ in front of $MERGED should not be necessary.
			# However without it, DeltaWalker (at least v1.9.8 on Windows)
			# crashes with a JRE exception.  The DeltaWalker user manual,
			# shows $(pwd)/ whenever the '-merged' options is given.
			# Adding it here seems to work around the problem.
			if $base_present
			then
				"$merge_tool_path" "$LOCAL" "$REMOTE" "$BASE" -merged="$(pwd)/$MERGED"
			else
				"$merge_tool_path" "$LOCAL" "$REMOTE" -merged="$(pwd)/$MERGED"
			fi >/dev/null 2>&1
			@formatter:on
		 */
		// TODO: add support for $(pwd)
		addPreDefinedTool("deltawalker", "DeltaWalker", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$LOCAL\" \"$REMOTE\" \"$BASE\" -merged=\"$(pwd)/$MERGED\"", //$NON-NLS-1$
				"\"$LOCAL\" \"$REMOTE\" -merged=\"$(pwd)/$MERGED\""); //$NON-NLS-1$

		addPreDefinedTool("diffmerge", "diffmerge", //$NON-NLS-1$ //$NON-NLS-2$
				"--merge --result=\"$MERGED\" \"$LOCAL\" \"$BASE\" \"$REMOTE\"", //$NON-NLS-1$
				"--merge --result=\"$MERGED\" \"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$

		addPreDefinedTool("diffuse", "diffuse", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$LOCAL\" \"$MERGED\" \"$REMOTE\" \"$BASE\"", //$NON-NLS-1$
				"\"$LOCAL\" \"$MERGED\" \"$REMOTE\""); //$NON-NLS-1$

		addPreDefinedTool("ecmerge", "ecmerge", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$BASE\" \"$LOCAL\" \"$REMOTE\" --default --mode=merge3 --to=\"$MERGED\"", //$NON-NLS-1$
				"\"$LOCAL\" \"$REMOTE\" --default --mode=merge2 --to=\"$MERGED\""); //$NON-NLS-1$

		// TODO: add support for $(basename)
		addPreDefinedTool("emerge", "emacs", //$NON-NLS-1$ //$NON-NLS-2$
				"-f emerge-files-with-ancestor-command \"$LOCAL\" \"$REMOTE\" \"$BASE\" \"$(basename \"$MERGED\")\"", //$NON-NLS-1$
				"-f emerge-files-command \"$LOCAL\" \"$REMOTE\" \"$(basename \"$MERGED\")\""); //$NON-NLS-1$

		String vimdifOptionsWithBase = "-f -d -c 'wincmd J' \"$MERGED\" \"$LOCAL\" \"$BASE\" \"$REMOTE\""; //$NON-NLS-1$
		String vimdifOptionsWithOutBase = "-f -d -c 'wincmd l' \"$LOCAL\" \"$MERGED\" \"$REMOTE\""; //$NON-NLS-1$
		addPreDefinedTool("gvimdiff", "gvim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdifOptionsWithBase, vimdifOptionsWithOutBase);

		String vimdif2Options = "-f -d -c 'wincmd l' \"$LOCAL\" \"$MERGED\" \"$REMOTE\""; //$NON-NLS-1$
		addPreDefinedTool("gvimdiff2", "gvim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdif2Options);

		String vimdif3OptionsWithBase = "-f -d -c 'hid | hid | hid' \"$LOCAL\" \"$REMOTE\" \"$BASE\" \"$MERGED\""; //$NON-NLS-1$
		String vimdif3OptionsWithOutBase = "-f -d -c 'hid | hid' \"$LOCAL\" \"$REMOTE\" \"$MERGED\""; //$NON-NLS-1$
		addPreDefinedTool("gvimdiff3", "gvim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdif3OptionsWithBase, vimdif3OptionsWithOutBase);

		addPreDefinedTool("kdiff3", "kdiff3", //$NON-NLS-1$ //$NON-NLS-2$
				"--auto --L1 \"$MERGED (Base)\" --L2 \"$MERGED (Local)\" --L3 \"$MERGED (Remote)\" -o \"$MERGED\" \"$BASE\" \"$LOCAL\" \"$REMOTE\"", //$NON-NLS-1$
				"--auto --L1 \"$MERGED (Local)\" --L2 \"$MERGED (Remote)\" -o \"$MERGED\" \"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$

		/* cannot merge !
		addPreDefinedTool("kompare", "kompare", "\"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ */

		addPreDefinedTool("meld", "meld", //$NON-NLS-1$ //$NON-NLS-2$
				"--output \"$MERGED\" \"$LOCAL\" \"$BASE\" \"$REMOTE\"", //$NON-NLS-1$
				"--output \"$MERGED\" \"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$

		addPreDefinedTool("opendiff", "opendiff", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$LOCAL\" \"$REMOTE\" -ancestor \"$BASE\" -merge \"$MERGED\"", //$NON-NLS-1$
				"\"$LOCAL\" \"$REMOTE\" -merge \"$MERGED\""); //$NON-NLS-1$

		// TODO: if base is not present copy local to base
		addPreDefinedTool("p4merge", "p4merge", //$NON-NLS-1$ //$NON-NLS-2$
				"\"$BASE\" \"$REMOTE\" \"$LOCAL\" \"$MERGED\"", //$NON-NLS-1$
				"\"$LOCAL\" \"$REMOTE\" \"$LOCAL\" \"$MERGED\""); //$NON-NLS-1$

		addPreDefinedTool("tkdiff", "tkdiff", //$NON-NLS-1$ //$NON-NLS-2$
				"-a \"$BASE\" -o \"$MERGED\" \"$LOCAL\" \"$REMOTE\"", //$NON-NLS-1$
				"-o \"$MERGED\" \"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$

		// TODO: add support for tortoisemerge: "-base:\"$BASE\" -mine:\"$LOCAL\" -theirs:\"$REMOTE\" -merged:\"$MERGED\"
		// cannot use without base!!!
		addPreDefinedTool("tortoisemerge", "tortoisegitmerge", //$NON-NLS-1$ //$NON-NLS-2$
				"-base \"$BASE\" -mine \"$LOCAL\" -theirs \"$REMOTE\" -merged \"$MERGED\""); //$NON-NLS-1$

		addPreDefinedTool("vimdiff", "vim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdifOptionsWithBase, vimdifOptionsWithOutBase);

		addPreDefinedTool("vimdiff2", "vim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdif2Options);

		addPreDefinedTool("vimdiff3", "vim", //$NON-NLS-1$ //$NON-NLS-2$
				vimdif3OptionsWithBase, vimdif3OptionsWithOutBase);

		// TODO: add support for $extra
		addPreDefinedTool("xxdiff", "xxdiff", //$NON-NLS-1$ //$NON-NLS-2$
				"-X --show-merged-pane -R 'Accel.SaveAsMerged: \"Ctrl-S\"' -R 'Accel.Search: \"Ctrl+F\"' -R 'Accel.SearchForward: \"Ctrl-G\"' --merged-file \"$MERGED\" \"$LOCAL\" \"$BASE\" \"$REMOTE\"", //$NON-NLS-1$
				"-X $extra -R 'Accel.SaveAsMerged: \"Ctrl-S\"' -R 'Accel.Search: \"Ctrl+F\"' -R 'Accel.SearchForward: \"Ctrl-G\"' --merged-file \"$MERGED\" \"$LOCAL\" \"$REMOTE\""); //$NON-NLS-1$
	}

}
