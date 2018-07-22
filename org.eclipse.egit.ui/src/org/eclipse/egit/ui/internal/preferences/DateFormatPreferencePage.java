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
package org.eclipse.egit.ui.internal.preferences;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences concerning date formats in EGit. */
public class DateFormatPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private static final PersonIdent SAMPLE = new PersonIdent("", "", //$NON-NLS-1$ //$NON-NLS-2$
			new Date(System.currentTimeMillis() - 24 * 3600 * 1000),
			getDifferentTimeZone());

	private static final Map<GitDateFormatter.Format, FormatInfo> DATA = initializeData();

	private ComboFieldEditor formatChooser;

	private StringFieldEditor dateFormat;

	private Label dateFormatPreview;

	private Label formatExplanation;

	private String lastCustomValue;

	/** Creates a new instance. */
	public DateFormatPreferencePage() {
		super(GRID);
		initializeData();
		setTitle(UIText.DateFormatPreferencePage_title);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		String[][] values = new String[DATA.size()][2];
		int i = 0;
		for (Map.Entry<GitDateFormatter.Format, FormatInfo> entry : DATA
				.entrySet()) {
			values[i][0] = entry.getValue().name;
			values[i][1] = entry.getKey() == null
					? UIPreferences.DATE_FORMAT_CUSTOM : entry.getKey().name();
			i++;
		}
		final Composite pane = getFieldEditorParent();
		formatChooser = new ComboFieldEditor(
				UIPreferences.DATE_FORMAT_CHOICE,
				UIText.DateFormatPreferencePage_formatChooser_label,
				values,
				pane);
		addField(formatChooser);
		dateFormat = new StringFieldEditor(
				UIPreferences.DATE_FORMAT,
				UIText.DateFormatPreferencePage_formatInput_label,
				StringFieldEditor.UNLIMITED,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, pane) {
			@Override
			protected boolean doCheckState() {
				// Validate the contents. If we're disabled, we're showing some
				// built-in format string, which we always consider as valid.
				if (!getTextControl(pane).isEnabled()) {
					return true;
				}
				try {
					updatePreview(
							new SimpleDateFormat(getStringValue().trim()));
					return true;
				} catch (IllegalArgumentException e) {
					dateFormatPreview.setText(""); //$NON-NLS-1$
					return false;
				}
			}

			@Override
			protected void doLoad() {
				// Set explicitly below
			}

			@Override
			protected void doStore() {
				// Never store invalid values, or built-in values
				if (getTextControl(pane).isEnabled() && doCheckState()) {
					super.doStore();
				}
			}

			@Override
			public void setStringValue(String value) {
				super.setStringValue(value);
				refreshValidState();
			}
		};
		dateFormat.setEmptyStringAllowed(false);
		dateFormat.setErrorMessage(
				UIText.DateFormatPreferencePage_invalidDateFormat_message);
		addField(dateFormat);
		// We know that the layout will have two columns
		Label dpLabel = SWTUtils.createLabel(pane,
				UIText.DateFormatPreferencePage_datePreview_label);
		dpLabel.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT,
				false, false));
		dateFormatPreview = SWTUtils.createLabel(pane, null, 1);
		Label dummyLabel = SWTUtils.createLabel(pane, ""); //$NON-NLS-1$
		dummyLabel.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT,
				SWT.DEFAULT, false, false));
		formatExplanation = new Label(pane, SWT.LEFT | SWT.WRAP);
		GridData layout = SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT,
				true, false);
		layout.widthHint = 150; // For wrapping
		formatExplanation.setLayoutData(layout);
		// Setup based on initial values. We don't get any events by the editors
		// on initial load!
		lastCustomValue = getPreferenceStore()
				.getString(UIPreferences.DATE_FORMAT);
		String initialValue = getPreferenceStore()
				.getString(UIPreferences.DATE_FORMAT_CHOICE);
		updateFields(initialValue);
	}

	@Override
	protected void initialize() {
		super.initialize();
		// When the chooser's selection changes, update the dateFormat &
		// enablement
		formatChooser.setPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (FieldEditor.VALUE.equals(event.getProperty())) {
					GitDateFormatter.Format format = fromString(
							(String) event.getOldValue());
					if (format == null) {
						lastCustomValue = dateFormat.getStringValue();
					}
					updateFields((String) event.getNewValue());
				}
			}
		});
	}

	private void updateFields(String newSelection) {
		GitDateFormatter.Format format = fromString(newSelection);
		FormatInfo info = DATA.get(format);
		formatExplanation.setText(info.explanation);
		if (format == null) {
			dateFormat.getTextControl(getFieldEditorParent()).setEnabled(true);
			dateFormat.setStringValue(lastCustomValue);
		} else {
			dateFormat.getTextControl(getFieldEditorParent()).setEnabled(false);
			dateFormat.setStringValue(info.format);
			updatePreview(format);
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		// We don't get property changed events when the default values are
		// restored...
		lastCustomValue = getPreferenceStore()
				.getDefaultString(UIPreferences.DATE_FORMAT);
		updateFields(getPreferenceStore()
				.getDefaultString(UIPreferences.DATE_FORMAT_CHOICE));
	}

	private GitDateFormatter.Format fromString(String value) {
		try {
			return GitDateFormatter.Format.valueOf(value);
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}

	private void updatePreview(SimpleDateFormat format) {
		dateFormatPreview.setText(format.format(SAMPLE.getWhen()));
	}

	private void updatePreview(GitDateFormatter.Format format) {
		dateFormatPreview
				.setText(new GitDateFormatter(format).formatDate(SAMPLE));
	}

	private static Map<GitDateFormatter.Format, FormatInfo> initializeData() {
		Map<GitDateFormatter.Format, FormatInfo> d = new LinkedHashMap<>();
		d.put(GitDateFormatter.Format.DEFAULT,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitDefault_label,
						"EEE MMM dd HH:mm:ss yyyy Z", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpGitDefault_label));
		d.put(GitDateFormatter.Format.LOCAL,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitLocal_label,
						"EEE MMM dd HH:mm:ss yyyy", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpGitLocal_label));
		d.put(GitDateFormatter.Format.RELATIVE,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitRelative_label,
						UIText.DateFormatPreferencePage_gitRelative_format_text,
						UIText.DateFormatPreferencePage_helpGitRelative_label));
		d.put(GitDateFormatter.Format.ISO,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitIso_label,
						"yyyy-MM-dd HH:mm:ss Z", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpGitIso_label));
		d.put(GitDateFormatter.Format.RFC,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitRfc_label,
						"EEE, dd MMM yyyy HH:mm:ss Z", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpGitRfc_label));
		d.put(GitDateFormatter.Format.SHORT,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitShort_label,
						"yyyy-MM-dd", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpGitShort_label));
		DateFormat systemFormat = DateFormat
				.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
		String localeLocalFormat = (systemFormat instanceof SimpleDateFormat)
				? ((SimpleDateFormat) systemFormat).toPattern()
				: UIText.DateFormatPreferencePage_gitLocaleLocal_format_text;
		String localeFormat = (systemFormat instanceof SimpleDateFormat)
				? localeLocalFormat + " Z" //$NON-NLS-1$
				: UIText.DateFormatPreferencePage_gitLocale_format_text;
		d.put(GitDateFormatter.Format.LOCALE,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceGitLocale_label,
						localeFormat,
						UIText.DateFormatPreferencePage_helpGitLocale_label));
		d.put(GitDateFormatter.Format.LOCALELOCAL, new FormatInfo(
				UIText.DateFormatPreferencePage_choiceGitLocaleLocal_label,
				localeLocalFormat,
				UIText.DateFormatPreferencePage_helpGitLocaleLocal_label));
		d.put(null,
				new FormatInfo(
						UIText.DateFormatPreferencePage_choiceCustom_label, "", //$NON-NLS-1$
						UIText.DateFormatPreferencePage_helpCustom_label));
		return d;
	}

	private static TimeZone getDifferentTimeZone() {
		TimeZone localTimeZone = TimeZone.getDefault();
		int offset = (localTimeZone.getRawOffset() / 3600 / 1000 - 6);
		// 6h to the West, full hour. Now get back to a sane range:
		if (offset < -12) {
			offset += 24;
		}
		String[] zoneIds = TimeZone.getAvailableIDs(offset * 3600 * 1000);
		if (zoneIds.length == 0) {
			// Huh?
			return localTimeZone;
		}
		return TimeZone.getTimeZone(zoneIds[0]);
	}

	private static final class FormatInfo {
		private final String name;

		private final String format;

		private final String explanation;

		public FormatInfo(String name, String dateFormat, String explanation) {
			this.name = name;
			this.format = dateFormat;
			this.explanation = explanation;
		}

	}
}
