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
public class BaseTool implements ITool {

	private String name = null;

	private AttributeSet attributes = new AttributeSet();

	/**
	 * @param name
	 */
	public BaseTool(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public String getOptions(int... optionsNr) {
		return null;
	}

	@Override
	public String getCommand(int... optionsNr) {
		return null;
	}

	@Override
	public AttributeSet getAttributeSet() {
		return attributes;
	}

}
