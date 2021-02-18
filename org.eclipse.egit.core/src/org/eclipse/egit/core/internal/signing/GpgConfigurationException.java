/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.signing;

/**
 * Exception indicating a problem with the GPG configuration.
 */
public class GpgConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link GpgConfigurationException}.
	 */
	public GpgConfigurationException() {
		super();
	}

	/**
	 * Creates a new {@link GpgConfigurationException} with the given message.
	 *
	 * @param message
	 *            to set
	 */
	public GpgConfigurationException(String message) {
		super(message);
	}

	/**
	 * Creates a new {@link GpgConfigurationException} with the given message
	 * and cause.
	 *
	 * @param message
	 *            to set
	 * @param cause
	 *            of the exception
	 */
	public GpgConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
