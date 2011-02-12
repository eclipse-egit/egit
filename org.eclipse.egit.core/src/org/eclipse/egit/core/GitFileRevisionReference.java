/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ilya Ivanov (Intland) - implementation
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Public class that contains information about particular file revision.
 * Contains repository related path, {@link Repository} and {@link RevCommit}
 * Used for object contributions.
 *
 * <a href="mailto:ilya.ivanov@intland.com">Ilya Ivanov</a>
 */
public class GitFileRevisionReference {

	private Repository repository;
	private RevCommit revCommit;
	private String path;

	/**
	 * Creates new {@link GitFileRevisionReference}
	 * @param r
	 * @param revCommit
	 * @param path
	 */
	public GitFileRevisionReference(Repository r, RevCommit revCommit, String path) {
		this.repository = r;
		this.revCommit = revCommit;
		this.path = path;
	}

	/**
	 * @return Repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return RevCommit
	 */
	public RevCommit getRevCommit() {
		return revCommit;
	}

	/**
	 * @return repository related path
	 */
	public String getPath() {
		return path;
	}
}
