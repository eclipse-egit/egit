/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.egit.ui.UIIcons.EXPAND_ALL;
import static org.eclipse.egit.ui.UIText.GitActionContributor_ExpandAll;
import static org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration.NAVIGATE_GROUP;
import static org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration.P_TOOLBAR_MENU;

import org.eclipse.egit.ui.internal.synchronize.action.ExpandAllModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;

class GitActionContributor extends ModelSynchronizeParticipantActionGroup {

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		ExpandAllModelAction expandAllAction = new ExpandAllModelAction(
				GitActionContributor_ExpandAll, configuration);
		expandAllAction.setImageDescriptor(EXPAND_ALL);
		appendToGroup(P_TOOLBAR_MENU, NAVIGATE_GROUP, expandAllAction);
	}

}
