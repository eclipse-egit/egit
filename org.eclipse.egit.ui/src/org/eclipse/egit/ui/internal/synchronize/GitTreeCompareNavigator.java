/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.INavigatable;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.JobFamilies;

/**
 * Wraps given {@link CompareNavigator} and waits for add to index, remove from
 * index and repository change jobs finish before call
 * {@link CompareNavigator#selectChange(boolean)} from main navigator
 */
class GitTreeCompareNavigator extends CompareNavigator {

	private static final Class<CompareNavigator> COMPARE_NAVIGATOR_CLASS = CompareNavigator.class;

	private final CompareNavigator mainNavigator;

	public GitTreeCompareNavigator(CompareNavigator mainNavigator) {
		this.mainNavigator = mainNavigator;
	}

	@Override
	protected INavigatable[] getNavigatables() {
		Method baseNavigables;
		try {
			baseNavigables = COMPARE_NAVIGATOR_CLASS.getDeclaredMethod(
					"getNavigatables", Void.class); //$NON-NLS-1$
			baseNavigables.setAccessible(true);
			return (INavigatable[]) baseNavigables.invoke(mainNavigator,
					Void.class);
		} catch (SecurityException e) {
			// should never happen
		} catch (NoSuchMethodException e) {
			// should never happen
		} catch (IllegalArgumentException e) {
			// should never happen
		} catch (IllegalAccessException e) {
			// should never happen
		} catch (InvocationTargetException e) {
			// should never happen
		}
		return new INavigatable[0];
	}

	@Override
	public boolean selectChange(boolean next) {
		// wait for repositories actions
		IJobManager manager = Job.getJobManager();
		try {
			manager.join(JobFamilies.ADD_TO_INDEX, null);
			manager.join(JobFamilies.REMOVE_FROM_INDEX, null);
			manager.join(org.eclipse.egit.core.JobFamilies.REPOSITORY_CHANGED, null);
		} catch (InterruptedException e) {
			// ignore
		}

		return mainNavigator.selectChange(next);
	}

}
