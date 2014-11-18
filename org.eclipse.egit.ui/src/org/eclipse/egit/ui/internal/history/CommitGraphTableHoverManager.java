/*******************************************************************************
 * Copyright (C) 2012, 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.text.AbstractHoverInformationControlManager;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/**
 * For showing tool tips when hovering over cells in the commit graph table.
 */
class CommitGraphTableHoverManager extends
		AbstractHoverInformationControlManager {

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

		TableItem item = tableViewer.getTable().getItem(new Point(e.x, e.y));
		if (item != null) {
			SWTCommit commit = (SWTCommit) item.getData();
			if (commit != null && commit.getRefCount() > 0) {
				Rectangle itemBounds = item.getBounds();
				int firstColumnWidth = tableViewer.getTable().getColumn(0)
						.getWidth();
				int relativeX = e.x - firstColumnWidth - itemBounds.x;
				for (int i = 0; i < commit.getRefCount(); i++) {
					Ref ref = commit.getRef(i);
					Point textSpan = renderer.getRefHSpan(ref);
					if ((textSpan != null)
							&& (relativeX >= textSpan.x && relativeX <= textSpan.y)) {

						String hoverText = getHoverText(ref, i, commit);
						int width = textSpan.y - textSpan.x;
						Rectangle rectangle = new Rectangle(firstColumnWidth
								+ itemBounds.x + textSpan.x, itemBounds.y,
								width, itemBounds.height);
						setInformation(hoverText, rectangle);
						return;
					}
				}
			}
		}

		// computeInformation must setInformation in all cases
		setInformation(null, null);
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
}
