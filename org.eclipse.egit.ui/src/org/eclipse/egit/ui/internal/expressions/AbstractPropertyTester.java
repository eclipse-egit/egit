/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.expressions;

import org.eclipse.core.expressions.PropertyTester;

/**
 * Helper class that extends {@link PropertyTester} with methods useful for
 * implementing property testers that can handle expected values and arguments.
 */
public abstract class AbstractPropertyTester extends PropertyTester {

	/**
	 * If the expected value is a {@link Boolean}, returns {@code true} if it's
	 * equal to {@code result}, otherwise returns {@code result}.
	 *
	 * @param expectedValue
	 *            as passed in to the
	 *            {@link PropertyTester#test(Object, String, Object[], Object)
	 *            test()} method (the {@code value="..."} attribute from the
	 *            XML, converted as usual)
	 * @param result
	 *            as computed by the test
	 * @return {@code true} is the {@code result} matches the
	 *         {@code expectedValue}
	 */
	protected boolean computeResult(Object expectedValue, boolean result) {
		if (expectedValue instanceof Boolean) {
			return ((Boolean) expectedValue).booleanValue() == result;
		}
		return result;
	}

}
