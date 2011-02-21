/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.jgit.revwalk;

import org.eclipse.egit.ui.internal.synchronize.mapping.GitChangeSetSorterTest;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;

/**
 * The only reason of this class existence is to tests {@link GitModelCommit}
 * ordering in {@link GitChangeSetSorterTest}
 * 
 * DO NOT USE IT ELSEWERE!
 */
public class MockRevCommit extends RevCommit {

	public MockRevCommit(int commitTime) {
		super(zeroId());
		this.commitTime = commitTime;
	}

}
