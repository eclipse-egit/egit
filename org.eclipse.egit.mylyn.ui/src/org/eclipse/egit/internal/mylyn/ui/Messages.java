/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid
 * duplicating strings.
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.egit.internal.mylyn.ui.messages"; //$NON-NLS-1$

	/** */
	public static String CommitHyperlinkDetector_CommitNotFound;

	/**	 */
	public static String CommitHyperlinkDetector_CommitNotFoundInRepositories;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
