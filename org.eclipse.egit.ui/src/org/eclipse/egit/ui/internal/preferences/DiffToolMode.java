/*
 * Copyright (C) 2019, Mykola Zakharchuk <mykola.zakharchuk@advantest.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.egit.ui.internal.preferences;

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
