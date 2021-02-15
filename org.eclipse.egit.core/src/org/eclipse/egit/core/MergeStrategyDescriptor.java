/*******************************************************************************
 * Copyright (c) 2015, 2021 Laurent Delaigue (Obeo) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf -- factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.jgit.merge.MergeStrategy;

/**
 * Describes a MergeStrategy which can be registered with the mergeStrategy
 * extension point.
 */
public class MergeStrategyDescriptor {
	private final String name;

	private final String label;

	private final Class<?> implementedBy;

	/**
	 * @param name
	 *            The referred strategy's name, to use for retrieving the
	 *            strategy from MergeRegistry via
	 *            {@link MergeStrategy#get(String)}
	 * @param label
	 *            The label to display to users so they can select the
	 *            strategy they need
	 * @param implementedBy
	 *            The class of the MergeStrategy registered through the
	 *            mergeStrategy extension point
	 */
	public MergeStrategyDescriptor(String name, String label,
			Class<?> implementedBy) {
		this.name = name;
		this.label = label;
		this.implementedBy = implementedBy;
	}

	/**
	 * @return The actual strategy's name, which can be used to retrieve
	 *         that actual strategy via {@link MergeStrategy#get(String)}.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The strategy label, for display purposes.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return The class of the MergeStrategy registered through the
	 *         mergeStrategy extension point.
	 */
	public Class<?> getImplementedBy() {
		return implementedBy;
	}
}