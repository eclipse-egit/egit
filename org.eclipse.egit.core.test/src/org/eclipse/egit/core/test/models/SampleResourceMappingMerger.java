/*******************************************************************************
 * Copyright (C) 2014 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
