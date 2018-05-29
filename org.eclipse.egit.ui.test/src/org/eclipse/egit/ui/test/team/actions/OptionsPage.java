/*******************************************************************************
 * Copyright (C) 2012, IBM Corporation and others.
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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class OptionsPage {

	private SWTBotShell shell;

	public OptionsPage(SWTBotShell shell) {
		this.shell = shell;
	}

	private SWTBotCombo getFormatCombo() {
		return shell.bot().comboBoxWithLabel(UIText.GitCreatePatchWizard_Format);
	}

	public void setFormat(final String selection) {
		getFormatCombo().setSelection(selection);
	}
}
