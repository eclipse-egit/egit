/*******************************************************************************
 * Copyright (C) 2019, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.mapping.ResourceMapping;

/**
 * Abstract base class for decoratable {@link ResourceMapping}s.
 */
public abstract class DecoratableResourceGroup extends DecoratableResource {

	/** Set to {@code true} if there is at least one shared resource. */
	protected boolean someShared = false;

	/**
	 * Creates a new {@link DecoratableResourceGroup}.
	 *
	 * @param mapping
	 *            to decorate
	 */
	protected DecoratableResourceGroup(ResourceMapping mapping) {
		super(null); // No resource
	}

	/**
	 * Tells whether the ResourceMapping contains shared resources at all.
	 *
	 * @return whether the resource mapping contains any shared resources
	 */
	public boolean hasSharedResources() {
		return someShared;
	}

}
