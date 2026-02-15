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
package org.eclipse.egit.core.internal.bitbucket;

import java.util.List;

/**
 * Domain model representing a changed file in a Bitbucket pull request
 */
public class ChangedFile {

	private Path path;

	private Path srcPath;

	private String type;

	private boolean executable;

	/**
	 * @return the path information
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @param path the path information
	 */
	public void setPath(Path path) {
		this.path = path;
	}

	/**
	 * @return the source path for moves/renames, null otherwise
	 */
	public Path getSrcPath() {
		return srcPath;
	}

	/**
	 * @param srcPath the source path
	 */
	public void setSrcPath(Path srcPath) {
		this.srcPath = srcPath;
	}

	/**
	 * @return the change type (ADD, DELETE, MODIFY, MOVE, COPY)
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the change type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return whether the file is executable
	 */
	public boolean isExecutable() {
		return executable;
	}

	/**
	 * @param executable whether the file is executable
	 */
	public void setExecutable(boolean executable) {
		this.executable = executable;
	}

	/**
	 * Nested class representing path information
	 */
	public static class Path {

		private String toString;

		private List<String> components;

		private String name;

		private String extension;

		/**
		 * @return the full path as string
		 */
		public String getToString() {
			return toString;
		}

		/**
		 * @param toString the full path string
		 */
		public void setToString(String toString) {
			this.toString = toString;
		}

		/**
		 * @return the path components
		 */
		public List<String> getComponents() {
			return components;
		}

		/**
		 * @param components the path components
		 */
		public void setComponents(List<String> components) {
			this.components = components;
		}

		/**
		 * @return the file name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the file name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the file extension
		 */
		public String getExtension() {
			return extension;
		}

		/**
		 * @param extension the file extension
		 */
		public void setExtension(String extension) {
			this.extension = extension;
		}
	}
}
