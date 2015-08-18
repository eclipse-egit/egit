/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow;

/**
 * Git flow branch names and prefixes.
 */
public final class GitFlowDefaults {

	/** */
	private static final String FEATURE = "feature"; //$NON-NLS-1$

	/** */
	private static final String RELEASE = "release"; //$NON-NLS-1$

	/** */
	private static final String HOTFIX = "hotfix"; //$NON-NLS-1$

	/** */
	public static final String MASTER = "master"; //$NON-NLS-1$

	/** */
	public static final String DEVELOP = "develop"; //$NON-NLS-1$

	/** */
	private static final String SLASH = "/"; //$NON-NLS-1$

	/** */
	public static final String FEATURE_PREFIX = FEATURE + SLASH;

	/** */
	public static final String RELEASE_PREFIX = RELEASE + SLASH;

	/** */
	public static final String HOTFIX_PREFIX = HOTFIX + SLASH;

	/** */
	public static final String VERSION_TAG = ""; //$NON-NLS-1$
}
