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
	// TODO: this must be configurable

	/** */
	public static final String MASTER = "master"; //$NON-NLS-1$

	/** */
	public static final String DEVELOP = "develop"; //$NON-NLS-1$

	/** */
	public static final String FEATURE_PREFIX = "feature/"; //$NON-NLS-1$

	/** */
	public static final String RELEASE_PREFIX = "release/"; //$NON-NLS-1$

	/** */
	public static final String HOTFIX_PREFIX = "hotfix/"; //$NON-NLS-1$

	/** */
	public static final String VERSION_TAG = ""; //$NON-NLS-1$
}
