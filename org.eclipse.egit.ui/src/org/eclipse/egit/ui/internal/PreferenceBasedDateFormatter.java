/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.GitDateFormatter;

/**
 * A date formatter that formats dates according to the user's preferences.
 *
 * @see org.eclipse.egit.ui.internal.preferences.DateFormatPreferencePage
 */
public class PreferenceBasedDateFormatter extends GitDateFormatter {

	private final GitDateFormatter gitFormat;

	private final SimpleDateFormat customFormat;

	/**
	 * Creates a new {@link PreferenceBasedDateFormatter} that will format dates
	 * according to the date format preferences set at the time this instance
	 * was created.
	 */
	public PreferenceBasedDateFormatter() {
		super(GitDateFormatter.Format.DEFAULT);
		String choice = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.DATE_FORMAT_CHOICE);
		GitDateFormatter git = null;
		try {
			git = new GitDateFormatter(GitDateFormatter.Format.valueOf(choice));
		} catch (IllegalArgumentException | NullPointerException e) {
			// Custom format: ignore and leave at null
		}
		this.gitFormat = git;
		String pattern = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.DATE_FORMAT);
		SimpleDateFormat format;
		try {
			format = new SimpleDateFormat(pattern);
		} catch (IllegalArgumentException | NullPointerException e1) {
			// Corrupted preferences?
			Activator.logError("org.eclipse.egit.ui preference " //$NON-NLS-1$
					+ UIPreferences.DATE_FORMAT + " is invalid; ignoring", e1); //$NON-NLS-1$
			format = new SimpleDateFormat(
					Activator.getDefault().getPreferenceStore()
							.getDefaultString(UIPreferences.DATE_FORMAT));
		}
		this.customFormat = format;
	}

	/**
	 * Formats a {@link Date} using the default {@link TimeZone} according to
	 * the date format preferences set when this instance was created.
	 *
	 * @param date
	 *            to format
	 * @return the string representation of the date, or the empty string if the
	 *         date is {@code null}.
	 */
	public String formatDate(Date date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		TimeZone timeZone = TimeZone.getDefault();
		Assert.isNotNull(timeZone);
		return formatDate(date, timeZone);
	}

	/**
	 * Formats a {@link Date} using the given {@link TimeZone} according to the
	 * date format preferences set when this instance was created.
	 *
	 * @param date
	 *            to format
	 * @param timeZone
	 *            to use
	 * @return the string representation of the date
	 */
	public String formatDate(@NonNull Date date, @NonNull TimeZone timeZone) {
		return formatDate(new PersonIdent("", "", date, timeZone)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Formats the date of a {@link PersonIdent} according to the date format
	 * preferences set when this instance was created.
	 *
	 * @param ident
	 *            to format the date of
	 * @return the string representation of the date, or the empty string if
	 *         {@code ident} is {@code null}.
	 */
	@Override
	public String formatDate(PersonIdent ident) {
		if (ident == null) {
			return ""; //$NON-NLS-1$
		}
		if (gitFormat != null) {
			return gitFormat.formatDate(ident);
		}
		return customFormat.format(ident.getWhen());
	}

}
