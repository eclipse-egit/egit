/*******************************************************************************
 * Copyright (C) 2018 Michael Keppler <michael.keppler@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Show working tree node in system explorer. Implementation partially taken
 * from
 * {@code org.eclipse.ui.internal.ide.handlers.ShowInSystemExplorerHandler},
 * which unfortunately requires an IResource as input.
 */
@SuppressWarnings("restriction")
public class ShowInSystemExplorerActionHandler extends RepositoryActionHandler {

	private static final String VARIABLE_RESOURCE = "${selected_resource_loc}"; //$NON-NLS-1$
	private static final String VARIABLE_RESOURCE_URI = "${selected_resource_uri}"; //$NON-NLS-1$
	private static final String VARIABLE_FOLDER = "${selected_resource_parent_loc}"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object element = getSelection().getFirstElement();
		if (!(element instanceof RepositoryTreeNode)) {
			return null;
		}
		File canonicalPath = ((RepositoryTreeNode) element).getPath().toFile();

		Job job = Job.create(
				UIText.ShowInSystemExplorerActionHandler_JobTitle,
				monitor -> {
					try {
						String launchCmd = getShowInSystemExplorerCommand(
								canonicalPath);

						if ("".equals(launchCmd)) { //$NON-NLS-1$
							return Status.CANCEL_STATUS;
						}

						File dir = canonicalPath.getParentFile();
						if (Util.isLinux() || Util.isMac()) {
							Runtime.getRuntime().exec(
									new String[] { "/bin/sh", "-c", launchCmd }, //$NON-NLS-1$ //$NON-NLS-2$
									null, dir);
						} else {
							Runtime.getRuntime().exec(launchCmd, null, dir);
						}
					} catch (IOException e) {
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				});
		job.schedule();
		return null;
	}

	/**
	 * Prepare command for launching system explorer to show a path
	 *
	 * @param path
	 *            the path to show
	 * @return the command that shows the path
	 * @throws IOException
	 */
	private String getShowInSystemExplorerCommand(File path)
			throws IOException {
		String command = IDEWorkbenchPlugin.getDefault().getPreferenceStore()
				.getString(IDEInternalPreferences.WORKBENCH_SYSTEM_EXPLORER);

		command = Util.replaceAll(command, VARIABLE_RESOURCE,
				quotePath(path.getCanonicalPath()));
		command = Util.replaceAll(command, VARIABLE_RESOURCE_URI,
				path.getCanonicalFile().toURI().toString());
		File parent = path.getParentFile();
		if (parent != null) {
			command = Util.replaceAll(command, VARIABLE_FOLDER,
					quotePath(parent.getCanonicalPath()));
		}
		return command;
	}

	private String quotePath(String path) {
		if (Util.isLinux() || Util.isMac()) {
			// Quote for usage inside "", man sh, topic QUOTING:
			path = path.replaceAll("[\"$`]", "\\\\$0"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// Windows: Can't quote, since explorer.exe has a very special command
		// line parsing strategy.
		return path;
	}

	@Override
	public boolean isEnabled() {
		return getSelection().size() == 1;
	}

}
