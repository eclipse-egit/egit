package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * An adapter factory for <code>StagedNode</code>s so that the property page
 * handler can open property pages on them correctly.
 */
public class StagedNodeAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		return ((StagedNode)adaptableObject).getAdapter(IResource.class);
	}

	public Class[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
