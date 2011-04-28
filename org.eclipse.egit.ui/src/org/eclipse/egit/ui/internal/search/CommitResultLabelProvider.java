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
package org.eclipse.egit.ui.internal.search;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.Image;

/**
 * Commit search results label provider class.
 */
public class CommitResultLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private DateFormat dateFormat = DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM, DateFormat.SHORT);

	private Image commitImage = UIIcons.CHANGESET.createImage();

	/**
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	public void dispose() {
		this.commitImage.dispose();
		super.dispose();
	}

	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		return this.commitImage;
	}

	/**
	 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
	 */
	public StyledString getStyledText(Object element) {
		StyledString styled = new StyledString();
		if (element instanceof RepositoryCommit) {
			RepositoryCommit commit = (RepositoryCommit) element;
			RevCommit revCommit = commit.getRevCommit();

			styled.append(MessageFormat.format(
					UIText.CommitResultLabelProvider_SectionMessage,
					commit.abbreviate(), revCommit.getShortMessage()));

			PersonIdent author = revCommit.getAuthorIdent();
			if (author != null)
				styled.append(MessageFormat.format(
						UIText.CommitResultLabelProvider_SectionAuthor,
						author.getName(), dateFormat.format(author.getWhen())),
						StyledString.QUALIFIER_STYLER);

			styled.append(MessageFormat.format(
					UIText.CommitResultLabelProvider_SectionRepository,
					commit.getRepositoryName()),
					StyledString.DECORATIONS_STYLER);
		}
		return styled;
	}
}
