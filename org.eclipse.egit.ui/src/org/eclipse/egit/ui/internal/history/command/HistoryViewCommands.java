/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

/**
 * Constants for the commands used in the history view.
 * <p>
 * Only those constants are listed that are being used in code.
 */
public class HistoryViewCommands {
	/**
	 * "Compare mode" parameter for the "open" command (see
	 * {@link #SHOWVERSIONS})
	 */
	public static final String COMPARE_MODE_PARAM = "org.eclipse.egit.ui.history.CompareMode"; //$NON-NLS-1$

	/** "Reset" mode (sort, mixed, hard) */
	public static final String RESET_MODE = "org.eclipse.egit.ui.history.ResetMode"; //$NON-NLS-1$

	/** "Target" parameter for setting the quickdiff baseline (HEAD or HEAD^1) */
	public static final String BASELINE_TARGET = "org.eclipse.egit.ui.history.ResetQuickdiffBaselineTarget"; //$NON-NLS-1$

	/** "Open" or "Show Versions" (depending on the selection) */
	public static final String SHOWVERSIONS = "org.eclipse.egit.ui.history.ShowVersions"; //$NON-NLS-1$
}
