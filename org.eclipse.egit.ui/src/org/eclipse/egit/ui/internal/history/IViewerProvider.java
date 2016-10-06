/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jface.viewers.Viewer;

/**
 * An abstraction for something that can provide access to a {@link Viewer}.
 */
public interface IViewerProvider {

	/**
	 * Obtains the {@link Viewer}.
	 * 
	 * @return the viewer
	 */
	Viewer getViewer();

}
