/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * A {@link ContentOutlinePage} that is to be nested in a
 * {@link MultiPageEditorContentOutlinePage}.
 */
public class NestedContentOutlinePage extends ContentOutlinePage {

	@Override
	public void init(IPageSite pageSite) {
		// ContentOutlinePage insists on setting itself as selection provider.
		// For pages nested inside a MultiPageEditorContentOutlinePage this is
		// wrong. Save and restore the selection provider.
		ISelectionProvider provider = pageSite.getSelectionProvider();
		super.init(pageSite);
		pageSite.setSelectionProvider(provider);
	}
}
