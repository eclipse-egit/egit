/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REFS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Simple entity for remote and local repositories containing only repo name and
 * list of refs associated with it.
 */
public class SyncRepoEntity {

	/**
	 * Simple entity for refs containing only human readable ref name and git
	 * ref path
	 */
	public static class SyncRefEntity {
		private final String descr;

		private final String value;

		/**
		 * @param descr
		 *            human readable description of repository
		 * @param value
		 *            value that will be associated with this repo eg. HEAD,
		 *            refs/heads/master, etc
		 */
		public SyncRefEntity(String descr, String value) {
			this.descr = descr;
			this.value = value;
		}

		/**
		 * @return human readable description of ref
		 */
		public String getDescription() {
			return descr;
		}

		/**
		 * @return value that is associated with this ref eg. HEAD,
		 *         refs/heads/master, etc.
		 */
		public String getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + descr.hashCode();
			result = prime * result + value.hashCode();

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;

			if (obj == null)
				return false;

			if (getClass() != obj.getClass())
				return false;

			SyncRefEntity other = (SyncRefEntity) obj;
			if (!descr.equals(other.descr))
				return false;

			if (!value.equals(other.value))
				return false;

			return true;
		}

		@Override
		public String toString() {
			return "SyncRepoEntity:" + descr + "=>" + value; //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	static final Pattern PATTERN = Pattern.compile("^(" //$NON-NLS-1$
			+ R_HEADS + ")|(" //$NON-NLS-1$
			+ R_REMOTES + ")|(" //$NON-NLS-1$
			+ R_TAGS + ")|(" //$NON-NLS-1$
			+ R_REFS + ")"); //$NON-NLS-1$

	/**
	 *
	 * @param repo
	 * @return all refs in repository
	 */
	public static SyncRepoEntity getAllRepoEntities(Repository repo) {
		SyncRepoEntity sre = new SyncRepoEntity(repo.getWorkTree().getName());
		sre.setToolTip(repo.getDirectory().getAbsolutePath());
		for (String refKey : repo.getAllRefs().keySet())
			sre.addRef(createSyncRepoEntity(refKey));

		return sre;
	}

	/**
	 *
	 * @param refDatabase
	 * @param rc
	 * @return remote ref specs
	 * @throws IOException
	 */
	public static SyncRepoEntity getRemoteSyncRepo(RefDatabase refDatabase,
			RemoteConfig rc) throws IOException {
		String name = rc.getName();
		SyncRepoEntity syncRepoEnt = new SyncRepoEntity(name);
		Collection<Ref> remoteRefs = refDatabase
				.getRefs(R_REMOTES + name + "/").values(); //$NON-NLS-1$

		for (Ref ref : remoteRefs)
			syncRepoEnt.addRef(createSyncRepoEntity(name, ref.getName()));

		return syncRepoEnt;
	}

	/**
	 *
	 * @param repo
	 * @return tags ref spec
	 */
	public static SyncRepoEntity getTagsSyncRepo(Repository repo) {
		Set<String> allRefs = repo.getAllRefs().keySet();
		SyncRepoEntity local = new SyncRepoEntity(
				UIText.SynchronizeWithAction_tagsName);
		for (String ref : allRefs)
			if (ref.startsWith(Constants.R_TAGS))
				local.addRef(createSyncRepoEntity(ref));

		return local;
	}

	/**
	 *
	 * @param ref
	 * @return ref spec for repository
	 */
	public static SyncRefEntity createSyncRepoEntity(String ref) {
		return createSyncRepoEntity("", ref); //$NON-NLS-1$
	}

	private static SyncRefEntity createSyncRepoEntity(String repoName, String ref) {
		String name = PATTERN.matcher(ref).replaceFirst(""); //$NON-NLS-1$

		if (name.startsWith(repoName + "/")) //$NON-NLS-1$
			name = name.substring(repoName.length() + 1);

		return new SyncRefEntity(name, ref);
	}

	private final String name;

	private final List<SyncRefEntity> refs;

	private String toolTip;

	/**
	 * @param name
	 *            of repository eg. local, origin, etc.
	 */
	public SyncRepoEntity(String name) {
		this.name = name;
		refs = new ArrayList<SyncRefEntity>();
	}

	/**
	 * @return name of repository
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param ref
	 *            that will be added to this repository
	 */
	public void addRef(SyncRefEntity ref) {
		refs.add(ref);
	}

	/**
	 *
	 * @return list of refs associated with this repository
	 */
	public List<SyncRefEntity> getRefList() {
		return Collections.unmodifiableList(refs);
	}

	/**
	 *
	 * @return tool tip
	 */
	public String getToolTip() {
		return toolTip;
	}

	/**
	 * Sets tool tip value for this entity
	 * @param toolTip
	 */
	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		SyncRepoEntity other = (SyncRepoEntity) obj;
		if (!name.equals(other.name))
			return false;

		if (!refs.equals(other.refs))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + refs.hashCode();

		return result;
	}

}
