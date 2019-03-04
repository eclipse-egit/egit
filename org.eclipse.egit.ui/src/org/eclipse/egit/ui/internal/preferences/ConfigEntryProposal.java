/*******************************************************************************
 * Copyright (C) 2019, Michael Keppler <michael.keppler@gmx.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.internal.UIText;

class ConfigEntryProposal {
	private final String key;

	private final String description;

	private final List<String> values;

	public ConfigEntryProposal(String key, String description,
			List<String> values) {
		this.key = key;
		this.description = description;
		this.values = values;
	}

	public static Map<String, ConfigEntryProposal> createAllProposals() {
		List<ConfigEntryProposal> proposals = Arrays.asList(
				new ConfigEntryProposal("branch.autosetuprebase", //$NON-NLS-1$
						UIText.ConfigEntryProposal_autosetuprebase,
						Arrays.asList("always", "never", "local", "remote")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				new ConfigEntryProposal("user.email", //$NON-NLS-1$
						UIText.ConfigEntryProposal_user_email,
						Collections.emptyList()),
				new ConfigEntryProposal("user.name", //$NON-NLS-1$
						UIText.ConfigEntryProposal_user_name,
						Collections.emptyList()),
				new ConfigEntryProposal("core.autocrlf", //$NON-NLS-1$
						UIText.ConfigEntryProposal_core_autocrlf,
						Arrays.asList("true", "input")), //$NON-NLS-1$//$NON-NLS-2$
				new ConfigEntryProposal("core.longpaths", //$NON-NLS-1$
						UIText.ConfigEntryProposal_core_longpaths,
						Arrays.asList("true")), //$NON-NLS-1$
				new ConfigEntryProposal("fetch.prune", //$NON-NLS-1$
						UIText.ConfigEntryProposal_fetch_prune,
						Arrays.asList("true"))); //$NON-NLS-1$
		return proposals.stream()
				.collect(Collectors.toMap(
						ConfigEntryProposal::getKey, Function.identity()));
	}

	String getKey() {
		return key;
	}

	String getDescription() {
		return description;
	}

	List<String> getValues() {
		return values;
	}
}
