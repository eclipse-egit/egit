/*******************************************************************************
 * Copyright (C) 2010, 2022 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushConfig;
import org.eclipse.jgit.transport.PushConfig.PushDefault;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Handler for "Push to Upstream". The handler displays the configured remote
 * name.
 */
public class SimplePushActionHandler extends RepositoryActionHandler
		implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}
		try {
			PushOperationUI.pushToUpstream(getShell(event), repository);
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemoteCached(repository);
		if (config == null) {
			return false;
		}
		List<RefSpec> refSpecs = config.getPushRefSpecs();
		if (!refSpecs.isEmpty()) {
			// It's too expensive to determine if anything would match
			return true;
		}
		PushDefault pushDefault = SelectionRepositoryStateCache.INSTANCE
				.getConfig(repository).get(PushConfig::new).getPushDefault();
		return !PushDefault.NOTHING.equals(pushDefault);
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemoteCached(getRepository());
		if (config != null) {
			element.setText(SimpleConfigurePushDialog
					.getSimplePushCommandLabel(config));
		}
	}

}
