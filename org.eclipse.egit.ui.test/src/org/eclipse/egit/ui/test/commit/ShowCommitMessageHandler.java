/*******************************************************************************
 * Copyright (C) 2025 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.commit.CommitContext;
import org.eclipse.egit.ui.commit.CommitContextUtils;
import org.eclipse.jgit.lib.Repository;

public class ShowCommitMessageHandler extends AbstractHandler {

	public static final String NEW_MESSAGE = "Changed\n\nmessage\n";

	public static record Data(Repository repository, String message) {
		// Empty
	}

	public static volatile Data lastData = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CommitContext context = CommitContextUtils.getCommitContext(event);
		if (context != null) {
			Repository repository = context.getRepository();
			if (repository != null) {
				lastData = new Data(repository, context.getCommitMessage());
				context.setCommitMessage(NEW_MESSAGE);
			}
		}
		return null;
	}

}
