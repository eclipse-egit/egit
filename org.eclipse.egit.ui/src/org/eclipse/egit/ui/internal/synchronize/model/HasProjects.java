/*******************************************************************************
 * Copyright (C) 2012, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import org.eclipse.core.resources.IProject;

/**
 * Indicates does contain projects
 */
public interface HasProjects {

	/**
	 * @return list of projects
	 */
	IProject[] getProjects();

}
