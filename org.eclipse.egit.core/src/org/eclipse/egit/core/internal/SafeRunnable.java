/*******************************************************************************
 * Copyright (C) 2018 Michael Keppler <michael.keppler@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

/**
 * Helper class to invoke the code of an {@link ISafeRunnable} as lambda
 * expression.
 */
public final class SafeRunnable {

	private SafeRunnable() {
		// No instantiation of utility class
	}

	/**
	 * Helper to invoke the code of an {@link ISafeRunnable} as lambda
	 * expression.
	 *
	 * @param code
	 *            implementation of the {@link ISafeRunnable#run()} method
	 */
	public static void run(Runner code) {
		SafeRunner.run(code);
	}

	/**
	 * Helper interface implementing the exception handling of
	 * {@link ISafeRunnable}, thereby creating a functional interface for the
	 * remaining {@link ISafeRunnable#run()} method.
	 *
	 */
	@FunctionalInterface
	public interface Runner extends ISafeRunnable {

		@Override
		default void handleException(Throwable exception) {
			// logged by SafeRunner
		}
	}
}