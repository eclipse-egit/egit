/*******************************************************************************
 * Copyright (C) 2020, Mykola Zakharchuk <mykola.zakharchuk@advantest.com> and others.
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
 * Interprets the use cases for external merge execution
 *
 */
public enum MergeMode implements DiffMergeMode {

	/**
	 * Invoke prompt when starting tool
	 */
	PROMPT(0),

	/**
	 * Working tree (pre-merged by Git)
	 */
	WORKSPACE(1),

	/**
	 * Last HEAD (unmerged)
	 */
	LAST_HEAD(2),

	/**
	 * Pre-merged to "ours"
	 */
	OURS(3);

	private int value;

	MergeMode(int i) {
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
	public MergeMode fromInt(int i) {
		for (MergeMode b : MergeMode.values()) {
			if (b.getValue() == i) {
				return b;
			}
		}
		return null;
	}
}
