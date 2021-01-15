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

import java.util.Collections;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * Property source to display a list of values.
 */
public class ListPropertySource implements IPropertySource {

	private final List<?> values;

	private final IPropertyDescriptor[] descriptors;

	/**
	 * Creates a new {@link ListPropertySource}.
	 *
	 * @param values
	 *            to show
	 */
	public ListPropertySource(List<?> values) {
		this.values = values == null ? Collections.emptyList() : values;
		IPropertyDescriptor[] result = new IPropertyDescriptor[this.values
				.size()];
		for (int i = 1; i <= result.length; i++) {
			result[i - 1] = new PropertyDescriptor(Integer.valueOf(i),
					Integer.toString(i));
		}
		descriptors = result;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return descriptors;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id instanceof Integer) {
			int index = ((Integer) id).intValue() - 1;
			if (index >= 0 && index < values.size()) {
				return values.get(index);
			}
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// Read-only
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// Read-only
	}

	@Override
	public String toString() {
		return values.isEmpty() ? "" : values.toString(); //$NON-NLS-1$
	}
}
