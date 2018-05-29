/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Input component for {@link BranchRebaseMode}: a {@link ComboViewer} with a
 * label, allowing the user to choose a branch rebase mode.
 */
public class BranchRebaseModeCombo {

	private final @NonNull Label label;

	private final @NonNull ComboViewer combo;

	/**
	 * Creates a new {@link BranchRebaseModeCombo} in the given {@code parent}.
	 *
	 * @param parent
	 *            {@link Composite} to contain the controls
	 */
	public BranchRebaseModeCombo(Composite parent) {
		label = new Label(parent, SWT.NONE);
		label.setText(UIText.BranchRebaseModeCombo_RebaseModeLabel);
		combo = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		combo.setContentProvider(ArrayContentProvider.getInstance());
		combo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == BranchRebaseMode.REBASE) {
					return UIText.BranchRebaseMode_Rebase;
				} else if (element == BranchRebaseMode.PRESERVE) {
					return UIText.BranchRebaseMode_Preserve;
				} else if (element == BranchRebaseMode.INTERACTIVE) {
					return UIText.BranchRebaseMode_Interactive;
				} else if (element == BranchRebaseMode.NONE) {
					return UIText.BranchRebaseMode_None;
				}
				return ""; //$NON-NLS-1$
			}
		});
		combo.setInput(BranchRebaseMode.values());
	}

	/**
	 * Retrieves the currently selected {@link BranchRebaseMode}.
	 *
	 * @return the {@link BranchRebaseMode}, or {@code null} if none selected.
	 */
	public BranchRebaseMode getRebaseMode() {
		IStructuredSelection selection = (IStructuredSelection) combo
				.getSelection();
		return selection.isEmpty() ? null
				: (BranchRebaseMode) selection.getFirstElement();

	}

	/**
	 * Sets the currently selected {@link BranchRebaseMode}.
	 *
	 * @param mode
	 *            to select and reveal, or {@code null} to clear the selection
	 */
	public void setRebaseMode(BranchRebaseMode mode) {
		if (mode == null) {
			combo.setSelection(StructuredSelection.EMPTY, false);
		} else {
			combo.setSelection(new StructuredSelection(mode), true);
		}
	}

	/**
	 * Retrieves the {@link Label} control.
	 *
	 * @return the {@link Label}
	 */
	public @NonNull Label getLabel() {
		return label;
	}

	/**
	 * Retrieves the {@link ComboViewer}.
	 *
	 * @return the {@link ComboViewer}
	 */
	public @NonNull ComboViewer getViewer() {
		return combo;
	}

	/**
	 * Sets the enablement of the {@link BranchRebaseModeCombo}.
	 *
	 * @param enabled
	 *            whether the component is enabled
	 */
	public void setEnabled(boolean enabled) {
		label.setEnabled(enabled);
		combo.getCombo().setEnabled(enabled);
	}

	/**
	 * Retrieves the enablement of the {@link BranchRebaseModeCombo}.
	 *
	 * @return whether the component is enabled
	 */
	public boolean isEnabled() {
		return combo.getCombo().isEnabled();
	}

}
