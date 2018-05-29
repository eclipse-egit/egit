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
import org.eclipse.team.core.mapping.ResourceMappingMerger;

public class SampleResourceMappingMerger extends ResourceMappingMerger {
	private final ModelProvider provider;

	public SampleResourceMappingMerger(ModelProvider provider) {
		this.provider = provider;
	}

	@Override
	protected ModelProvider getModelProvider() {
		return provider;
	}
}
