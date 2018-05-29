/*******************************************************************************
 * Copyright (c) 2016, Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - [485124] initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.pull.PullWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * A handler action for asking user to specify a pull operation (via wizard) and
 * run it
 *
 * @see PullWithOptionsAction
 */
public class PullWithOptionsActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repo = getRepository();
		WizardDialog dialog = new WizardDialog(getShell(event),
				new PullWizard(repo));
		dialog.open();
		return null;

	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
