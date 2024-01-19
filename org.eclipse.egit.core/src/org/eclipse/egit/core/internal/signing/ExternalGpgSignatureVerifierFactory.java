/*******************************************************************************
 * Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.signing;

import org.eclipse.jgit.lib.GpgSignatureVerifier;
import org.eclipse.jgit.lib.GpgSignatureVerifierFactory;

/**
 * A factory to get the {@link ExternalGpgSignatureVerifier}.
 */
public class ExternalGpgSignatureVerifierFactory
		extends GpgSignatureVerifierFactory {

	private static final ExternalGpgSignatureVerifier VERIFIER = new ExternalGpgSignatureVerifier();

	@Override
	public GpgSignatureVerifier getVerifier() {
		return VERIFIER;
	}

}
