package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * An adapter factory for <code>GitXStagedNode</code>s so that the property page
 * handler can open property pages on them correctly.
 */
public class GitXStagedNodeAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		return ((GitXStagedNode)adaptableObject).getAdapter(IResource.class);
	}

	public Class[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
