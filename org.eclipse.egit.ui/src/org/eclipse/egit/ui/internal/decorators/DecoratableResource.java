// TODO add license header
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;

/**
 * TODO
 */
class DecoratableResource implements IDecoratableResource {

	protected final IResource resource;

	protected String repositoryName = ""; //$NON-NLS-1$

	protected String branch = ""; //$NON-NLS-1$

	protected boolean tracked = false;

	protected boolean ignored = false;

	protected boolean dirty = false;

	protected Staged staged = Staged.NOT_STAGED;

	protected boolean conflicts = false;

	protected boolean assumeValid = false;

	DecoratableResource(IResource resource) {
		this.resource = resource;
	}

	public int getType() {
		return resource.getType();
	}

	public String getName() {
		return resource.getName();
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public String getBranch() {
		return branch;
	}

	public boolean isTracked() {
		return tracked;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Staged staged() {
		return staged;
	}

	public boolean hasConflicts() {
		return conflicts;
	}

	public boolean isAssumeValid() {
		return assumeValid;
	}
}
