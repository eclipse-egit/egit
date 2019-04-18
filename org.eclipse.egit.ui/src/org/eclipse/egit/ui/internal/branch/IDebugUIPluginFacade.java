/*******************************************************************************
 * Copyright (C) 2019, Peter Severin <peter@wireframesketcher.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *    Peter Severin <peter@wireframesketcher.com> - Bug 546329
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;

/**
 * Interface that hides the optional dependency on org.eclipse.debug.ui plugin
 */
interface IDebugUIPluginFacade {
	/**
	 * If there is a running launch covering at least one project from the given
	 * repositories, return the name of the first such launch configuration.
	 *
	 * @param repositories
	 *            to determine projects to be checked whether they are used in
	 *            running launches
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return the name of the launch configuration, or {@code null} if none
	 *         found.
	 */
	@Nullable
	public String getRunningLaunchConfigurationName(
			final Collection<Repository> repositories,
			IProgressMonitor monitor);
}
