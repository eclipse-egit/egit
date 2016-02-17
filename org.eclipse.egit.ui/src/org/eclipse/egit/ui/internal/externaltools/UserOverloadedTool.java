/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.externaltools;

/**
 * @author anb0s
 *
 */
public class UserOverloadedTool extends PreDefinedTool {

	/**
	 * @param tool
	 *            the predefined tool that will be overloaded
	 * @param path
	 *            new / overloaded tool path
	 */
	public UserOverloadedTool(ITool tool, String path) {
		super(tool.getName(), path, tool.getOptions());
	}

}
