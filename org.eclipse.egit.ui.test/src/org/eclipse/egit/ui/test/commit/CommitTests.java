/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.test.commit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Commit unit test suite
 */
@RunWith(Suite.class)
@SuiteClasses({ CommitEditorInputFactoryTest.class, //
		CommitEditorInputTest.class, //
		CommitEditorTest.class, //
		DiffStyleRangeFormatterTest.class, //
		RepositoryCommitTest.class, //
})
public class CommitTests {
	// Intentionally left blank
}
