package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;

/**
 * Implementation of a Synchronization Strategy that is aware of model
 * providers. This implementation consults the model providers to synchronize
 * not only selected files but their whole logical models, as provided by
 * registered model providers.
 */
public class ModelAwareSyncStrategy extends SyncStrategy {

	/** The cached {@link ResourceMappingContext} used by this strategy. */
	private ResourceMappingContext context;

	/**
	 * @param resources
	 * @param repository
	 * @param leftRev
	 * @param rightRev
	 * @param includeLocal
	 */
	public ModelAwareSyncStrategy(IResource[] resources,
			@NonNull Repository repository, String leftRev, String rightRev,
			boolean includeLocal) {
		super(resources, repository, leftRev, rightRev, includeLocal);
	}

	/**
	 * This can be used to open the synchronize view for the given set of
	 * resources, using the whole logical models the resources are part of, and
	 * comparing the given revisions together.
	 * <p>
	 * Note that this falls back to the git tree compare view if the destination
	 * revision is the index.
	 * </p>
	 *
	 * @throws IOException
	 */
	@Override
	public void synchronize() throws IOException {
		final Set<IResource> includedResources = new HashSet<>(
				Arrays.asList(resources));
		final Set<ResourceMapping> allMappings = new HashSet<>();

		Set<IResource> newResources = new HashSet<>(includedResources);
		do {
			final Set<IResource> copy = newResources;
			newResources = new HashSet<>();
			for (IResource resource : copy) {
				Assert.isNotNull(resource);
				ResourceMapping[] mappings = ResourceUtil
						.getResourceMappings(resource,
								getResourceMappingContext());
				allMappings.addAll(Arrays.asList(mappings));
				newResources.addAll(collectResources(mappings));
			}
		} while (includedResources.addAll(newResources));

		if (rightRev.equals(GitFileRevision.INDEX)) {
			final IResource[] resourcesArray = includedResources
					.toArray(new IResource[includedResources.size()]);
			GitModelSynchronize.openGitTreeCompare(resourcesArray, leftRev,
					CompareTreeView.INDEX_VERSION, includeLocal);
		} else if (leftRev.equals(GitFileRevision.INDEX)) {
			// Even git tree compare cannot handle index as source...
			// Synchronize using the local data for now.
			final ResourceMapping[] mappings = allMappings
					.toArray(new ResourceMapping[allMappings.size()]);
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					leftRev, rightRev, true, includedResources);
			GitModelSynchronize.launch(new GitSynchronizeDataSet(data),
					mappings);
		} else {
			final ResourceMapping[] mappings = allMappings
					.toArray(new ResourceMapping[allMappings.size()]);
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					leftRev, rightRev, includeLocal, includedResources);
			GitModelSynchronize.launch(new GitSynchronizeDataSet(data),
					mappings);
		}
	}

	@Override
	protected boolean canCompareDirectly() {
		if (resources.length == 1) {
			IResource resource = resources[0];
			if (resource instanceof IFile) {
				return canCompareDirectly((IFile) resource);
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

	/**
	 * The model providers need information about the remote sides to properly
	 * detect whether a given file is part of a logical model or not. This will
	 * prepare the RemoteResourceMappingContext corresponding to the given
	 * source branch ("ours" side of the comparison, {@code leftRev} or the work
	 * tree, depending on the state of {@code inclueLocal}) and the given
	 * destination branch ("theirs" side, {@code rightRev}). The common ancestor
	 * ("base" side) for this comparison will be inferred as the first common
	 * ancestor of {@code leftRev} and {@code rightRev}.
	 *
	 * @return a {@link RemoteResourceMappingContext} ready for use by the model
	 *         providers.
	 */
	private ResourceMappingContext getResourceMappingContext() {
		if (context == null) {
			try {
				GitSynchronizeData gsd = new GitSynchronizeData(repository,
						leftRev, rightRev, includeLocal);
				GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
				GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
						gsds);
				subscriber.init(new NullProgressMonitor());

				context = new GitSubscriberResourceMappingContext(subscriber,
						gsds);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
				context = ResourceMappingContext.LOCAL_CONTEXT;
			}
		}
		return context;
	}

	/**
	 * Provide all the resources to take into account for the given mappings.
	 *
	 * @param mappings
	 *            The mappings for which we seek the resources involved
	 * @return The set of all the resources involved in the given mappings
	 */
	private Set<IResource> collectResources(ResourceMapping[] mappings) {
		final Set<IResource> result = new HashSet<>();
		for (ResourceMapping mapping : mappings) {
			try {
				ResourceTraversal[] traversals = mapping.getTraversals(
						getResourceMappingContext(), new NullProgressMonitor());
				for (ResourceTraversal traversal : traversals)
					result.addAll(Arrays.asList(traversal.getResources()));
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * Indicates if it is OK to open the selected file directly in a compare
	 * editor.
	 * <p>
	 * It is not OK to show the single file if the file is part of a logical
	 * model element that spans multiple files.
	 * </p>
	 *
	 * @param file
	 *            file the user is trying to compare
	 * @return <code>true</code> if the file can be opened directly in a compare
	 *         editor, <code>false</code> if the synchronize view should be
	 *         opened instead.
	 */
	private boolean canCompareDirectly(@NonNull IFile file) {
		// Using a local context for the ResourceMapping computation would make
		// for a faster test... but we need the model providers to be able to
		// load remote information. The local file may very well be a single
		// file, but it is possible that the remote side has multiple files to
		// take into account for that model. (if part of the logical model has
		// been locally deleted, or if some new files have been created on the
		// remote side(s).)
		//
		// Only builds the logical model if the preference holds true
		final ResourceMapping[] mappings = ResourceUtil
				.getResourceMappings(file, getResourceMappingContext());

		for (ResourceMapping mapping : mappings) {
			try {
				final ResourceTraversal[] traversals = mapping
						.getTraversals(getResourceMappingContext(), null);
				for (ResourceTraversal traversal : traversals) {
					final IResource[] traversalResources = traversal
							.getResources();
					if (traversalResources.length > 1 && Arrays
							.asList(traversalResources).contains(file)) {
						return false;
					}
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return true;
	}
}
