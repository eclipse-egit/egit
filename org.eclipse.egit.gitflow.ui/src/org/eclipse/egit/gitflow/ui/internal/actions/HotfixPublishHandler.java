/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.egit.gitflow.ui.internal.UIText;

/**
 * git flow hotfix finish
 */
public class HotfixPublishHandler extends AbstractPublishHandler {
	@Override
	protected String getProgressText() {
		return UIText.HotfixPublishHandler_publishingHotfix;
	}
}
