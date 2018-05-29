/*******************************************************************************
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.clean;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * A Wizard that guides the user through cleaning the repository.
 */
public class CleanWizardDialog extends WizardDialog {

	/**
	 * Creates a new Clean Wizard instance
	 *
	 * @param parentShell
	 *            parent shell
	 * @param repository
	 */
	public CleanWizardDialog(Shell parentShell, Repository repository) {
		super(parentShell, new CleanWizard(repository));
	}

}
