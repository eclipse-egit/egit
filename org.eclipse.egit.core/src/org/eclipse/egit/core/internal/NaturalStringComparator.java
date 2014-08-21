/*******************************************************************************
 * Copyright (C) 2014 Andreas Hermann <a.v.hermann@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static java.lang.Character.isDigit;

import java.util.Comparator;

/**
 * Implements comparison of strings containing numbers. Instead of sorting these
 * strings in ASCII order like the default java string comparator, this
 * comparator sorts the contained numbers numerically. <br>
 */
public class NaturalStringComparator implements Comparator<String> {

	/**
	 * Comparator instance comparing strings containing numbers in natural
	 * order.
	 */
	public static NaturalStringComparator INSTANCE = new NaturalStringComparator();

	private NaturalStringComparator() {
		// prevent instantiation
	}

	public int compare(String str1, String str2) {
		int pos1 = 0;
		int pos2 = 0;

		while (pos1 < str1.length() && pos2 < str2.length()) {
			boolean digit1 = isDigit(str1.charAt(pos1));
			boolean digit2 = isDigit(str2.charAt(pos2));

			if (digit1 && digit2) {
				// find next number
				int i1 = pos1;
				while (i1 < str1.length() && isDigit(str1.charAt(i1)))
					i1++;
				Long n1 = Long.valueOf(str1.substring(pos1, i1));
				pos1 = i1;

				// find next number
				int i2 = pos2;
				while (i2 < str2.length() && isDigit(str2.charAt(i2)))
					i2++;
				Long n2 = Long.valueOf(str2.substring(pos2, i2));
				pos2 = i2;

				int res = n1.compareTo(n2);
				if (res != 0)
					return res;
			} else if (digit1 != digit2) {
				return str1.substring(pos1).compareTo(str2.substring(pos2));
			} else {
				// find next string
				int i1 = pos1;
				while (i1 < str1.length() && !isDigit(str1.charAt(i1))) {
					i1++;
				}
				String t1 = str1.substring(pos1, i1);
				pos1 = i1;

				// find next string
				int i2 = pos2;
				while (i2 < str2.length() && !isDigit(str2.charAt(i2))) {
					i2++;
				}
				String t2 = str2.substring(pos2, i2);
				pos2 = i2;

				int res = t1.compareTo(t2);
				if (res != 0)
					return res;
			}
		}

		return 0;
	}
}
