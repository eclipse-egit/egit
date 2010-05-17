/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jgit.lib.Repository;

/**
 * A node that can be used for displaying a listing of a repository's staged,
 * modified, and untracked files.
 */
abstract class StatusNode {

	private Set<IFile> files;

	private Repository repository;

	StatusNode(Repository repository) {
		this.repository = repository;
	}

	void setFiles(Set<IFile> files) {
		this.files = files;
	}

	Set<IFile> getFiles() {
		return files;
	}

	abstract String getLabel();

	Repository getRepository() {
		return repository;
	}

	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			return repository == ((StatusNode) other).repository;
		}
		return false;
	}

	public int hashCode() {
		return 31 * getClass().getName().hashCode() * repository.hashCode();
	}

}
