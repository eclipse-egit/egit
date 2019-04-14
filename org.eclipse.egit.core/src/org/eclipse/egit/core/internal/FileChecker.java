/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;

/**
 * FileChecker is used to check a set of files. It is checked whether resources
 * exist for the files and if the resources are located in projects shared with
 * Git
 *
 */
public class FileChecker {

	/**
	 * CheckResult provides the result of a file check. It contains a
	 * CheckResultEntry for each problematic file.
	 */
	public static class CheckResult {

		private boolean isOk = true;

		private boolean containsNonWorkspaceFiles = false;

		private boolean containsNotSharedResources = false;

		private Map<String, CheckResultEntry> problems = new HashMap<>();

		/**
		 * @return true if there are no problems
		 */
		public boolean isOk() {
			return isOk;
		}

		/**
		 * @return true if at least one checked file does not exist in the
		 *         workspace
		 */
		public boolean containsNonWorkspaceFiles() {
			return containsNonWorkspaceFiles;
		}

		/**
		 * @return true if at least one checked file has the following problem:
		 *         a resource exists for the given file but all resources
		 *         existing for the given file are not shared with Git
		 */
		public boolean containsNotSharedResources() {
			return containsNotSharedResources;
		}

		/**
		 * @return map containing a problem description for each problematic
		 *         file
		 */
		public Map<String, CheckResultEntry> getProblems() {
			return problems;
		}

		void addEntry(String path, CheckResultEntry entry) {
			isOk = false;
			if (!entry.inWorkspace)
				containsNonWorkspaceFiles = true;
			if (entry.inWorkspace && !entry.shared)
				containsNotSharedResources = true;
			problems.put(path, entry);
		}

		/**
		 * @param path
		 * @return a {@link CheckResultEntry} if a problem occurred for the
		 *         given file, null otherwise.
		 */
		public CheckResultEntry getEntry(String path) {
			return problems.get(path);
		}
	}

	/**
	 * CheckResultEntry describes the problem of a single file
	 *
	 */
	public static class CheckResultEntry {
		/**
		 * True if the related object exists in the workspace
		 */
		final public boolean inWorkspace;

		/**
		 * True if the related object exists in the workspace and is shared with
		 * Git (shared => inWorkspace)
		 */
		final public boolean shared;

		/**
		 * @param inWorkspace
		 * @param shared
		 */
		public CheckResultEntry(boolean inWorkspace, boolean shared) {
			this.inWorkspace = inWorkspace;
			this.shared = shared;
		}

	}

	/**
	 * The method checks a collection of files. Problems are reported for a
	 * file if no resource exists for the file or if the related resource is
	 * located in a project not shared with Git.
	 *
	 * @param repository
	 * @param files
	 * @return a {@link CheckResult} containing result of the check
	 */
	public static CheckResult checkFiles(Repository repository,
			Collection<String> files) {
		CheckResult result = new CheckResult();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String workTreePath = repository.getWorkTree().getAbsolutePath();
		for (String filePath : files) {
			IFile[] filesForLocation = root.findFilesForLocationURI(new File(
					workTreePath, filePath).toURI());
			if (filesForLocation.length == 0) {
				result.addEntry(filePath, new CheckResultEntry(false, false));
				continue;
			}
			boolean mappedResourceFound = false;
			for (IFile file : filesForLocation) {
				if (RepositoryMapping.getMapping(file) != null) {
					mappedResourceFound = true;
					break;
				}
			}
			if (!mappedResourceFound)
				result.addEntry(filePath, new CheckResultEntry(true, false));
		}
		return result;
	}

}
