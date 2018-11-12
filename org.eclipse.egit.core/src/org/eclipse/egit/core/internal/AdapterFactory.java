/*******************************************************************************
 * Copyright (c) 2018 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.jgit.lib.Repository;

/**
 * An {@link IAdapterFactory} for core items.
 */
public class AdapterFactory implements IAdapterFactory {

	// TODO: Generify once baseline is Eclipse Neon
	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == Repository.class) {
			if (adaptableObject instanceof GitFileRevision) {
				return ((GitFileRevision) adaptableObject).getRepository();
			}
		}
		return null;
	}

	// TODO: Generify once baseline is Eclipse Neon
	@Override
	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { Repository.class };
	}
}
