/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Benjamin Muskalla (Tasktop Technologies) - moved into Core for reusability
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Utility class for compare-related functionality
 */
public class CompareCoreUtils {

	/**
	 * Determine the encoding used by Eclipse for the resource which belongs to
	 * repoPath in the eclipse workspace or null if no resource is found
	 *
	 * @param db
	 *            the repository
	 * @param repoPath
	 *            the path in the git repository
	 * @return the encoding used in eclipse for the resource or null if
	 *
	 */
	public static String getResourceEncoding(Repository db, String repoPath) {
		if (db.isBare())
			return null;
		IFile resource = ResourceUtil.getFileForLocation(db, repoPath);
		if (resource == null)
			return null;

		return getResourceEncoding(resource);
	}

	/**
	 * Determine the encoding used by eclipse for the resource.
	 *
	 * @param resource
	 *            must be an instance of IEncodedStorage
	 * @return the encoding used in Eclipse for the resource if found or null
	 */
	public static String getResourceEncoding(IResource resource) {
		// Get the encoding for the current version. As a matter of
		// principle one might want to use the eclipse settings for the
		// version we are retrieving as that may be defined by the
		// project settings, but there is no historic API for this.
		String charset;
		IEncodedStorage encodedStorage = ((IEncodedStorage) resource);
		try {
			charset = encodedStorage.getCharset();
			if (charset == null)
				charset = resource.getParent().getDefaultCharset();
		} catch (CoreException e) {
			charset = Constants.CHARACTER_ENCODING;
		}
		return charset;
	}

}
