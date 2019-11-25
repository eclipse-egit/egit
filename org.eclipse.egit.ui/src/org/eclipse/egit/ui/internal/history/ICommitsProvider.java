/*******************************************************************************
 * Copyright (C) 2019 Simon Muschel <smuschel@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Simon Muschel <smuschel@gmx.de> - Bug 345466
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.revwalk.RevFlag;

interface ICommitsProvider {

	Object getSearchContext();

	SWTCommit[] getCommits();

	RevFlag getHighlight();
}