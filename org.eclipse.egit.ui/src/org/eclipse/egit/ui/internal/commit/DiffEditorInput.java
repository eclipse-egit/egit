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

import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An {@link IEditorInput} that gives access to the {@link DiffDocument} created
 * by a diff formatter. Intended to be used as input for a {@link DiffEditor} to
 * show a unified diff.
 */
public class DiffEditorInput implements IEditorInput {

	private IDocument document;

	private String title;

	private String tooltip;

	private final @NonNull IRepositoryCommit tip;

	private final IRepositoryCommit base;

	/**
	 * Creates a new {@link DiffEditorInput} for the given
	 * {@link IRepositoryCommit}.
	 *
	 * @param commit
	 *            to diff
	 */
	public DiffEditorInput(@NonNull IRepositoryCommit commit) {
		this(commit, null, null, null);
	}

	/**
	 * Creates a new {@link DiffEditorInput} for the given
	 * {@link IRepositoryCommit}s.
	 *
	 * @param tip
	 *            top commit of the diff
	 * @param base
	 *            base commit of the diff, if {@null}, the parent of tip is
	 *            taken
	 * @throws IllegalArgumentException
	 *             if the two commits are from different repositories
	 */
	public DiffEditorInput(@NonNull IRepositoryCommit tip,
			IRepositoryCommit base) {
		this(tip, base, null, null);
	}

	/**
	 * Creates a new {@link DiffEditorInput} for the given
	 * {@link IRepositoryCommit}s and {@link DiffDocument}.
	 *
	 * @param tip
	 *            top commit of the diff
	 * @param base
	 *            base commit of the diff, if {@null}, the parent of tip is
	 *            taken
	 * @param diff
	 *            to show if already computed, may be {@code null}
	 * @throws IllegalArgumentException
	 *             if the two commits are from different repositories
	 */
	public DiffEditorInput(@NonNull IRepositoryCommit tip,
			IRepositoryCommit base, DiffDocument diff) {
		this(tip, base, diff, null);
	}

	/**
	 * Creates a new {@link DiffEditorInput} for the given
	 * {@link IRepositoryCommit}s and {@link DiffDocument} with a given title.
	 *
	 * @param tip
	 *            top commit of the diff
	 * @param base
	 *            base commit of the diff; if {@null}, the parent of tip is
	 *            taken
	 * @param diff
	 *            to show if already computed, may be {@code null}
	 * @param title
	 *            for the editor input; may be shown in the UI; if {@code null},
	 *            a default title is computed
	 * @throws IllegalArgumentException
	 *             if the two commits are from different repositories
	 */
	public DiffEditorInput(@NonNull IRepositoryCommit tip,
			IRepositoryCommit base, DiffDocument diff, String title) {
		Assert.isLegal(base == null || tip.getRepository().getDirectory()
				.equals(base.getRepository().getDirectory()));
		this.tip = tip;
		this.base = base;
		this.document = diff;
		this.title = title;
	}

	/**
	 * @return the tip {@link IRepositoryCommit}
	 */
	@NonNull
	public IRepositoryCommit getTip() {
		return tip;
	}

	/**
	 * @return the tip {@link IRepositoryCommit}, or {@code null} if none
	 */
	public IRepositoryCommit getBase() {
		return base;
	}

	/**
	 * Sets the document.
	 *
	 * @param diff
	 */
	public void setDocument(IDocument diff) {
		document = diff;
	}

	/**
	 * @return the {@link IDocument}
	 */
	public IDocument getDocument() {
		return document;
	}

	@Override
	public String getName() {
		if (title == null) {
			if (base == null) {
				title = MessageFormat.format(UIText.DiffEditorInput_Title1,
						tip.getObjectId().abbreviate(7).name(),
						GitLabels.getPlainShortLabel(tip.getRepository()));
			} else {
				title = MessageFormat.format(UIText.DiffEditorInput_Title2,
						base.getObjectId().abbreviate(7).name(),
						tip.getObjectId().abbreviate(7).name(),
						GitLabels.getPlainShortLabel(tip.getRepository()));
			}
		}
		return title;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		return (obj instanceof DiffEditorInput)
				&& Objects.equals(tip, ((DiffEditorInput) obj).tip)
				&& Objects.equals(base, ((DiffEditorInput) obj).base);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tip, base);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IRepositoryCommit.class) {
			return adapter.cast(tip);
		} else if (adapter == RevCommit.class) {
			return adapter.cast(tip.getRevCommit());
		} else if (adapter == Repository.class) {
			return adapter.cast(tip.getRepository());
		}
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
		if (tooltip == null) {
			if (base == null) {
				tooltip = MessageFormat.format(UIText.DiffEditorInput_Tooltip1,
						tip.getObjectId().abbreviate(7).name(),
						GitLabels.getPlainShortLabel(tip.getRepository()));
			} else {
				tooltip = MessageFormat.format(
						UIText.DiffEditorInput_Tooltip2,
						base.getObjectId().abbreviate(7).name(),
						tip.getObjectId().abbreviate(7).name(),
						GitLabels.getPlainShortLabel(tip.getRepository()));
			}
		}
		return tooltip;
	}
}
