/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Utility class
 *
 */
public class Utils {

	/**
	 * @param repository
	 * @return display name of the repository
	 */
	public static String getRepositoryName(Repository repository) {
		String repositoryName;
		File gitDir = repository.getDirectory();
		if (gitDir != null)
			repositoryName = repository.getDirectory().getParentFile()
					.getName();
		else
			repositoryName = ""; //$NON-NLS-1$
		return repositoryName;
	}

	/**
	 * @param id
	 * @return a shortened ObjectId (first 6 digits)
	 */
	public static String getShortObjectId(ObjectId id) {
		return id.getName().substring(0, 6);
	}

}
