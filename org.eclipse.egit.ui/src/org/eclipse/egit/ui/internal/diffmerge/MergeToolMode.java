/*******************************************************************************
 * Copyright (C) 2020, Mykola Zakharchuk <mykola.zakharchuk@advantest.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.diffmerge;

/**
 * Interprets the use cases for external mergetools:
 * <ul>
 * <li>Use Built-in Eclipse compare editor to invoke merge procedure.
 * <li>External (specified) tool. End-user specifies which tool he wants to use
 * from the list (provided by git configuration). We force Eclipse and JGit to
 * use this specified external mergetool for every merge execution,
 * independently of what is specified by git attributes / git config.
 * </ul>
 *
 */
public enum MergeToolMode implements DiffMergeMode {

	/**
	 * Use internal Eclipse compare editor.
	 */
	INTERNAL(0),
	/**
	 * Use external diff tools only for data types specifically specified in git
	 * attributes.
	 */
	EXTERNAL_FOR_TYPE(1),
	/**
	 * Use git configuration defined in .gitconfig and git attributes.
	 */
	GIT_CONFIG(2),
	/**
	 * Use an external diff tool.
	 */
	EXTERNAL(3);

	private int value;

	MergeToolMode(int i) {
		this.value = i;
	}

	/**
	 * @return value of the enum
	 */
	@Override
	public int getValue() {
		return value;
	}

	/**
	 * @param i
	 * @return get corresponding enum from value
	 */
	public static MergeToolMode fromInt(int i) {
		for (MergeToolMode b : MergeToolMode.values()) {
			if (b.getValue() == i) {
				return b;
			}
		}
		return null;
	}
}
