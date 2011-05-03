package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * An adapter factory for <code>StagingEntry</code>s so that the property page
 * handler can open property pages on them correctly.
 */
public class StagingEntryAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		return ((StagingEntry)adaptableObject).getAdapter(IResource.class);
	}

	public Class[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
