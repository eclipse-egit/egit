/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.GitDateFormatter;

/**
 * A date formatter that formats dates according to the user's preferences.
 *
 * @see org.eclipse.egit.ui.internal.preferences.DateFormatPreferencePage
 */
public class PreferenceBasedDateFormatter extends GitDateFormatter {

	private final SimpleDateFormat customFormat;

	private final GitDateFormatter.Format gitFormat;

	/**
	 * Creates a new {@link PreferenceBasedDateFormatter} that will format dates
	 * according to the date format preferences set at the time it was created.
	 *
	 * @return a new {@link PreferenceBasedDateFormatter}
	 */
	public static @NonNull PreferenceBasedDateFormatter create() {
		String choice = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.DATE_FORMAT_CHOICE);
		GitDateFormatter.Format format = null;
		try {
			format = GitDateFormatter.Format.valueOf(choice);
		} catch (IllegalArgumentException | NullPointerException e) {
			// Custom format: ignore and leave at null
		}
		return new PreferenceBasedDateFormatter(format);
	}

	private PreferenceBasedDateFormatter(GitDateFormatter.Format gitFormat) {
		super(gitFormat != null ? gitFormat : GitDateFormatter.Format.DEFAULT);
		this.gitFormat = gitFormat;
		SimpleDateFormat format = null;
		if (gitFormat == null) {
			String pattern = Activator.getDefault().getPreferenceStore()
					.getString(UIPreferences.DATE_FORMAT);
			try {
				format = new SimpleDateFormat(pattern);
			} catch (IllegalArgumentException | NullPointerException e1) {
				// Corrupted preferences?
				Activator.logError("org.eclipse.egit.ui preference " //$NON-NLS-1$
						+ UIPreferences.DATE_FORMAT + " is invalid; ignoring", //$NON-NLS-1$
						e1);
				format = new SimpleDateFormat(
						Activator.getDefault().getPreferenceStore()
								.getDefaultString(UIPreferences.DATE_FORMAT));
			}
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
		if (customFormat == null) {
			return super.formatDate(ident);
		}
		return customFormat.format(ident.getWhen());
	}

	/**
	 * Retrieves the {@link org.eclipse.jgit.util.GitDateFormatter.Format
	 * GitDateFormatter.Format} this formatter uses.
	 *
	 * @return the format, or {@code null} if a user-defined date format pattern
	 *         is used
	 */
	public @Nullable GitDateFormatter.Format getFormat() {
		return gitFormat;
	}
}
