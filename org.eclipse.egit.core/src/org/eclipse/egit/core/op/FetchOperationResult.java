/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.URIish;

/**
 * Stores the result of a fetch operation
 */
public class FetchOperationResult {
	private final URIish uri;

	private final FetchResult fetchResult;

	private final String fetchErrorMessage;

	/**
	 * @param uri
	 * @param result
	 */
	public FetchOperationResult(URIish uri, FetchResult result) {
		this.uri = uri;
		this.fetchResult = result;
		this.fetchErrorMessage = null;
	}

	/**
	 * @param uri
	 * @param errorMessage
	 */
	public FetchOperationResult(URIish uri, String errorMessage) {
		this.uri = uri;
		this.fetchResult = null;
		this.fetchErrorMessage = errorMessage;
	}

	/**
	 * @return the URI
	 *
	 */
	public URIish getURI() {
		return uri;
	}

	/**
	 * @return the result
	 */
	public FetchResult getFetchResult() {
		return fetchResult;
	}

	/**
	 * @return the error message
	 */
	public String getErrorMessage() {
		return fetchErrorMessage;
	}
}
