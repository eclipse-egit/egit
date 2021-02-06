/*******************************************************************************
 * Copyright (C) 2021, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

enum EGitFlowOperation {
	START("start"), //$NON-NLS-1$
	PUBLISH("publish"), //$NON-NLS-1$
	TRACK("track"), //$NON-NLS-1$
	FINISH("finish"); //$NON-NLS-1$

	private final String id;

	private EGitFlowOperation(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}
}
