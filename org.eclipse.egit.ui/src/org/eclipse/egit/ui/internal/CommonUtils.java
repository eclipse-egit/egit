/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Comparator;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Ref;

/**
 * Class containing all common utils
 */
public class CommonUtils {

	private CommonUtils() {
		// non-instantiable utility class
	}

	private static final Pattern BETWEEN_PARTS =
			Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"); //$NON-NLS-1$

	private static final Pattern LEADING_ZEROS = Pattern.compile("^0*"); //$NON-NLS-1$

	/**
	 * Instance of comparator that sorts strings in ascending alphabetical and
	 * numerous order (also known as natural order).
	 */
	public static final Comparator<String> STRING_ASCENDING_COMPARATOR = new Comparator<String>() {
		public int compare(String o1, String o2) {
			if (o1.length() == 0)
				return -1;
			if (o2.length() == 0)
				return 1;

			String[] o1Parts = BETWEEN_PARTS.split(o1);
			String[] o2Parts = BETWEEN_PARTS.split(o2);

			for (int i = 0; i < o1Parts.length; i++) {
				if (i >= o2Parts.length)
					return 1;

				String o1Part = o1Parts[i];
				String o2Part = o2Parts[i];

				int result;

				if (Character.isDigit(o1Part.charAt(0)) && Character.isDigit(o2Part.charAt(0))) {
					o1Part = LEADING_ZEROS.matcher(o1Part).replaceFirst(""); //$NON-NLS-1$
					o2Part = LEADING_ZEROS.matcher(o2Part).replaceFirst(""); //$NON-NLS-1$
					result = o1Part.length() - o2Part.length();
					if (result == 0)
						result = o1Part.compareTo(o2Part);
				} else {
					result = o1Part.compareTo(o2Part);
				}

				if (result != 0)
					return result;
			}

			return -1;
		}
	};

	/**
	 * Instance of comparator which sorts {@link Ref} names using
	 * {@link CommonUtils#STRING_ASCENDING_COMPARATOR}.
	 */
	public static final Comparator<Ref> REF_ASCENDING_COMPARATOR = new Comparator<Ref>() {
		public int compare(Ref o1, Ref o2) {
			return STRING_ASCENDING_COMPARATOR.compare(o1.getName(), o2.getName());
		}
	};

}
