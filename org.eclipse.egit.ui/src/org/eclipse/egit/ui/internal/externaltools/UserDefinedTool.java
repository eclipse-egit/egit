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
public class UserDefinedTool extends BaseTool {

	private String cmd = null;

	/**
	 * @param name
	 *            tool name
	 * @param cmd
	 *            tool command
	 */
	public UserDefinedTool(String name, String cmd) {
		super(name);
		this.cmd = cmd;
	}

	@Override
	public String getCommand(int... optionsNr) {
		return cmd;
	}
}
