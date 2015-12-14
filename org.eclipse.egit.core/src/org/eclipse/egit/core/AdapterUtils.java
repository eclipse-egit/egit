/******************************************************************************
 *  Copyright (c) 2011, 2015 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.jgit.annotations.Nullable;

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
	@Nullable
	public static <V> V adapt(Object object, Class<V> target) {
		if (object == null) {
			return null;
		}
		if (target.isInstance(object)) {
			return target.cast(object);
		}
		if (object instanceof IAdaptable) {
			V adapter = Utils.getAdapter(((IAdaptable) object), target);
			if (adapter != null || object instanceof PlatformObject) {
				return adapter;
			}
		}
		Object adapted = Platform.getAdapterManager().getAdapter(object, target);
		return target.cast(adapted);
	}
}
