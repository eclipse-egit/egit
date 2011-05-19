/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_REFS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.egit.ui.internal.synchronize.GitSynchronizeWizard;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectIdRef.PeeledTag;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 *
 */
public class SynchronizeWithMenu extends ContributionItem implements
		IWorkbenchContribution {

	/** the maximum number of refs to show in the sub-menu */
	private static final int MAX_NUM_MENU_ENTRIES = 20;

	private final Image tagImage;

	private final Image branchImage;

	private ISelectionService srv;

	/**
	 *
	 */
	public SynchronizeWithMenu() {
		this(null);
	}

	/**
	 * @param id
	 */
	public SynchronizeWithMenu(String id) {
		super(id);

		tagImage = UIIcons.TAG.createImage();
		branchImage = UIIcons.BRANCH.createImage();
	}

	@Override
	public void fill(final Menu menu, int index) {
		if (srv == null)
			return;
		final IProject selectedProject = getSelection();
		if (selectedProject == null)
			return;

		RepositoryMapping mapping = RepositoryMapping
				.getMapping(selectedProject);
		if (mapping == null)
			return;

		final Repository repo = mapping.getRepository();
		if (repo == null)
			return;

		List<Ref> refs = new LinkedList<Ref>();
		RefDatabase refDatabase = repo.getRefDatabase();
		try {
			refs.addAll(refDatabase.getAdditionalRefs());
		} catch (IOException e) {
			// do nothing
		}
		try {
			refs.addAll(refDatabase.getRefs(RefDatabase.ALL).values());
		} catch (IOException e) {
			// do nothing
		}
		Collections.sort(refs, CommonUtils.REF_ASCENDING_COMPARATOR);
		String currentBranch;
		try {
			currentBranch = repo.getFullBranch();
		} catch (IOException e) {
			currentBranch = ""; //$NON-NLS-1$
		}

		int count = 0;
		String oldName = null;
		int refsLength = R_REFS.length();
		int tagsLength = R_TAGS.substring(refsLength).length();
		for (Ref ref : refs) {
			final String name = ref.getName();
			if (name.equals(Constants.HEAD) || name.equals(currentBranch) || excludeTag(ref, repo))
				continue;
			if (name.startsWith(R_REFS) && oldName != null
					&& !oldName.regionMatches(refsLength, name, refsLength,
							tagsLength))
				new MenuItem(menu, SWT.SEPARATOR);

			MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setText(name);
			if (name.startsWith(Constants.R_TAGS))
				item.setImage(tagImage);
			else if (name.startsWith(Constants.R_HEADS) || name.startsWith(Constants.R_REMOTES))
				item.setImage(branchImage);

			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					GitSynchronizeData data;
					try {
						data = new GitSynchronizeData(repo, HEAD, name, true);
						GitModelSynchronize.launch(data, new IResource[] { selectedProject });
					} catch (IOException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			});

			if (++count == MAX_NUM_MENU_ENTRIES)
				break;
			oldName = name;
		}

		if (count > 1)
			new MenuItem(menu, SWT.SEPARATOR);

		MenuItem custom = new MenuItem(menu, SWT.PUSH);
		custom.setText(UIText.SynchronizeWithMenu_custom);
		custom.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GitSynchronizeWizard gitWizard = new GitSynchronizeWizard();
				gitWizard.selectProjects(selectedProject);
				WizardDialog wizard = new WizardDialog(menu.getShell(),
						gitWizard);
				wizard.create();
				wizard.open();
			}
		});
	}

	public void initialize(IServiceLocator serviceLocator) {
		srv = (ISelectionService) serviceLocator
		.getService(ISelectionService.class);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
	@Override
	public void dispose() {
		tagImage.dispose();
		branchImage.dispose();
	}

	private IProject getSelection() {
		ISelection sel = srv.getSelection();

		if (!(sel instanceof IStructuredSelection))
			return null;

		Object selected = ((IStructuredSelection) sel).getFirstElement();
		if (selected instanceof IAdaptable)
			return (IProject) ((IAdaptable) selected)
					.getAdapter(IProject.class);
		if (selected instanceof IProject)
			return (IProject) selected;

		return null;
	}

	private boolean excludeTag(Ref ref, Repository repo) {
		if (ref instanceof PeeledTag) {
			RevWalk rw = new RevWalk(repo);
			try {
				RevTag tag = rw.parseTag(ref.getObjectId());

				return !(rw.parseAny(tag.getObject()) instanceof RevCommit);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		return false;
	}

}
