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

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Commit search results label provider class.
 */
public class CommitResultLabelProvider extends WorkbenchLabelProvider {

	private DateFormat dateFormat = DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM, DateFormat.SHORT);

	private int layout;

	/**
	 * Create commit result label provider
	 *
	 * @param layout
	 */
	public CommitResultLabelProvider(int layout) {
		this.layout = layout;
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

			if (layout == AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT)
				styled.append(MessageFormat.format(
						UIText.CommitResultLabelProvider_SectionRepository,
						commit.getRepositoryName()),
						StyledString.DECORATIONS_STYLER);
		} else if (element instanceof RepositoryMatch) {
			RepositoryMatch repository = (RepositoryMatch) element;
			styled.append(repository.getLabel(repository));
			styled.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			styled.append(repository.getRepository().getDirectory()
					.getAbsolutePath(), StyledString.QUALIFIER_STYLER);
			styled.append(MessageFormat.format(" ({0})", //$NON-NLS-1$
					Integer.valueOf(repository.getMatchCount())),
					StyledString.COUNTER_STYLER);
		}
		return styled;
	}
}
