/*******************************************************************************
 * Copyright (C) 2017, SATO Yusuke <yusuke.sato.zz@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Log operation that changes HEAD.
 */
public class OperationLogger {
	private final String startMessage;

	private final String endMessage;

	private final String errorMessage;

	private final String[] parameter;

	/**
	 * Set messages for logging.
	 *
	 * @param startMessage
	 *            message starts with "[Start]"
	 * @param endMessage
	 *            message starts with "[End]"
	 * @param errorMessage
	 *            message starts with "[Error]"
	 * @param parameter
	 *            parameter for log message
	 */
	public OperationLogger(String startMessage, String endMessage,
			String errorMessage, String[] parameter) {
		this.startMessage = startMessage;
		this.endMessage = endMessage;
		this.errorMessage = errorMessage;
		this.parameter = parameter;
	}

	/**
	 * log a message starts with "[Start]"
	 */
	public void logStart() {
		Activator.logInfo(NLS.bind(startMessage, parameter));
	}

	/**
	 * log a message starts with "[End]"
	 */
	public void logEnd() {
		Activator.logInfo(NLS.bind(endMessage, parameter));
	}

	/**
	 * log a message starts with "[End]" with additional parameter
	 *
	 * @param extraParameter
	 *            extra parameter
	 */
	public void logEnd(String[] extraParameter) {
		String[] newParameter = Stream.concat(Arrays.stream(this.parameter),
				Arrays.stream(extraParameter)).toArray(String[]::new);
		Activator.logInfo(NLS.bind(endMessage, newParameter));
	}

	/**
	 * log a message starts with "[Error]"
	 *
	 * @param e
	 *            cause of error
	 */
	public void logError(Exception e) {
		Activator.logInfo(
				NLS.bind(errorMessage, parameter) + ", " + e.getMessage()); //$NON-NLS-1$
	}

	/**
	 * Get short name of current branch
	 *
	 * @param repo
	 * @return branch name
	 */
	public static String getCurrentBranch(final Repository repo) {
		try {
			return repo.getBranch();
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}
}
