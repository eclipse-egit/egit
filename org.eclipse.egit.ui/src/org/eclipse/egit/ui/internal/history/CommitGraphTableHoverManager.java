/*******************************************************************************
 * Copyright (C) 2012, 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.text.AbstractHoverInformationControlManager;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.jgit.util.GitDateFormatter.Format;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

/**
 * For showing tool tips when hovering over cells in the commit graph table.
 */
class CommitGraphTableHoverManager extends
		AbstractHoverInformationControlManager {

	private final GitDateFormatter dateFormatter = new GitDateFormatter(
			Format.ISO);

	private final TableViewer tableViewer;

	private final SWTPlotRenderer renderer;

	CommitGraphTableHoverManager(TableViewer tableViewer,
			SWTPlotRenderer renderer) {
		super(new RefHoverInformationControlCreator());
		this.tableViewer = tableViewer;
		this.renderer = renderer;
	}

	@Override
	protected void computeInformation() {
		MouseEvent e = getHoverEvent();

		Information information = null;
		ViewerCell cell = tableViewer.getCell(new Point(e.x, e.y));
		if (cell != null) {
			SWTCommit commit = (SWTCommit) cell.getElement();
			if (commit != null)
				information = computeInformationForCommit(commit, cell, e);
		}

		// computeInformation must setInformation in all cases
		if (information != null)
			setInformation(information.information, information.subjectArea);
		else
			setInformation(null, null);
	}

	private Information computeInformationForCommit(SWTCommit commit,
			ViewerCell cell, MouseEvent e) {
		final int columnIndex = cell.getColumnIndex();
		switch (columnIndex) {
		case 1:
			return computeInformationForRef(commit, cell, e);
		case 2:
			return computeInformationForName(commit.getAuthorIdent(), cell);
		case 3:
			return computeInformationForDate(commit.getAuthorIdent(), cell);
		case 4:
			return computeInformationForName(commit.getCommitterIdent(), cell);
		case 5:
			return computeInformationForDate(commit.getCommitterIdent(), cell);
		}
		return null;
	}

	private Information computeInformationForRef(SWTCommit commit,
			ViewerCell cell, MouseEvent e) {
		if (commit.getRefCount() == 0)
			return null;
		Rectangle itemBounds = cell.getBounds();
		int relativeX = e.x - itemBounds.x;
		for (int i = 0; i < commit.getRefCount(); i++) {
			Ref ref = commit.getRef(i);
			Point textSpan = renderer.getRefHSpan(ref);
			if ((textSpan != null)
					&& (relativeX >= textSpan.x && relativeX <= textSpan.y)) {

				String hoverText = getHoverText(ref, i, commit);
				int x = itemBounds.x + textSpan.x;
				int width = textSpan.y - textSpan.x;
				Rectangle rectangle = new Rectangle(x, itemBounds.y, width,
						itemBounds.height);
				return new Information(hoverText, rectangle);
			}
		}
		return null;
	}

	private Information computeInformationForName(PersonIdent ident,
			ViewerCell cell) {
		String nameWithEmail = ident.getName()
				+ " <" + ident.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		return new Information(nameWithEmail, cell.getBounds());
	}

	private Information computeInformationForDate(PersonIdent ident,
			ViewerCell cell) {
		String formattedDate = dateFormatter.formatDate(ident);
		return new Information(formattedDate, cell.getBounds());
	}

	private String getHoverText(Ref ref, int refIndex, SWTCommit commit) {
		if (ref.getName().startsWith(Constants.R_TAGS)
				&& renderer.isShownAsEllipsis(ref)) {
			StringBuilder sb = new StringBuilder(
					UIText.CommitGraphTable_HoverAdditionalTags);
			for (int i = refIndex; i < commit.getRefCount(); i++) {
				Ref tag = commit.getRef(i);
				String name = tag.getName();
				if (name.startsWith(Constants.R_TAGS)) {
					sb.append('\n');
					sb.append(name.substring(Constants.R_TAGS.length()));
				}
			}
			return sb.toString();
		} else {
			return getHoverTextForSingleRef(ref);
		}
	}

	private String getHoverTextForSingleRef(Ref r) {
		StringBuilder sb = new StringBuilder();
		String name = r.getName();
		sb.append(name);
		if (r.isSymbolic()) {
			sb.append(": "); //$NON-NLS-1$
			sb.append(r.getLeaf().getName());
		}
		String description = GitLabels.getRefDescription(r);
		if (description != null) {
			sb.append("\n"); //$NON-NLS-1$
			sb.append(description);
		}
		return sb.toString();
	}

	private static final class RefHoverInformationControlCreator extends
			AbstractReusableInformationControlCreator {
		@Override
		protected IInformationControl doCreateInformationControl(Shell parent) {
			return new DefaultInformationControl(parent);
		}
	}

	private static class Information {
		final Object information;
		final Rectangle subjectArea;

		private Information(Object information, Rectangle subjectArea) {
			this.information = information;
			this.subjectArea = subjectArea;
		}
	}
}
