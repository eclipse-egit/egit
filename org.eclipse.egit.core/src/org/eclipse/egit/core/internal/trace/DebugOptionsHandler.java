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
package org.eclipse.egit.core.internal.trace;

import org.eclipse.egit.core.Activator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi component to get notified about debug option changes.
 */
@Component(property = DebugOptions.LISTENER_SYMBOLICNAME + '='
		+ Activator.PLUGIN_ID)
public class DebugOptionsHandler implements DebugOptionsListener {

	@Override
	public void optionsChanged(DebugOptions options) {
		GitTraceLocation.initializeFromOptions(options);
	}
}
