/*******************************************************************************
 * Copyright (C) 2012, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class LocationPage {
	private SWTBotShell shell;

	public LocationPage(SWTBotShell shell) {
		this.shell = shell;
	}

	void selectClipboard() {
		shell.bot().radio(UIText.GitCreatePatchWizard_Clipboard).click();
	}

	void selectFilesystem(final String path) {
		shell.bot().radio(UIText.GitCreatePatchWizard_File).click();
		shell.bot().text(0).setText(path);
	}

	void selectWorkspace(final String path) {
		shell.bot().radio(UIText.GitCreatePatchWizard_Workspace).click();
		shell.bot().text(1).setText(path);
	}

	public OptionsPage nextToOptionsPage() {
		shell.bot().button("Next >").click();
		return new OptionsPage(shell);
	}
}
