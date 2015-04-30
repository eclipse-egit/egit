package org.eclipse.egit.gitflow.ui.internal.factories;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;

/**
 *
 */
@SuppressWarnings("restriction")
public class GitFlowAdapterFactory implements IAdapterFactory {
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		Repository repository = null;
		if (adaptableObject instanceof IResource) {
			IResource resource = (IResource) adaptableObject;
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(resource.getProject());
			repository = repositoryMapping.getRepository();
		} else if (adaptableObject instanceof PlatformObject) {
			PlatformObject platformObject = (PlatformObject) adaptableObject;
			repository = (Repository) platformObject.getAdapter(Repository.class);
		} else {
			throw new IllegalStateException();
		}

		return repository;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IResource.class, RepositoryTreeNode.class };
	}

}
