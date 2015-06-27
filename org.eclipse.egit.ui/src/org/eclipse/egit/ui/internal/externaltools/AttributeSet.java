/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.externaltools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author anb0s
 *
 */
public class AttributeSet {

	private Set<Attribute> attributes = new HashSet<>();

	/**
	 * @param name
	 * @param value
	 */
	public void addAttribute(String name, String value) {
		removeAttribute(name);
		Attribute attr = new Attribute(name, value);
		attributes.add(attr);
	}

	/**
	 * @param name
	 */
	public void removeAttribute(String name) {
		Attribute attr = getAttribute(name);
		if (attr != null) {
			attributes.remove(attr);
		}
	}

	/**
	 * @param name
	 * @return the attribute
	 */
	public Attribute getAttribute(String name) {
		for (Iterator<Attribute> iterator = attributes
				.iterator(); iterator.hasNext();) {
			Attribute attr = iterator.next();
			if (attr.getName().equals(name)) {
				return attr;
			}
		}
		return null;
	}

}
