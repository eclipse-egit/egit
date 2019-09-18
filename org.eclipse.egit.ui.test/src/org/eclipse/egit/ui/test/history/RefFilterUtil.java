/*******************************************************************************
 * Copyright (C) 2019, Tim Neumann <Tim.Neumann@advantest.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.history;

import static org.mockito.Mockito.when;

import java.util.Objects;

import org.eclipse.egit.ui.internal.history.RefFilterHelper;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.Mockito;

public class RefFilterUtil {
	private RefFilterUtil() {
		// Forbid instantiation
	}

	public static Matcher<RefFilter> newRefFilterMatcher(String filterString,
			boolean preConfigured,
			boolean selected) {
		return new TypeSafeMatcher<RefFilterHelper.RefFilter>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("a ref filter with ");
				description.appendValue(filterString);
				description.appendText(" which is ");
				if (!preConfigured) {
					description.appendText("_not_ ");
				}
				description.appendText("preconfigured and ");
				if (!selected) {
					description.appendText("_not_ ");
				}
				description.appendText("selected");
			}

			@Override
			protected boolean matchesSafely(RefFilter item) {
				if (item.isPreconfigured() != preConfigured) {
					return false;
				}

				if (item.isSelected() != selected) {
					return false;
				}

				return Objects.equals(filterString, item.getFilterString());
			}
		};
	}

	@SuppressWarnings("boxing")
	public static RefFilter newRefFilter(RefFilterHelper helper,
			String filterString, boolean preConfigured,
			boolean selected) {
		RefFilter mock = Mockito.mock(RefFilter.class);
		when(mock.getFilterString()).thenReturn(filterString);
		when(mock.isPreconfigured()).thenReturn(preConfigured);
		when(mock.isSelected()).thenReturn(selected);
		return helper.new RefFilter(mock);
	}

}
