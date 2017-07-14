/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.expressions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A {@link PropertyTester} to test some properties related to commits and
 * commit histories. Offers the following property tests:
 * <dl>
 * <dt>(IRepositoryCommit|RevCommit).parentCount</dt>
 * <dd>Evaluates to the number of parents of the commit. If an expected
 * <code>value</code> is given and is an integer value, the test is
 * <code>true</code> if the number of parents equals the given value. Otherwise
 * the test is <code>true</code> if the number of parents is &gt; 0.</dd>
 * <dt>(IRepositoryCommit|Repository).isBare</dt>
 * <dd>Evaluates to <code>true</code> if the repository is not a bare
 * repository.</dd>
 * <dt>(IRepositoryCommit|Repository).isSafe</dt>
 * <dd>Evaluates to <code>true</code> if the repository is in a
 * {@link RepositoryState#SAFE SAFE} state.</dd>
 * <dt>(IRepositoryCommit|Repository).canCommit</dt>
 * <dd>Evaluates to <code>true</code> if the repository is in a state where we
 * can create a new commit.</dd>
 * <dt>IRepositoryCommit.hasRef [args="&lt;ref prefixes&gt;"]</dt>
 * <dd>Tests whether there are any refs starting with one of the given prefixes
 * point to the given commit. If no argument is given, or no
 * <code>&lt;ref_prefix&gt;</code> starts with "refs/", a default of
 * "refs/heads/" is assumed, i.e., the test is <code>true</code> if there is any
 * local branch pointing to the commit.</dd>
 * <dt>IRepositoryCommit.hasMultipleRefs [args="&lt;ref prefixes&gt;"]</dt>
 * <dd>Like <code>hasRef</code>, but evaluates to <code>true</code> only if
 * there are more than one ref pointing to the commit.</dd>
 * </dl>
 * <p>
 * The <code>hasRef</code> and <code>hasMultipleRefs</code> tests may be
 * expensive if the {@link RevCommit} of the {@link IRepositoryCommit} is
 * <em>not</em> a {@link PlotCommit} since they then must read the refs from the
 * repository.
 * </p>
 */
public class GitPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if ("parentCount".equals(property)) { //$NON-NLS-1$
			RevCommit commit = AdapterUtils.adapt(receiver, RevCommit.class);
			if (commit == null) {
				return false;
			}
			if (expectedValue instanceof Integer) {
				return commit.getParentCount() <= ((Integer) expectedValue)
						.intValue();
			} else {
				return computeResult(expectedValue,
						commit.getParentCount() > 0);
			}
		} else if ("isBare".equals(property)) { //$NON-NLS-1$
			Repository repository = AdapterUtils.adapt(receiver,
					Repository.class);
			if (repository != null) {
				return computeResult(expectedValue, repository.isBare());
			}
		} else if ("isSafe".equals(property)) { //$NON-NLS-1$
			Repository repository = AdapterUtils.adapt(receiver,
					Repository.class);
			if (repository != null) {
				return computeResult(expectedValue, repository
						.getRepositoryState().equals(RepositoryState.SAFE));
			}
		} else if ("canCommit".equals(property)) { //$NON-NLS-1$
			Repository repository = AdapterUtils.adapt(receiver,
					Repository.class);
			if (repository != null) {
				return computeResult(expectedValue,
						repository.getRepositoryState().canCommit());
			}
		} else if ("hasMultipleRefs".equals(property)) { //$NON-NLS-1$
			IRepositoryCommit commit = AdapterUtils.adapt(receiver,
					IRepositoryCommit.class);
			if (commit != null) {
				return computeResult(expectedValue,
						hasMultipleRefs(commit, toRefNames(args)));
			}
		} else if ("hasRef".equals(property)) { //$NON-NLS-1$
			IRepositoryCommit commit = AdapterUtils.adapt(receiver,
					IRepositoryCommit.class);
			if (commit != null) {
				return computeResult(expectedValue,
						hasRef(commit, toRefNames(args)));
			}
		}
		return false;
	}

	private Collection<String> toRefNames(Object[] args) {
		List<String> names = new ArrayList<>(2);
		if (args != null) {
			for (Object arg : args) {
				String name = arg.toString();
				if (name.startsWith(Constants.R_REFS)) {
					names.add(name);
				}
			}
		}
		if (names.isEmpty()) {
			names.add(Constants.R_HEADS);
		}
		return names;
	}

	private boolean hasMultipleRefs(IRepositoryCommit commit,
			Collection<String> names) {
		Repository repository = commit.getRepository();
		if (repository == null) {
			return false;
		}
		int count = 0;
		RevCommit revCommit = commit.getRevCommit();
		if (revCommit instanceof PlotCommit) {
			int n = ((PlotCommit) revCommit).getRefCount();
			for (int i = 0; i < n; i++) {
				Ref ref = ((PlotCommit) revCommit).getRef(i);
				for (String name : names) {
					if (ref.getName().startsWith(name)) {
						if (++count > 1) {
							break;
						}
					}
				}
				if (count > 1) {
					return true;
				}
			}
		} else {
			try {
				ObjectId selectedId = commit.getRevCommit().getId();
				for (String name : names) {
					for (Ref branch : repository.getRefDatabase().getRefs(name)
							.values()) {
						ObjectId objectId = branch.getLeaf().getObjectId();
						if (objectId != null && objectId.equals(selectedId)) {
							if (++count > 1) {
								return true;
							}
						}
					}
				}
			} catch (IOException e) {
				// ignore here
			}
		}
		return false;
	}

	private boolean hasRef(IRepositoryCommit commit, Collection<String> names) {
		Repository repository = commit.getRepository();
		if (repository == null) {
			return false;
		}
		RevCommit revCommit = commit.getRevCommit();
		if (revCommit instanceof PlotCommit) {
			int n = ((PlotCommit) revCommit).getRefCount();
			for (int i = 0; i < n; i++) {
				Ref ref = ((PlotCommit) revCommit).getRef(i);
				for (String name : names) {
					if (ref.getName().startsWith(name)) {
						return true;
					}
				}
			}
		} else {
			try {
				ObjectId selectedId = commit.getRevCommit().getId();
				for (String name : names) {
					for (Ref branch : repository.getRefDatabase().getRefs(name)
							.values()) {
						ObjectId objectId = branch.getLeaf().getObjectId();
						if (objectId != null && objectId.equals(selectedId)) {
							return true;
						}
					}
				}
			} catch (IOException e) {
				// ignore here
			}
		}
		return false;
	}

}
