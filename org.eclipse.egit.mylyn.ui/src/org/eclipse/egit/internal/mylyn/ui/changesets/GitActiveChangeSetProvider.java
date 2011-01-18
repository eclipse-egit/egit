/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *******************************************************************************/

package org.eclipse.egit.internal.mylyn.ui.changesets;

import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.mylyn.internal.team.ui.ContextChangeSet;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.team.ui.AbstractActiveChangeSetProvider;
import org.eclipse.mylyn.team.ui.IContextChangeSet;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetManager;

public class GitActiveChangeSetProvider extends AbstractActiveChangeSetProvider {

	private GitResourceVariantTreeSubscriber subscriberInstance;
	private ActiveChangeSetManager changeSetManager;
	
	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		if (changeSetManager == null) {
			changeSetManager = new SubscriberChangeSetManager(getSubscriber()) {

				protected ActiveChangeSet doCreateSet(String name) {
					return new ActiveChangeSet(this, name);
				}

			};
		}
		return changeSetManager;
	}

	@Override
	public IContextChangeSet createChangeSet(ITask task) {
		return new ContextChangeSet(task, changeSetManager);
	}
	
	private Subscriber getSubscriber() {
		if (subscriberInstance == null) {
			subscriberInstance = new GitResourceVariantTreeSubscriber(
					new GitSynchronizeDataSet());
		}
		return subscriberInstance;
	}

}