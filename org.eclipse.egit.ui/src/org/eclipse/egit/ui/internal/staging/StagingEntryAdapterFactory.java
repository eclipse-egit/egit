/*******************************************************************************
 * Copyright (C) 2011, 2015 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.lib.Repository;

/**
 * An adapter factory for <code>StagingEntry</code>s so that the property page
 * handler can open property pages on them correctly.
 */
public class StagingEntryAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject != null) {
			StagingEntry entry = (StagingEntry) adaptableObject;
			if (adapterType == IResource.class) {
				IResource resource = entry.getFile();
				if (resource != null && resource.isAccessible()) {
					return adapterType.cast(resource);
				}
			} else if (adapterType == IPath.class) {
				return adapterType.cast(entry.getLocation());
			} else if (adapterType == Repository.class) {
				return adapterType.cast(entry.getRepository());
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IResource.class, IPath.class,
				Repository.class };
	}

}
