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

	private String options[] = null;

	/**
	 * @param name
	 * @param path
	 * @param options
	 */
	public PreDefinedTool(String name, String path, String... options) {
		super(name);
		this.path = path;
		this.options = new String[options.length];
		for (int i = 0; i < options.length; i++) {
			this.options[i] = options[i];
		}
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getOptions(int... optionsNr) {
		int onr = optionsNr.length > 0 ? optionsNr[0] : 0;
		if (onr >= options.length || onr < 0)
			onr = 0;
		return options[onr];
	}

	@Override
	public String getCommand(int... optionsNr) {
		return path + " " + getOptions(optionsNr); //$NON-NLS-1$
	}

}
