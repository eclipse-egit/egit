/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	}

	private final String name;

	private final List<SyncRefEntity> refs;

	/**
	 * @param name
	 *            of repository eg. local, origin, etc.
	 */
	public SyncRepoEntity(String name) {
		this.name = name;
		refs = new ArrayList<>();
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

}
