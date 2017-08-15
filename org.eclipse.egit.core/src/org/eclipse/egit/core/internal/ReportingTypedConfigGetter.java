/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.DefaultTypedConfigGetter;
import org.eclipse.jgit.lib.ITypedConfigGetter;
import org.eclipse.jgit.storage.file.FileBasedConfig;

/**
 * A {@link ITypedConfigGetter} that logs and returns the default values if the
 * config is invalid.
 */
public class ReportingTypedConfigGetter extends DefaultTypedConfigGetter {

	@Override
	public boolean getBoolean(Config config, String section, String subsection,
			String name, boolean defaultValue) {
		try {
			return super.getBoolean(config, section, subsection, name,
					defaultValue);
		} catch (IllegalArgumentException e) {
			warn(config, join(section, subsection, name),
					Boolean.toString(defaultValue), e);
			return defaultValue;
		}
	}

	@Override
	public <T extends Enum<?>> T getEnum(Config config, T[] all, String section,
			String subsection, String name, T defaultValue) {
		try {
			return super.getEnum(config, all, section, subsection, name,
					defaultValue);
		} catch (IllegalArgumentException e) {
			String valueUsed;
			if (defaultValue instanceof Config.ConfigEnum) {
				valueUsed = ((Config.ConfigEnum) defaultValue).toConfigValue();
			} else {
				valueUsed = defaultValue.toString().toLowerCase(Locale.ROOT);
			}
			warn(config, join(section, subsection, name), valueUsed, e);
			return defaultValue;
		}
	}

	@Override
	public int getInt(Config config, String section, String subsection,
			String name, int defaultValue) {
		try {
			return super.getInt(config, section, subsection, name,
					defaultValue);
		} catch (IllegalArgumentException e) {
			warn(config, join(section, subsection, name),
					Integer.toString(defaultValue), e);
			return defaultValue;
		}
	}

	@Override
	public long getLong(Config config, String section, String subsection,
			String name, long defaultValue) {
		try {
			return super.getLong(config, section, subsection, name,
					defaultValue);
		} catch (IllegalArgumentException e) {
			warn(config, join(section, subsection, name),
					Long.toString(defaultValue), e);
			return defaultValue;
		}
	}

	@Override
	public long getTimeUnit(Config config, String section, String subsection,
			String name, long defaultValue, TimeUnit wantUnit) {
		try {
			return super.getTimeUnit(config, section, subsection, name,
					defaultValue, wantUnit);
		} catch (IllegalArgumentException e) {
			warn(config, join(section, subsection, name),
					Long.toString(defaultValue) + ' ' + toString(wantUnit), e);
			return defaultValue;
		}
	}

	private static void warn(Config config, String entry, String defaultValue,
			Throwable cause) {
		String location = null;
		if (config instanceof FileBasedConfig) {
			File file = ((FileBasedConfig) config).getFile();
			location = file.getAbsolutePath();
		}
		if (location == null) {
			Activator.logWarning(MessageFormat.format(
					CoreText.ReportingTypedConfigGetter_invalidConfig, entry,
					defaultValue), cause);
		} else {
			Activator.logWarning(
					MessageFormat.format(
							CoreText.ReportingTypedConfigGetter_invalidConfigWithLocation,
							location, entry, defaultValue),
					cause);
		}
	}

	private static String join(String section, String subsection, String name) {
		if (subsection == null) {
			return section + '.' + name;
		}
		return section + '.' + subsection + '.' + name;
	}

	private static String toString(TimeUnit unit) {
		switch (unit) {
		case NANOSECONDS:
			return "ns"; //$NON-NLS-1$
		case MICROSECONDS:
			return "µs"; //$NON-NLS-1$
		case MILLISECONDS:
			return "ms"; //$NON-NLS-1$
		case SECONDS:
			return "s"; //$NON-NLS-1$
		case MINUTES:
			return "m"; //$NON-NLS-1$
		case HOURS:
			return "h"; //$NON-NLS-1$
		case DAYS:
			return "d"; //$NON-NLS-1$
		default:
			return ""; //$NON-NLS-1$
		}
	}
}
