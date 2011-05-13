/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.util.Date;

import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.RGB;

/**
 * Annotation revision
 */
public class AnnotationRevision extends Revision {

	private int start;

	private int lines = 1;

	private org.eclipse.jgit.blame.Revision revision;

	private Repository repository;

	public Object getHoverInfo() {
		return this;
	}

	public RGB getColor() {
		return AuthorColors.getDefault().getCommitterRGB(getAuthor());
	}

	public String getId() {
		return revision.getCommit().abbreviate(7).name();
	}

	public Date getDate() {
		return revision.getCommit().getAuthorIdent().getWhen();
	}

	/**
	 * Register revision
	 *
	 * @return this revision
	 */
	public AnnotationRevision register() {
		addRange(new LineRange(start, lines));
		return this;
	}

	/**
	 * Increment line count
	 *
	 * @return this revision
	 */
	public AnnotationRevision addLine() {
		lines++;
		return this;
	}

	/**
	 * Reset revision
	 *
	 * @param number
	 * @return this revision
	 */
	public AnnotationRevision reset(int number) {
		start = number;
		lines = 1;
		return this;
	}

	/**
	 * Set revision
	 *
	 * @param revision
	 * @return this
	 */
	public AnnotationRevision setRevision(
			org.eclipse.jgit.blame.Revision revision) {
		this.revision = revision;
		return this;
	}

	/**
	 * Get revision
	 *
	 * @return revision
	 */
	public org.eclipse.jgit.blame.Revision getRevision() {
		return this.revision;
	}

	/**
	 * Set repository
	 *
	 * @param repository
	 * @return this
	 */
	public AnnotationRevision setRepository(Repository repository) {
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
		return revision.getCommit().getAuthorIdent().getName();
	}

}