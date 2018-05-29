/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.push.PushTagsWizard;
import org.eclipse.jgit.lib.Repository;

/**
 * "Push Tags..." action for repository
 */
public class PushTagsActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);

		PushTagsWizard.openWizardDialog(repository);

		return null;
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
