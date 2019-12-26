/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2017, Martin Fleck <mfleck@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;

/**
 * A decoratable resource used for testing to easily compare decoratable
 * resources and their resource state information.
 */
public class TestDecoratableResource extends DecoratableResource {

	private String name = null;
	private int type = -1;

	/**
	 * Creates a new decoratable resource based on the given resource.
	 *
	 * @param resource
	 *            resource to be decorated
	 */
	public TestDecoratableResource(IResource resource) {
		super(resource);
	}

	/**
	 * Creates a new decoratable resource using the specified name and type. For
	 * comparisons, the name and type must match.
	 *
	 * @param name
	 *            name of the decoratable resource
	 * @param type
	 *            type of the decoratable resource
	 */
	public TestDecoratableResource(String name, int type) {
		super(null);
		this.name = name;
		this.type = type;
	}

	public TestDecoratableResource tracked() {
		setTracked(true);
		return this;
	}

	public TestDecoratableResource ignored() {
		setIgnored(true);
		return this;
	}

	public TestDecoratableResource dirty() {
		setDirty(true);
		return this;
	}

	public TestDecoratableResource conflicts() {
		setConflicts(true);
		return this;
	}

	public TestDecoratableResource added() {
		setStagingState(StagingState.ADDED);
		return this;
	}

	public TestDecoratableResource removed() {
		setStagingState(StagingState.REMOVED);
		return this;
	}

	public TestDecoratableResource modified() {
		setStagingState(StagingState.MODIFIED);
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IDecoratableResource))
			return false;

		IDecoratableResource decoratableResource = (IDecoratableResource) obj;
		if (decoratableResource.getType() != getType())
			return false;
		if (!decoratableResource.getName().equals(getName()))
			return false;
		if (decoratableResource.isTracked() != isTracked())
			return false;
		if (decoratableResource.isIgnored() != isIgnored())
			return false;
		if (decoratableResource.isDirty() != isDirty())
			return false;
		if (decoratableResource.hasConflicts() != hasConflicts())
			return false;
		if (!decoratableResource.getStagingState().equals(getStagingState()))
			return false;

		return true;
	}

	@Override
	public String getName() {
		if (name != null) {
			return name;
		}
		return super.getName();
	}

	@Override
	public int getType() {
		if (type >= 0) {
			return type;
		}
		return super.getType();
	}

	@Override
	public int hashCode() {
		// this appeases FindBugs
		return super.hashCode();
	}
}
