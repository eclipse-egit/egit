package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * @author lock
 *
 */
@SuppressWarnings("restriction")
public class GitChangeSetCapability extends ChangeSetCapability {

	@Override
	public boolean supportsActiveChangeSets() {
		return true;
	}

	@Override
	public boolean supportsCheckedInChangeSets() {
		return true;
	}

	@Override
	public ActiveChangeSet createChangeSet(
			ISynchronizePageConfiguration configuration, IDiff[] diffs) {
		// TODO Auto-generated method stub
		return super.createChangeSet(configuration, diffs);
	}

	@Override
	public SyncInfoSetChangeSetCollector createSyncInfoSetChangeSetCollector(
			ISynchronizePageConfiguration configuration) {
		// TODO Auto-generated method stub
		return super.createSyncInfoSetChangeSetCollector(configuration);
	}

	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		// TODO Auto-generated method stub
		return super.getActiveChangeSetManager();
	}

}
