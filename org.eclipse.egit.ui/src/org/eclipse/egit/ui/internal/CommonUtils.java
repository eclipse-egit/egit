/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.math.BigInteger;
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

	private static final Pattern NUMBERS = Pattern.compile("[\\d]*"); //$NON-NLS-1$

	private static final Pattern CHARS = Pattern.compile("[^0-9]*"); //$NON-NLS-1$

	/**
	 * Instance of comparator that sorts strings in ascending alphabetical and
	 * numerous order.
	 */
	public static final Comparator<String> STRING_ASCENDING_COMPARATOR = new Comparator<String>() {
		public int compare(String o1, String o2) {
			String o1Chars = NUMBERS.matcher(o1).replaceAll(""); //$NON-NLS-1$
			String o2Chars = NUMBERS.matcher(o2).replaceAll(""); //$NON-NLS-1$
			int charCompare = o1Chars.compareTo(o2Chars);

			if (charCompare == 0) {
				String o1Numbers = CHARS.matcher(o1).replaceAll(""); //$NON-NLS-1$
				String o2Numbers = CHARS.matcher(o2).replaceAll(""); //$NON-NLS-1$
				if (o1Numbers.length() == 0)
					o1Numbers = "0"; //$NON-NLS-1$
				if (o2Numbers.length() == 0)
					o2Numbers = "0"; //$NON-NLS-1$

				return new BigInteger(o1Numbers).compareTo(new BigInteger(o2Numbers));
			}

			return charCompare;
		}
	};

	/**
	 *
	 */
	public static final Comparator<Ref> REF_ASCENDING_COMPARATOR = new Comparator<Ref>() {
		public int compare(Ref o1, Ref o2) {
			return STRING_ASCENDING_COMPARATOR.compare(o1.getName(), o2.getName());
		}
	};

}
