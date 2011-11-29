/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import org.eclipse.ui.IEditorReference;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

class CompareEditorTitleMatcher extends BaseMatcher<IEditorReference> {

	private final String fileName;

	public CompareEditorTitleMatcher(String fileName) {
		this.fileName = fileName;
	}

	public void describeTo(Description description) {
		description.appendText("Editor that title contains text: " + fileName);
	}

	public boolean matches(Object item) {
		if (item instanceof IEditorReference) {
			IEditorReference editor = (IEditorReference) item;
			return editor.getTitle().contains(fileName);
		}

		return false;
	}

}
