package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Default Synchronization Strategy, which does not take logical models into
 * account.
 */
public class SyncStrategy {
	/**
	 * The resources to compare. Can be empty (in which case we'll synchronize
	 * the whole repository).
	 */
	protected final IResource[] resources;

	/** The repository to load file revisions from. */
	protected final @NonNull Repository repository;

	/**
	 * Left revision of the comparison (usually the local or "new" revision).
	 * Won't be used if <code>includeLocal</code> is <code>true</code>.
	 */
	protected final String leftRev;

	/** Right revision of the comparison (usually the "old" revision). */
	protected final String rightRev;

	/**
	 * If <code>true</code>, this will use the local data as the "left" side of
	 * the comparison.
	 */
	protected final boolean includeLocal;

	/**
	 * @param resources
	 *            The resources to compare. Can be empty (in which case we'll
	 *            synchronize the whole repository).
	 * @param repository
	 *            The repository to load file revisions from.
	 * @param leftRev
	 *            Left revision of the comparison (usually the local or "new"
	 *            revision). Won't be used if <code>includeLocal</code> is
	 *            <code>true</code>.
	 * @param rightRev
	 *            Right revision of the comparison (usually the "old" revision).
	 * @param includeLocal
	 *            If <code>true</code>, this will use the local data as the
	 *            "left" side of the comparison and {@code leftRev} will be
	 *            ignored.
	 */
	public SyncStrategy(IResource[] resources, @NonNull Repository repository,
			String leftRev, String rightRev, boolean includeLocal) {
		this.resources = resources;
		this.repository = repository;
		this.leftRev = leftRev;
		this.rightRev = rightRev;
		this.includeLocal = includeLocal;
	}

	/**
	 * This can be used to compare a given set of resources between two
	 * revisions. If only one resource is to be compared, we'll open a
	 * comparison editor for that file alone. Otherwise, we'll launch a
	 * synchronization restrained to the given resources.
	 * <p>
	 * This can also be used to synchronize the whole repository if
	 * <code>resources</code> is empty.
	 * </p>
	 * <p>
	 * Note that this can be used to compare with the index by using
	 * {@link GitFileRevision#INDEX} as either one of the two revs.
	 * </p>
	 *
	 * @param page
	 *            If not {@null} try to re-use a compare editor on this page if
	 *            any is available. Otherwise open a new one.
	 * @throws IOException
	 */
	public void compare(IWorkbenchPage page) throws IOException {
		if (canCompareDirectly()) {
			if (includeLocal) {
				CompareUtils.compareWorkspaceWithRef(repository, resources[0],
						rightRev, page);
			} else {
				final IResource file = resources[0];
				Assert.isNotNull(file);
				final RepositoryMapping mapping = RepositoryMapping
						.getMapping(file);
				if (mapping == null) {
					Activator.error(
							NLS.bind(UIText.GitHistoryPage_errorLookingUpPath,
									file.getLocation(), repository),
							null);
					return;
				}
				final String gitPath = mapping.getRepoRelativePath(file);

				CompareUtils.compareBetween(repository, gitPath, leftRev,
						rightRev, page);
			}
		} else {
			synchronize();
		}
	}

	/**
	 * This can be used to compare a given set of resources between two
	 * revisions. If only one resource is to be compared, we'll open a
	 * comparison editor for that file alone, also taking leftPath and rightPath
	 * into account. Otherwise, we'll launch a synchronization restrained to the
	 * given resources.
	 * <p>
	 * This can also be used to synchronize the whole repository if
	 * <code>resources</code> is empty.
	 * </p>
	 * <p>
	 * Note that this can be used to compare with the index by using
	 * {@link GitFileRevision#INDEX} as either one of the two revs.
	 * </p>
	 *
	 * @param leftPath
	 *            The repository relative path to be used for the left revision,
	 *            when comparing directly.
	 * @param rightPath
	 *            The repository relative path to be used for the right
	 *            revision, when comparing directly.
	 * @param page
	 *            If not {@null} try to re-use a compare editor on this page if
	 *            any is available. Otherwise open a new one.
	 * @throws IOException
	 */
	public void compare(String leftPath, String rightPath, IWorkbenchPage page)
			throws IOException {
		if (canCompareDirectly()) {
			if (includeLocal) {
				CompareUtils.compareWorkspaceWithRef(repository, resources[0],
						rightRev, page);
			} else {
				CompareUtils.compareBetween(repository, leftPath, rightPath,
						leftRev, rightRev, page);
			}
		} else {
			synchronize();
		}
	}

	/**
	 * This can be used to open the synchronize view for the given set of
	 * resources, comparing the given revisions together.
	 * <p>
	 * Note that this falls back to the git tree compare view if the destination
	 * revision is the index.
	 * </p>
	 *
	 * @throws IOException
	 */
	public void synchronize() throws IOException {
		if (rightRev.equals(GitFileRevision.INDEX)) {
			GitModelSynchronize.openGitTreeCompare(resources, leftRev,
					CompareTreeView.INDEX_VERSION, includeLocal);
		} else if (leftRev.equals(GitFileRevision.INDEX)) {
			// Even git tree compare cannot handle index as source...
			// Synchronize using the local data for now.
			Set<IResource> resSet = new HashSet<>(Arrays.asList(resources));
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					leftRev, rightRev, true, resSet);
			GitModelSynchronize.launch(new GitSynchronizeDataSet(data),
					resources);
		} else {
			Set<IResource> resSet = new HashSet<>(Arrays.asList(resources));
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					leftRev, rightRev, includeLocal, resSet);
			GitModelSynchronize.launch(new GitSynchronizeDataSet(data),
					resources);
		}
	}

	/**
	 * Indicate whether the comparison can be opened directly (by default, this
	 * is <code>true</code> if it concerns only one file or one symbolic link).
	 *
	 * @return <code>true</code> if the synchronization can directly open a
	 *         comparison.
	 */
	protected boolean canCompareDirectly() {
		if (resources.length == 1) {
			IResource resource = resources[0];
			if (resource instanceof IFile) {
				return true;
			} else {
				IPath location = resource.getLocation();
				if (location != null
						&& Files.isSymbolicLink(location.toFile().toPath())) {
					// for directory *link*, file compare must be used
					return true;
				}
			}
		}
		return false;
	}
}
