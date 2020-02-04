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
 * Interprets the use cases for external difftools:
 * <ul>
 * <li>Use Built-in Eclipse compare editor to execute diff.
 * <li>Use Built-in Eclipse compare editor to execute diff with an option to use
 * (external) tool specified in git attributes (checkbox "If configured by git,
 * use external tool"). This option should be set per default and allows
 * end-user to use the standard approach to execute diff (with Eclipse compare tool) and
 * triggers the execution of an external difftool, if one explicitly specified
 * external tool in git attributes. If the checkbox is disabled - use Eclipse
 * compare for everything (default Eclipse behavior).
 * <li>Use external difftool, trust git (.gitconfig and .gitattributes). This
 * option relies on the system configuration of git (.gitconfig, .gitattributes)
 * and obeys the general rules of git configs. Git attributes wins over default
 * difftool specified by gitconfig.
 * <li>External (specified) tool. End-user specifies which tool he wants to use
 * from the list (provided by git configuration). We force Eclipse and JGit to
 * use this specified external difftool for every diff execution, independently
 * of what is specified by git attributes / git config.
 * </ul>
 *
 */
public enum DiffToolMode implements DiffMergeMode {

	/**
	 * Use internal Eclipse compare editor.
	 */
	INTERNAL(0),
	/**
	 * Use external diff tools only for data types specifically specified in git
	 * attributes or preferences.
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

	DiffToolMode(int i) {
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
	public static DiffToolMode fromInt(int i) {
		for (DiffToolMode b : DiffToolMode.values()) {
			if (b.getValue() == i) {
				return b;
			}
		}
		return null;
	}
}
