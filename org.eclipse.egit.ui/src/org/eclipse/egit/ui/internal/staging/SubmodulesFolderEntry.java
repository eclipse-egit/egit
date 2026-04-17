/*******************************************************************************
 * Copyright (C) 2026 EGit contributors and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.UIText;

/**
 * A synthetic top-level group node in the Git Staging View that collects all
 * submodule entries of a section (staged or unstaged) in a contiguous block at
 * the very top. Its children are regular {@link StagingEntry} objects marked as
 * submodules; their full repo-relative path is used as their label (the normal
 * tree grouping by project/folder is intentionally bypassed for submodules so
 * that path context such as {@code libs/foo} or {@code third_party/baz/qux}
 * remains visible).
 * <p>
 * This node carries no real container in the workspace. It exists purely for
 * visual grouping and ordering; action handlers that already understand
 * {@link StagingFolderEntry} (stage, unstage, drag &amp; drop, copy paths,
 * etc.) descend into its {@link StagingEntry} children unchanged.
 */
public class SubmodulesFolderEntry extends StagingFolderEntry {

	/**
	 * Sentinel repo-relative path used to distinguish the synthetic
	 * "Submodules" node from any real folder. The leading NUL character is not
	 * a legal path segment on disk, so this cannot clash with a real folder
	 * entry.
	 */
	private static final IPath SENTINEL_PATH = new Path("\u0000submodules"); //$NON-NLS-1$

	/**
	 * @param repoLocation
	 *            absolute path to the working tree of the parent repository
	 */
	public SubmodulesFolderEntry(IPath repoLocation) {
		super(repoLocation, SENTINEL_PATH,
				UIText.StagingView_SubmodulesNodeLabel);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SubmodulesFolderEntry)) {
			return false;
		}
		return getLocation().equals(((SubmodulesFolderEntry) obj).getLocation());
	}

	@Override
	public int hashCode() {
		return getLocation().hashCode() ^ 0x5b_3b_0d_e5;
	}

	@Override
	public String toString() {
		return "SubmodulesFolderEntry[" + getLocation() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
