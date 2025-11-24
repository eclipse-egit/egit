/*******************************************************************************
 * Copyright (C) 2025, Stephan Wahlbrink and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.fetch.FetchOperationUI;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Contributes menu items for 'simple fetch' for each remote
 */
public class SimpleFetchRemotesContributionItem extends CompoundContributionItem
		implements IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	/** */
	public SimpleFetchRemotesContributionItem() {
		super();
	}

	@Override
	public void initialize(final IServiceLocator locator) {
		this.serviceLocator = locator;
	}

	private @Nullable Repository getRepository() {
		if (this.serviceLocator != null) {
			IHandlerService handlerService = this.serviceLocator
					.getService(IHandlerService.class);
			if (handlerService != null) {
				return SelectionUtils
						.getRepository(handlerService.getCurrentState());
			}
		}
		return null;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		Repository repository = getRepository();
		if (repository != null) {
			try {
				List<IContributionItem> items = new ArrayList<>();
				var remoteConfigs = RemoteConfig
						.getAllRemoteConfigs(repository.getConfig());
				for (var remoteConfig : remoteConfigs) {
					items.add(new ActionContributionItem(
							new Action(SimpleConfigureFetchDialog
									.getSimpleFetchCommandLabel(remoteConfig)) {
								@Override
								public void run() {
									new FetchOperationUI(repository,
											remoteConfig, false).start();
								}
							}));
				}
				return items.toArray(IContributionItem[]::new);
			} catch (URISyntaxException e) { //
			}
		}
		return new IContributionItem[0];
	}

}
