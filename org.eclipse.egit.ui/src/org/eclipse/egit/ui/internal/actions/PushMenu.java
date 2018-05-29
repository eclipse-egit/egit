/*******************************************************************************
 * Copyright (C) 2014, Red Hat Inc. and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   - Mickael Istria (Red Hat Inc.) - 436669 Simply push workflow
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * This is the definition of the Push menu on a given node. Depending on the
 * node, it will show either "Push Branch '...'" or "Push HEAD".
 */
public class PushMenu extends CompoundContributionItem implements
		IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	private IHandlerService handlerService;

	/**	 */
	public PushMenu() {
		this(null);
	}

	/**
	 * @param id
	 */
	public PushMenu(String id) {
		super(id);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public void initialize(IServiceLocator locator) {
		this.serviceLocator = locator;
		this.handlerService = CommonUtils.getService(locator, IHandlerService.class);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> res = new ArrayList<>();

		if (this.handlerService != null) {
			Repository repository = SelectionUtils.getRepository(handlerService
					.getCurrentState());

			if (repository != null) {
				try {
					String ref = repository.getFullBranch();
					String menuLabel = UIText.PushMenu_PushHEAD;
					if (ref != null && ref.startsWith(Constants.R_HEADS)) {
						menuLabel = NLS.bind(UIText.PushMenu_PushBranch,
								Repository.shortenRefName(ref));
					}
					CommandContributionItemParameter params = new CommandContributionItemParameter(
							this.serviceLocator, getClass().getName(),
							ActionCommands.PUSH_BRANCH_ACTION,
							CommandContributionItem.STYLE_PUSH);
					params.label = menuLabel;
					CommandContributionItem item = new CommandContributionItem(
							params);
					res.add(item);
				} catch (IOException ex) {
					Activator.handleError(ex.getLocalizedMessage(), ex, false);
				}
			}
		}
		return res.toArray(new IContributionItem[res.size()]);
	}
}
