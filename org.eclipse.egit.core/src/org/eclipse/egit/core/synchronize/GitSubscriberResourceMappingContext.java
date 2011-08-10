/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;

/**
 *
 */
public class GitSubscriberResourceMappingContext extends
		SubscriberResourceMappingContext {

	private final GitSynchronizeDataSet data;

	/**
	 * @param subscriber
	 * @param data
	 */
	public GitSubscriberResourceMappingContext(
			GitResourceVariantTreeSubscriber subscriber,
			GitSynchronizeDataSet data) {
		super(subscriber, true);
		this.data = data;
	}

	/**
	 * @return git synchronize data set
	 */
	public GitSynchronizeDataSet getSyncData() {
		return data;
	}

}
