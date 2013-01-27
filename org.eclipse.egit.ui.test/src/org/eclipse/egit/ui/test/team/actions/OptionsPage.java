/*******************************************************************************
 * Copyright (C) 2012, IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
