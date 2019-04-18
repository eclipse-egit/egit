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
import org.eclipse.jgit.lib.Repository;

class NoopDebugUIPluginFacade implements IDebugUIPluginFacade {
	@Override
	public String getRunningLaunchConfigurationName(
			Collection<Repository> repositories, IProgressMonitor monitor) {
		return null;
	}
}
