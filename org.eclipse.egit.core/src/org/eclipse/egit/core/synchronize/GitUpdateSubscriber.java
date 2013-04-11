package org.eclipse.egit.core.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;

/**
 * This class is an update subscriber which can be used to create an
 * {@link ActiveChangeSetManager}
 */
public class GitUpdateSubscriber extends GitResourceVariantTreeSubscriber {

	private static GitUpdateSubscriber instance = null;

	private final IndexDiffChangedListener indexChangeListener;

	/**
	 * @return GitUpdateSubscriber
	 */
	public static synchronized GitUpdateSubscriber instance() {
		if (GitUpdateSubscriber.instance == null)
			GitUpdateSubscriber.instance = createUpdateSubscriber();
		return GitUpdateSubscriber.instance;
	}

	/**
	 * Creates an update subscriber with empty synchronize data
	 * 
	 * @return an update subscriber
	 */
	private static GitUpdateSubscriber createUpdateSubscriber() {
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		GitUpdateSubscriber updateSubscriber = new GitUpdateSubscriber(set);
		updateSubscriber.init(new NullProgressMonitor());
		return updateSubscriber;
	}

	/**
	 * @param data
	 */
	private GitUpdateSubscriber(GitSynchronizeDataSet data) {
		super(data);
		indexChangeListener = new IndexDiffChangedListener() {
			public void indexDiffChanged(Repository repository,
					IndexDiffData indexDiffData) {
				handleRepositoryChange(repository, indexDiffData);
			}
		};

		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		if (indexDiffCache != null)
			indexDiffCache.addIndexDiffChangedListener(indexChangeListener);
	}

	@Override
	public IResource[] roots() {
		roots = gsds.getAllProjects();
		IResource[] result = new IResource[roots.length];
		System.arraycopy(roots, 0, result, 0, roots.length);
		return result;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		// Refresh the cache for the local resource
		refresh(new IResource[] { local }, IResource.DEPTH_ZERO,
				new NullProgressMonitor());
		Repository repo = gsds.getData(local.getProject()).getRepository();
		SyncInfo info = new GitSyncInfo(local, base, remote,
				getResourceComparator(), cache.get(repo), repo);

		info.init();
		return info;
	}

	/**
	 * Looks for a new Git project which isn't in the cache yet, and adds it
	 * into the synchronize data
	 * 
	 * @param repo
	 * @param indexDiffData
	 */
	private void handleRepositoryChange(Repository repo,
			IndexDiffData indexDiffData) {
		Set<IProject> projects = new HashSet<IProject>();
		if (indexDiffData.getChangedResources() != null) {
			for (IResource resource : indexDiffData.getChangedResources()) {
				IProject project = resource.getProject();
				if (!projects.contains(project) && !gsds.contains(project)) {
					try {
						gsds.add(new GitSynchronizeData(repo, Constants.HEAD,
								Constants.HEAD, true));
						refresh(new IResource[] { project },
								IResource.DEPTH_INFINITE,
								new NullProgressMonitor());
						projects.add(project);
					} catch (Exception e) {
						// do nothing
					}
				}
			}
		}
	}
}
