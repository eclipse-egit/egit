/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

/**
 * Job families of EGit jobs. May be used in tests to join job execution.
 *
 */
public class JobFamilies {
	/**
	 * GenerateHistoryJob
	 */
	public final static Object GENERATE_HISTORY = new Object();

	/**
	 * Commit job
	 */
	public final static Object COMMIT = new Object();

	/**
	 * Checkout job
	 */
	public final static Object CHECKOUT = new Object();

	/**
	 * Push job
	 */
	public final static Object PUSH = new Object();


}
