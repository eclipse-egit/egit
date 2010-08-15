/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * Git blob object representation in Git ChangeSet
 */
public class GitModelBlob extends GitModelCommit {

	private final String name;

	private final ObjectId baseId;

	private final ObjectId remoteId;

	private final ObjectId ancestorId;

	private final IPath location;

	private final String gitPath;

	private static final GitModelObject[] empty = new GitModelObject[0];

	/**
	 *
	 * @param parent
	 *            parent of this object
	 * @param commit
	 *            remote commit
	 * @param ancestorId
	 *            common ancestor id
	 * @param baseId
	 *            id of base object variant
	 * @param remoteId
	 *            id of remote object variant
	 * @param name
	 *            human readable blob name (file name)
	 * @throws IOException
	 */
	public GitModelBlob(GitModelObject parent, RevCommit commit,
			ObjectId ancestorId, ObjectId baseId, ObjectId remoteId, String name)
			throws IOException {
		super(parent, commit);
		this.name = name;
		this.baseId = baseId;
		this.remoteId = remoteId;
		this.ancestorId = ancestorId;
		location = getParent().getLocation().append(name);
		gitPath = Repository.stripWorkDir(getRepository().getWorkTree(),
				getLocation().toFile());
	}

	@Override
	public GitModelObject[] getChildren() {
		return empty;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IProject[] getProjects() {
		return getParent().getProjects();
	}

	@Override
	public IPath getLocation() {
		return location;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public ITypedElement getAncestor() {
		if (objectExist(getAncestorCommit(), ancestorId))
			return CompareUtils.getFileRevisionTypedElement(gitPath,
					getAncestorCommit(), getRepository(), ancestorId);

		return null;
	}

	@Override
	public ITypedElement getLeft() {
		return CompareUtils.getFileRevisionTypedElement(gitPath,
				getRemoteCommit(), getRepository(), remoteId);
	}

	@Override
	public ITypedElement getRight() {
			return CompareUtils.getFileRevisionTypedElement(gitPath,
					getBaseCommit(), getRepository(), baseId);

	}

	@Override
	protected String getAncestorSha1() {
		return ancestorId.getName();
	}

	@Override
	protected String getBaseSha1() {
		return baseId.getName();
	}

	@Override
	protected String getRemoteSha1() {
		return remoteId.getName();
	}

	private boolean objectExist(RevCommit commit, ObjectId id) {
		return commit != null && id != null && !id.equals(zeroId());
	}

	public void prepareInput(CompareConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		configuration.setLeftLabel(getFileRevisionLabel(getLeft()));
		configuration.setRightLabel(getFileRevisionLabel(getRight()));

	}

	private String getFileRevisionLabel(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement castElement = (FileRevisionTypedElement)element;
			return NLS.bind(UIText.GitCompareFileRevisionEditorInput_RevisionLabel,
					new Object[]{element.getName(),
					CompareUtils.truncatedRevision(castElement.getContentIdentifier()),
					castElement.getAuthor()});

		}
		else
			return element.getName();
	}

}
