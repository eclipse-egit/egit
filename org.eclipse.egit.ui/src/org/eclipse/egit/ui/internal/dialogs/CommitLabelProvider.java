/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2011, IBM Corporation
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.swt.graphics.Image;

/**
 * A Label provider for commits.
 */
public class CommitLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	private GitDateFormatter dateFormatter;

	private boolean showEmail;

	private RevCommit lastCommit;

	private PersonIdent lastAuthor;

	private PersonIdent lastCommitter;

	private IPropertyChangeListener uiPrefsListener;

	private final IPreferenceStore store;

	/**
	 * Default constructor
	 */
	public CommitLabelProvider() {
		this(true);
	}

	/**
	 * Constructs a {@link CommitLabelProvider} that optionally does not react
	 * to the preference for showing e-mails.
	 *
	 * @param canShowEmailAddresses
	 *            whether this label provider shall show E-Mail addresses if the
	 *            corresponding preference is set
	 */
	public CommitLabelProvider(final boolean canShowEmailAddresses) {
		super();
		store = Activator.getDefault().getPreferenceStore();
		uiPrefsListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if ((UIPreferences.DATE_FORMAT.equals(property)
						|| UIPreferences.DATE_FORMAT_CHOICE.equals(property))
						&& (dateFormatter instanceof PreferenceBasedDateFormatter)) {
					setDateFormatter(PreferenceBasedDateFormatter.create());
				} else if (UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE
						.equals(property)) {
					setRelativeDate(store.getBoolean(
							UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE));
				} else if (canShowEmailAddresses
						&& UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES
								.equals(property)) {
					setShowEmailAddresses(store.getBoolean(
							UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES));
				}

			}
		};
		if (store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE)) {
			dateFormatter = new GitDateFormatter(
					GitDateFormatter.Format.RELATIVE);
		}
		showEmail = canShowEmailAddresses && store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES);
		store.addPropertyChangeListener(uiPrefsListener);
	}

	@Override
	public void dispose() {
		store.removePropertyChangeListener(uiPrefsListener);
		super.dispose();
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
		if (dateFormatter == null) {
			dateFormatter = PreferenceBasedDateFormatter.create();
		}
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
	private void setRelativeDate(boolean relative) {
		if (dateFormatter instanceof PreferenceBasedDateFormatter) {
			if (relative) {
				setDateFormatter(
						new GitDateFormatter(GitDateFormatter.Format.RELATIVE));
			}
		} else if (!relative) {
			setDateFormatter(PreferenceBasedDateFormatter.create());
		}
	}

	/**
	 * @param showEmail true to show e-mail addresses, false otherwise
	 */
	private void setShowEmailAddresses(boolean showEmail) {
		if (showEmail != this.showEmail) {
			this.showEmail = showEmail;
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}
	}

	private void setDateFormatter(GitDateFormatter formatter) {
		dateFormatter = formatter;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}
}
