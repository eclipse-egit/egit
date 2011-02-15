/*******************************************************************************
 * Copyright (c) 2011 Ilya Ivanov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ilya Ivanov <ilya.ivanov@intland.com>
 *******************************************************************************/

package org.eclipse.egit.mylyn.ui;

import org.eclipse.core.runtime.IPath;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;


/**
 * Interface is intended to be implemented by clients in order
 * to provide specific repository resolving mechanisms
 */
public interface ITaskRepositoryResolver {

	/**
	 * @param repositoryRootPath
	 * @param commitMessage
	 * @param commitSHA
	 * @return AbstractTaskReference or <code>null</code> if nothing found
	 */
	public AbstractTaskReference createTaskRerference(
			IPath repositoryRootPath, String commitMessage, String commitSHA);

}
