/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.List;

import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.SyncRefEntity;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class RemoteSelectionCombo extends Composite {

	private Combo refsCombo;

	private Combo remotesCombo;

	private final List<SyncRepoEntity> syncRepos;

	public RemoteSelectionCombo(Composite parent,
			List<SyncRepoEntity> syncRepos, String remoteLabel, String refLabel) {
		super(parent, SWT.NONE);
		this.syncRepos = syncRepos;

		setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

		createRemoteGroup(remoteLabel);
		createRefsGroup(refLabel);
	}

	@Override
	public void setEnabled(boolean enabled) {
		refsCombo.setEnabled(enabled);
		remotesCombo.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	private void createRemoteGroup(String remoteLabel) {
		Composite remoteComposite = new Composite(this, SWT.NONE);
		remoteComposite.setLayout(new GridLayout());
		remoteComposite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).hint(150, SWT.DEFAULT).create());
		new Label(remoteComposite, SWT.NONE).setText(remoteLabel);
		remotesCombo = new Combo(remoteComposite, SWT.NONE);
		remotesCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		for (SyncRepoEntity syncRepoEnt : syncRepos) {
			remotesCombo.add(syncRepoEnt.getName());
		}
		remotesCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fillRefsCombo();
			}
		});
	}

	private void createRefsGroup(String refLabel) {
		Composite refsComposite = new Composite(this, SWT.NONE);
		refsComposite.setLayout(new GridLayout());
		refsComposite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).hint(150, SWT.DEFAULT).create());
		Label ref = new Label(refsComposite, SWT.NONE);
		ref.setText(refLabel);
		refsCombo = new Combo(refsComposite, SWT.NONE);
		refsCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
	}

	public String getValue() {
		int refSelectedIndex = refsCombo.getSelectionIndex();
		int remoteSelectedIndex = remotesCombo.getSelectionIndex();

		if (remoteSelectedIndex < 0 && refSelectedIndex < 0) {
			return ""; //$NON-NLS-1$
		}

		return syncRepos.get(remoteSelectedIndex).getRefList().get(
				refSelectedIndex).getValue();
	}

	public void setDefaultValue(String remote, String ref) {
		int i = 0;
		for (; i < syncRepos.size(); i++)
			if (syncRepos.get(i).getName().equals(remote))
				break;

		if (i == syncRepos.size())
			return;	// repository name not found

		remotesCombo.select(i);
		fillRefsCombo();

		List<SyncRefEntity> refList = syncRepos.get(i).getRefList();
		i = 0;
		for (; i < refList.size(); i++)
			if (refList.get(i).getDescription().equals(ref))
				break;

		if (i == syncRepos.size())
			return;	// ref name not found
		refsCombo.select(i);
	}

	private void fillRefsCombo() {
		int selected = remotesCombo.getSelectionIndex();
		if (selected < 0) {
			return;
		}

		refsCombo.removeAll();
		SyncRepoEntity syncRepoEnt = syncRepos.get(selected);
		for (SyncRefEntity syncRefEnt : syncRepoEnt.getRefList()) {
			refsCombo.add(syncRefEnt.getDescription());
		}

		if (refsCombo.getItemCount() > 0) {
			refsCombo.select(0);
		}
	}

}
