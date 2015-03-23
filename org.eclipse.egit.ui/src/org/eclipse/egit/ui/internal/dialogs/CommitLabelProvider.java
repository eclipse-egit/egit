/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2011, IBM Corporation
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.jgit.util.GitDateFormatter.Format;
import org.eclipse.swt.graphics.Image;

/**
 * A Label Provider for Commits
 */
public class CommitLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	private GitDateFormatter dateFormatter;

	private boolean showEmail;

	private RevCommit lastCommit;

	private PersonIdent lastAuthor;

	private PersonIdent lastCommitter;

	private Format format = Format.LOCALELOCAL;

	/**
	 * Default constructor
	 */
	public CommitLabelProvider() {
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		final RevCommit c = (RevCommit) element;
		if (columnIndex == 0)
			return c.getId().abbreviate(7).name();
		if (columnIndex == 1)
			return c.getShortMessage();
		if (columnIndex == 2 || columnIndex == 3) {
			final PersonIdent author = authorOf(c);
			if (author != null)
				switch (columnIndex) {
				case 2:
					if (showEmail)
						return author.getName()
								+ " <" + author.getEmailAddress() + '>'; //$NON-NLS-1$
					else
						return author.getName();
				case 3:
					return getDateFormatter().formatDate(author);
				}
		}
		if (columnIndex == 4 || columnIndex == 5) {
			final PersonIdent committer = committerOf(c);
			if (committer != null)
				switch (columnIndex) {
				case 4:
					if (showEmail)
						return committer.getName()
								+ " <" + committer.getEmailAddress() + '>'; //$NON-NLS-1$
					else
						return committer.getName();
				case 5:
					return getDateFormatter().formatDate(committer);
				}
		}

		return ""; //$NON-NLS-1$
	}

	private GitDateFormatter getDateFormatter() {
		if (dateFormatter == null)
			dateFormatter = new GitDateFormatter(format);
		return dateFormatter;
	}

	private PersonIdent authorOf(final RevCommit c) {
		if (lastCommit != c) {
			lastCommit = c;
			lastAuthor = c.getAuthorIdent();
			lastCommitter = c.getCommitterIdent();
		}
		return lastAuthor;
	}

	private PersonIdent committerOf(final RevCommit c) {
		if (lastCommit != c) {
			lastCommit = c;
			lastAuthor = c.getAuthorIdent();
			lastCommitter = c.getCommitterIdent();
		}
		return lastCommitter;
	}

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}

	/**
	 * @param relative {@code true} if the date column should show relative dates
	 */
	public void setRelativeDate(boolean relative) {
		dateFormatter = null;
		if (relative)
			format = Format.RELATIVE;
		else
			format = Format.LOCALELOCAL;
	}

	/**
	 * @param showEmail true to show e-mail addresses, false otherwise
	 */
	public void setShowEmailAddresses(boolean showEmail) {
		this.showEmail = showEmail;
	}
}
