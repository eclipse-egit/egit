/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.properties;

import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * A {@link PropertyDescriptor} that uses the label as description if no
 * explicit description is set.
 */
public class GitPropertyDescriptor extends PropertyDescriptor {

	/**
	 * Creates a new {@link GitPropertyDescriptor}.
	 *
	 * @param id
	 *            for the property
	 * @param label
	 *            for the property
	 */
	public GitPropertyDescriptor(Object id, String label) {
		super(id, label);
	}

	@Override
	public String getDescription() {
		String description = super.getDescription();
		if (description == null) {
			description = getDisplayName();
		}
		return description;
	}
}
