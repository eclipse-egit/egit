/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.util.Date;

import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.RGB;

/**
 * Annotation revision
 */
public class BlameRevision extends Revision {

	private int start;

	private int lines = 1;

	private RevCommit commit;

	private Repository repository;

	public Object getHoverInfo() {
		return this;
	}

	public RGB getColor() {
		return AuthorColors.getDefault().getCommitterRGB(getAuthor());
	}

	public String getId() {
		return commit.abbreviate(7).name();
	}

	public Date getDate() {
		return commit.getAuthorIdent().getWhen();
	}

	/**
	 * Register revision
	 *
	 * @return this revision
	 */
	public BlameRevision register() {
		addRange(new LineRange(start, lines));
		return this;
	}

	/**
	 * Increment line count
	 *
	 * @return this revision
	 */
	public BlameRevision addLine() {
		lines++;
		return this;
	}

	/**
	 * Reset revision
	 *
	 * @param number
	 * @return this revision
	 */
	public BlameRevision reset(int number) {
		start = number;
		lines = 1;
		return this;
	}

	/**
	 * Set revision
	 *
	 * @param commit
	 * @return this
	 */
	public BlameRevision setCommit(RevCommit commit) {
		this.commit = commit;
		return this;
	}

	/**
	 * Get revision
	 *
	 * @return revision
	 */
	public RevCommit getCommit() {
		return this.commit;
	}

	/**
	 * Set repository
	 *
	 * @param repository
	 * @return this
	 */
	public BlameRevision setRepository(Repository repository) {
		this.repository = repository;
		return this;
	}

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	public Repository getRepository() {
		return this.repository;
	}

	public String getAuthor() {
		return commit.getAuthorIdent().getName();
	}

}