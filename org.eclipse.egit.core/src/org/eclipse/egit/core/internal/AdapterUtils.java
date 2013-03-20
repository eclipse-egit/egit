/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Utilities for working with objects that implement {@link IAdaptable}
 */
public class AdapterUtils {

	private AdapterUtils() {
		// Cannot be instantiated
	}

	/**
	 * Adapt object to given target class type
	 *
	 * @param object
	 * @param target
	 * @param <V> type of target
	 * @return adapted
	 */
	@SuppressWarnings("unchecked")
	public static <V> V adapt(Object object, Class<V> target) {
		if (object == null)
			return null;
		if (target.isInstance(object))
			return (V) object;
		if (object instanceof IAdaptable)
			return (V) ((IAdaptable) object).getAdapter(target);
		return null;
	}
}
