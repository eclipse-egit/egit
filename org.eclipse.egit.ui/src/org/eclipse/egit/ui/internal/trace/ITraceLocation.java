/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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
package org.eclipse.egit.ui.internal.trace;

/**
 * Helper interface for trace location management
 * <p>
 * Additional methods could return a description or such...
 */
public interface ITraceLocation {

	/**
	 * @return the location, e.g. "/debug/mainArea/subArea"
	 */
	public String getLocation();

	/**
	 * @return <code>true</code> if the location is active
	 */
	public boolean isActive();

}
