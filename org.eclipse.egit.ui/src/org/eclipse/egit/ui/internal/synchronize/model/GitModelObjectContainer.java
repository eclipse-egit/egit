/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

/**
 * An abstract class for all container models in change set.
 */
public abstract class GitModelObjectContainer extends GitModelObject {

	/**
	 * @param parent
	 *            parent object
	 */
	public GitModelObjectContainer(GitModelObjectContainer parent) {
		super(parent);
	}

	@Override
	public boolean isContainer() {
		return true;
	}

}
