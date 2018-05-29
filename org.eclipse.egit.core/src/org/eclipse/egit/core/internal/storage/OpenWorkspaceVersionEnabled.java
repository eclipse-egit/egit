/*******************************************************************************
 * Copyright (C) 2014, Konrad Kügler <swamblumat-eclipsebugs@yahoo.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.jgit.lib.Repository;

/**
 * Provides the workspace version of a file. <br>
 *
 * This interface serves as a hint to GitCompareFileRevisionEditorInput to
 * provide a "Open Workspace Version" action to the user.
 */
public interface OpenWorkspaceVersionEnabled {

	/**
	 * @return the repository containing this file
	 */
	public Repository getRepository();

	/**
	 * @return the file path relative to the repository's working directory
	 */
	public String getGitPath();

}
