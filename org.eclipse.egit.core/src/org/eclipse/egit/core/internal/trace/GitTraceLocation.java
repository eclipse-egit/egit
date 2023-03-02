/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.trace;

import org.eclipse.egit.core.Activator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;

/**
 * EGit Trace locations
 *
 */
public enum GitTraceLocation implements ITraceLocation {
	/** Core */
	CORE("/debug/core"), //$NON-NLS-1$
	/** GPG signing */
	GPG("/debug/core/gpg"), //$NON-NLS-1$
	/** IndexDiffCache */
	INDEXDIFFCACHE("/debug/core/indexdiffcache"), //$NON-NLS-1$
	/** refreshing resources */
	REFRESH("/debug/core/refresh"), //$NON-NLS-1$
	/** performance trace */
	PERFORMANCE("/performance"); //$NON-NLS-1$

	/**
	 * Initializes the locations.
	 *
	 * @param options
	 *            to initialize from
	 */
	public static void initializeFromOptions(DebugOptions options) {
		// we evaluate the plug-in switch
		boolean pluginIsDebugging = options
				.getBooleanOption(Activator.PLUGIN_ID + "/debug", false); //$NON-NLS-1$
		if (pluginIsDebugging) {
			myTrace = options.newDebugTrace(Activator.PLUGIN_ID);
			for (GitTraceLocation loc : values()) {
				boolean active = options.getBooleanOption(loc.getFullPath(),
						false);
				loc.setActive(active);
			}
		} else {
			// if the plug-in switch is off, we don't set the trace instance
			// to null to avoid problems with possibly running trace calls
			for (GitTraceLocation loc : values()) {
				loc.setActive(false);
			}
		}
	}

	private final String location;

	private final String fullPath;

	private boolean active = false;

	private static volatile DebugTrace myTrace;

	private GitTraceLocation(String path) {
		this.fullPath = Activator.PLUGIN_ID + path;
		this.location = path;
	}

	/**
	 * Convenience method
	 *
	 * @return the debug trace (may be null)
	 *
	 **/
	public static DebugTrace getTrace() {
		return GitTraceLocation.myTrace;
	}

	/**
	 *
	 * @return <code>true</code> if this location is active
	 */
	@Override
	public boolean isActive() {
		return this.active;
	}

	/**
	 * @return the full path
	 */
	public String getFullPath() {
		return this.fullPath;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	/**
	 * Sets the "active" flag for this location.
	 * <p>
	 * Used by the initializer
	 *
	 * @param active
	 *            the "active" flag
	 */
	private void setActive(boolean active) {
		this.active = active;
	}
}
