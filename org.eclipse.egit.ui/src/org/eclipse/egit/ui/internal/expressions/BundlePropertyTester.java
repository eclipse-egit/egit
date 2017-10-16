/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * A {@link PropertyTester} to test some properties related to bundles. Offers
 * the following property tests:
 * <dl>
 * <dt>isBundleInstalled
 * args="bundleName[,minimumVersionInclusive[,maximumVersionExclusive]]"</dt>
 * <dd>Like the org.eclipse.core.runtime.isBundleInstalled property test, but
 * additionally allows to check for a version range. Specify 0.0.0 for the
 * minimum version if you only want to test for < maximumVersionExclusive, and
 * omit the maximum version if you only want to test >=
 * minimumVersionInclusive.</dd>
 * </dl>
 */
public class BundlePropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if ("isBundleInstalled".equals(property)) { //$NON-NLS-1$
			return computeResult(expectedValue, versionCheck(args));
		}
		return false;
	}

	private boolean versionCheck(Object[] args) {
		if (args != null && args.length > 0 && args[0] instanceof String) {
			Bundle bundle = Platform.getBundle((String) args[0]);
			if (bundle == null) {
				return false;
			}
			Version min = args.length > 1 ? toVersion(args[1]) : null;
			Version max = args.length > 2 ? toVersion(args[2]) : null;
			boolean inRange = true;
			if (min != null) {
				inRange = bundle.getVersion().compareTo(min) >= 0;
			}
			if (inRange && max != null) {
				inRange = bundle.getVersion().compareTo(max) < 0;
			}
			return inRange;
		}
		return false;
	}

	private Version toVersion(Object arg) {
		try {
			return Version.valueOf(arg.toString());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}
