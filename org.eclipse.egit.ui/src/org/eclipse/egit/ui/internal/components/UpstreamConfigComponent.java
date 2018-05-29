/*******************************************************************************
 * Copyright (C) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Component for configuring the upstream of a branch (merge, rebase).
 */
public class UpstreamConfigComponent {

	/**
	 * Listener for when the user has selected an upstream config.
	 */
	public interface UpstreamConfigSelectionListener {
		/**
		 * @param upstreamConfig
		 *            selected by the user
		 */
		public void upstreamConfigSelected(BranchRebaseMode upstreamConfig);
	}

	private final Composite container;

	private Button configureUpstreamCheck;

	private BranchRebaseModeCombo rebase;

	private List<UpstreamConfigSelectionListener> listeners = new ArrayList<>();

	private Group upstreamConfigGroup;

	/**
	 * @param parent
	 *            the composite to use as a parent for the container
	 * @param style
	 *            the style of the container
	 */
	public UpstreamConfigComponent(Composite parent, int style) {
		container = new Composite(parent, style);
		container.setLayout(GridLayoutFactory.fillDefaults()
				.extendedMargins(0, 0, 0, 10).create());

		configureUpstreamCheck = new Button(container, SWT.CHECK);
		configureUpstreamCheck.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		configureUpstreamCheck
				.setText(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck);
		configureUpstreamCheck
				.setToolTipText(UIText.UpstreamConfigComponent_ConfigureUpstreamToolTip);
		configureUpstreamCheck.setSelection(true);

		upstreamConfigGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		upstreamConfigGroup.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).indent(UIUtils.getControlIndent(), 0)
				.create());
		upstreamConfigGroup.setLayout(
				GridLayoutFactory.swtDefaults().numColumns(2).create());

		rebase = new BranchRebaseModeCombo(upstreamConfigGroup);
		rebase.getViewer().addSelectionChangedListener(
				(event) -> upstreamConfigSelected());

		configureUpstreamCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabled();
				upstreamConfigSelected();
			}
		});
	}

	/**
	 * @return the container that holds all the controls
	 */
	public Composite getContainer() {
		return container;
	}

	/**
	 * @param listener
	 *            to add
	 */
	public void addUpstreamConfigSelectionListener(
			UpstreamConfigSelectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param upstreamConfig
	 *            to set the controls to
	 */
	public void setUpstreamConfig(BranchRebaseMode upstreamConfig) {
		if (upstreamConfig == null) {
			configureUpstreamCheck.setSelection(false);
		} else {
			configureUpstreamCheck.setSelection(true);
			rebase.setRebaseMode(upstreamConfig);
		}
		updateEnabled();
	}

	private void upstreamConfigSelected() {
		BranchRebaseMode config = getSelectedRebaseMode();
		for (UpstreamConfigSelectionListener listener : listeners) {
			listener.upstreamConfigSelected(config);
		}
	}

	/**
	 * Retrieves the selected {@link BranchRebaseMode}.
	 *
	 * @return the {@link BranchRebaseMode}, or {@code null} if none selected.
	 */
	public BranchRebaseMode getSelectedRebaseMode() {
		if (!configureUpstreamCheck.getSelection()) {
			return null;
		} else {
			return rebase.getRebaseMode();
		}
	}

	private void updateEnabled() {
		boolean enabled = configureUpstreamCheck.getSelection();
		upstreamConfigGroup.setEnabled(enabled);
		rebase.setEnabled(enabled);
	}
}
