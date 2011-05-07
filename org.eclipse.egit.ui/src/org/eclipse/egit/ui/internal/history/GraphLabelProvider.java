/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.RelativeDateFormatter;
import org.eclipse.swt.graphics.Image;

class GraphLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	private final DateFormat absoluteFormatter;

	private RevCommit lastCommit;

	private PersonIdent lastAuthor;

	private PersonIdent lastCommitter;

	private boolean relativeDate;

	GraphLabelProvider() {
		absoluteFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
	}

	public String getColumnText(final Object element, final int columnIndex) {
		final RevCommit c = (RevCommit) element;
		if (columnIndex == 0)
			return c.getShortMessage();
		if (columnIndex == 3)
			return c.getId().abbreviate(8).name() + "..."; //$NON-NLS-1$
		if (columnIndex == 1 || columnIndex == 2) {
			final PersonIdent author = authorOf(c);
			if (author != null) {
				switch (columnIndex) {
				case 1:
					return author.getName()
							+ " <" + author.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				case 2:
					if (relativeDate)
						return RelativeDateFormatter.format(author.getWhen());
					else
						return absoluteFormatter.format(author.getWhen());
				}
			}
		}
		if (columnIndex == 4) {
			final PersonIdent committer = committerOf(c);
			if (committer != null) {
				return committer.getName()
						+ " <" + committer.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return ""; //$NON-NLS-1$
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

	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}

	/**
	 * @param relative {@code true} if the date column should show relative dates
	 * @return {@code true} if the value was changed in this call
	 */
	public boolean setRelativeDate(boolean relative) {
		if (relative == relativeDate)
			return false;
		relativeDate = relative;
		return true;
	}
}
