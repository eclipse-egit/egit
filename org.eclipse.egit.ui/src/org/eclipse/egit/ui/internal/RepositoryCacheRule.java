/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

/**
 * A scheduling rule that can be used by (background) jobs that access the
 * {@link org.eclipse.egit.core.RepositoryCache}. Conflicts with other instances
 * of this class or subclasses, and contains only other RepositoryCacheRules.
 */
public class RepositoryCacheRule implements ISchedulingRule {

	@Override
	public boolean contains(ISchedulingRule rule) {
		if (rule instanceof RepositoryCacheRule) {
			return true;
		} else if (rule instanceof MultiRule) {
			for (ISchedulingRule child : ((MultiRule) rule).getChildren()) {
				if (!contains(child)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof RepositoryCacheRule) {
			return true;
		} else if (rule instanceof MultiRule) {
			for (ISchedulingRule child : ((MultiRule) rule).getChildren()) {
				if (isConflicting(child)) {
					return true;
				}
			}
		}
		return false;
	}

}
