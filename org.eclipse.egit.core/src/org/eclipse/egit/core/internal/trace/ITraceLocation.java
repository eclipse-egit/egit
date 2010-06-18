/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.trace;

/**
 * Helper interface for trace location management
 * <p>
 * Additional methods could return a description or such...
 *
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
