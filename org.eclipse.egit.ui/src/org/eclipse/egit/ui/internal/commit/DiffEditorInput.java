/*******************************************************************************
 *  Copyright (c) 2020 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.Objects;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An {@link IEditorInput} that gives access to the {@link DiffDocument} created
 * by a diff formatter. Intended to be used as input for a
 * {@link DiffEditorPage} to show a pre-created unified diff.
 */
public class DiffEditorInput implements IEditorInput {

	private final DiffDocument document;

	private final String title;

	/**
	 * Creates a new {@link DiffEditorInput} for the given {@link DiffDocument}.
	 *
	 * @param diff
	 *            to show
	 */
	public DiffEditorInput(DiffDocument diff) {
		this(diff, UIText.DiffEditorPage_Title);
	}

	/**
	 * Creates a new {@link DiffEditorInput} for the given {@link DiffDocument}
	 * with a given title.
	 *
	 * @param diff
	 *            to show
	 * @param title
	 *            for the diff
	 */
	public DiffEditorInput(DiffDocument diff, String title) {
		document = diff;
		this.title = title;
	}

	/**
	 * @return the {@link DiffDocument}
	 */
	public DiffDocument getDocument() {
		return document;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof DiffEditorInput)
				&& Objects.equals(document, ((DiffEditorInput) obj).document);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(document);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return UIIcons.CHANGESET;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}
}
