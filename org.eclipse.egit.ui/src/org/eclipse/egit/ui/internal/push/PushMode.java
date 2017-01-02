/*******************************************************************************
 * Copyright (C) 2015, Frank Jakob
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

/**
 * Push mode: either push to upstream, or push to Gerrit.
 */
public enum PushMode {
	/** Push to upstream. */
	UPSTREAM,
	/** Push to Gerrit. */
	GERRIT;
}
