/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.history;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.ui.internal.history.GitHistoryPageSource;
import org.eclipse.team.ui.history.IHistoryPageSource;

/**
 * An {@link IAdapterFactory} that adapts any non-{@code null} object to an
 * {@link IHistoryPageSource} for the git history page.
 *
 * @since 5.7
 */
public class GitHistoryAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject != null
				&& adapterType == IHistoryPageSource.class) {
			return adapterType.cast(GitHistoryPageSource.INSTANCE);
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IHistoryPageSource.class };
	}
}
