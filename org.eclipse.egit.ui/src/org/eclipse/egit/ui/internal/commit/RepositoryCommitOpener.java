/*******************************************************************************
 * Copyright (c) 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;

/**
 * An {@link OpenAndLinkWithEditorHelper} that supports opening
 * {@link RepositoryCommit}s.
 */
public class RepositoryCommitOpener
		extends OpenAndLinkWithEditorHelper {

	/**
	 * Installs a {@link RepositoryCommitOpener} on the given
	 * viewer.
	 *
	 * @param viewer
	 *            {@link StructuredViewer} to set up
	 */
	@SuppressWarnings("unused")
	public static void setup(StructuredViewer viewer) {
		new RepositoryCommitOpener(viewer);
	}

	private RepositoryCommitOpener(StructuredViewer viewer) {
		super(viewer);
	}

	@Override
	protected void open(ISelection selection, boolean activate) {
		handleOpen(selection, activate);
	}

	@Override
	protected void activate(ISelection selection) {
		handleOpen(selection, true);
	}

	private void handleOpen(ISelection selection, boolean activateOnOpen) {
		if (selection instanceof IStructuredSelection) {
			for (Object element : (IStructuredSelection) selection) {
				if (element instanceof RepositoryCommit) {
					CommitEditor.openQuiet((RepositoryCommit) element,
							activateOnOpen);
				}
			}
		}
	}
}
