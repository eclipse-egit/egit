/******************************************************************************
 *  Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.jgit.lib.Repository;

/**
 * A property tester for {@link FileDiff}s.
 */
public class FileDiffPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (!(receiver instanceof FileDiff)) {
			return false;
		}
		FileDiff diff = (FileDiff) receiver;
		switch (property) {
		case "isSubmodule": //$NON-NLS-1$
			return computeResult(expectedValue, diff.isSubmodule());
		case "isChange": //$NON-NLS-1$
			if (args == null || args.length == 0) {
				return false;
			}
			String expected = diff.getChange().name();
			boolean matches = false;
			for (Object arg : args) {
				if (arg != null && expected.equalsIgnoreCase(arg.toString())) {
					matches = true;
					break;
				}
			}
			return computeResult(expectedValue, matches);
		case "existsInWorktree": //$NON-NLS-1$
			boolean result = false;
			Repository repo = diff.getRepository();
			if (repo != null && !repo.isBare()) {
				String path = new Path(repo.getWorkTree().getAbsolutePath())
						.append(diff.getPath()).toOSString();
				result = new File(path).exists();
			}
			return computeResult(expectedValue, result);
		default:
			return false;
		}
	}

}
