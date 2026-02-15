/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;

/**
 * Adapter factory for {@link PullRequestChangedFile} to enable integration with
 * Eclipse platform features like property pages, "Show In" menus, and context
 * menu contributions.
 * <p>
 * This adapter attempts to resolve the changed file to a workspace
 * {@link IResource}. If the file exists in the workspace (e.g., the PR branch
 * is checked out locally), the adapter returns the corresponding
 * {@link IFile}. Otherwise, it returns {@code null} for resource-based
 * adaptations.
 * </p>
 */
public class PullRequestChangedFileAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject == null
				|| !(adaptableObject instanceof PullRequestChangedFile)) {
			return null;
		}

		PullRequestChangedFile entry = (PullRequestChangedFile) adaptableObject;

		if (adapterType == IResource.class || adapterType == IFile.class) {
			IFile file = entry.getWorkspaceFile();
			if (file != null && file.isAccessible()) {
				return (T) file;
			}
			return null;
		}

		if (adapterType == IPath.class) {
			// Return the location if available, otherwise the repo-relative path
			IPath location = entry.getLocation();
			if (location != null) {
				return (T) location;
			}
			// Fall back to repo-relative path
			return (T) entry.getRepoRelativePath();
		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IResource.class, IFile.class, IPath.class };
	}
}
