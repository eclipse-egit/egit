/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import static java.lang.System.arraycopy;
import static java.lang.reflect.Array.newInstance;

import java.util.Arrays;

import org.eclipse.jgit.lib.ObjectId;

/**
 * Utility class
 *
 */
public class Utils {

	/**
	 * @param id
	 * @return a shortened ObjectId (first 6 digits)
	 */
	public static String getShortObjectId(ObjectId id) {
		return id.getName().substring(0, 6);
	}

	/**
	 * This is a simple replacement for {@link Arrays#copyOf(Object[], int)}
	 * until we switch on JDK 6
	 *
	 * @param <T>
	 * @param source
	 * @return copy of given array
	 */
	public static <T> T[] copyOf(T[] source) {
		int length = source.length;
		@SuppressWarnings("unchecked")
		T[] result = (T[]) newInstance(source.getClass()
				.getComponentType(), length);
		arraycopy(source, 0, result, 0, length);

		return result;
	}

}
