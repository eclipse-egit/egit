/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.resources.mapping.ModelProvider;

/**
 * Interface that third party {@link ModelProvider}s can adapt to to indicate
 * that they don't want to participate in EGit synchronizations.
 */
public interface IgnoreInGitSynchronizations {
	// Empty on purpose
}
