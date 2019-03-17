/*******************************************************************************
 * Copyright (C) 2014 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.models;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.mapping.IResourceMappingMerger;

public class SampleModelAdapterFactory implements IAdapterFactory {
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof SampleModelProvider
				&& IResourceMappingMerger.class.isAssignableFrom(adapterType)) {
			return adapterType.cast(new SampleResourceMappingMerger(
					(ModelProvider) adaptableObject));
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IResourceMappingMerger.class, };
	}
}
