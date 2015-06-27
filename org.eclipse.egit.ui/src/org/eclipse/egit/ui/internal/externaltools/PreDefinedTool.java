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
public class PreDefinedTool extends BaseTool {

	private String path = null;
	private String options = null;

	/**
	 * @param name
	 * @param path
	 * @param options
	 */
	public PreDefinedTool(String name, String path, String options) {
		super(name);
		this.path = path;
		this.options = options;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getOptions() {
		return options;
	}

	@Override
	public String getCommand() {
		return path + " " + options; //$NON-NLS-1$
	}

}
