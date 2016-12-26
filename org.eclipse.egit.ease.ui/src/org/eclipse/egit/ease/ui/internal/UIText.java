/*******************************************************************************
 * Copyright (C) 2017, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ease.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid duplicating strings.
 */
public class UIText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the "Find Broken Externalized Strings" tool will not be able to find the corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.ease.ui.internal.uitext"; //$NON-NLS-1$

	static {
		initializeMessages(BUNDLE_NAME, UIText.class);
	}

	/**	 */
	public static String commits;

	/**	 */
	public static String DynamicHistoryMenu_startGitflowReleaseFrom;

	/**	 */
	public static String failedToCheckoutBranch;

	/**	 */
	public static String merging;

	/**	 */
	public static String rebaseFailed;

	/**	 */
	public static String rebasing;

	/**	 */
	public static String rebasingOn;
}
