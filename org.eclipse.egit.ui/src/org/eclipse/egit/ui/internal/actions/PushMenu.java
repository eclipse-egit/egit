/*******************************************************************************
 * Copyright (C) 2014, Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   - Mickael Istria (Red Hat Inc.) - 436669 Simply push workflow
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISources;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * This is the definition of the Push menu on a given node. Depending on the
 * node, it will show either "Push Branch '...'" or "Push HEAD".
 *
 * @author mistria
 *
 */
public class PushMenu extends ContributionItem implements
		IWorkbenchContribution {

	private ISelectionService selectionService;

	private ICommandService commandService;

	private IHandlerService handlerService;

	private Set<Resource> disposables = new HashSet<Resource>();

	/**
	 */
	public PushMenu() {
		this(null);
	}

	/**
	 * @param id
	 */
	public PushMenu(String id) {
		super(id);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public void fill(Menu menu, int index) {
		if (this.selectionService == null) {
			return;
		}
		final ISelection sel = this.selectionService.getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
		Object selected = ((IStructuredSelection) sel).getFirstElement();
		if (selected instanceof IAdaptable) {
			Object adapter = ((IAdaptable) selected).getAdapter(IProject.class);
			if (adapter != null)
				selected = adapter;
		}

		Repository repository = null;
		if (selected instanceof RepositoryNode)
			repository = ((RepositoryNode) selected).getRepository();
		else if (selected instanceof BranchesNode)
			repository = ((BranchesNode) selected).getRepository();
		else if (selected instanceof LocalNode)
			repository = ((LocalNode) selected).getRepository();
		else if ((selected instanceof IProject)) {
			RepositoryMapping mapping = RepositoryMapping
					.getMapping((IProject) selected);
			if (mapping != null)
				repository = mapping.getRepository();
		}

		if (repository != null) {
			try {
				String ref = repository.getFullBranch();
				String menuLabel = UIText.PushMenu_PushHEAD;
				if (ref.startsWith(Constants.R_HEADS)) {
					menuLabel = NLS.bind(UIText.PushMenu_PushBranch,
							Repository.shortenRefName(ref));
				}
				MenuItem pushMenu = new MenuItem(menu, SWT.PUSH);
				pushMenu.setText(menuLabel);
				Image menuIcon = UIIcons.PUSH.createImage();
				this.disposables.add(menuIcon);
				pushMenu.setImage(menuIcon);

				final Command command = commandService
						.getCommand(ActionCommands.PUSH_BRANCH_ACTION);
				final ExecutionEvent executionEvent = handlerService
						.createExecutionEvent(command, new Event());
				((IEvaluationContext) executionEvent
						.getApplicationContext()).addVariable(
						ISources.ACTIVE_CURRENT_SELECTION_NAME, sel);
				pushMenu.setEnabled(command.isEnabled() && command.isHandled());

				pushMenu.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						try {
							command.executeWithChecks(executionEvent);
						} catch (Exception ex) {
							Activator
									.getDefault()
									.getLog()
									.log(new Status(IStatus.ERROR, Activator
											.getDefault().getBundle()
											.getSymbolicName(),
											ex.getMessage(), ex));
						}
					}
				});
			} catch (IOException ex) {
				// TODO
			}
		}
	}

	public void initialize(IServiceLocator serviceLocator) {
		this.selectionService = (ISelectionService) serviceLocator
				.getService(ISelectionService.class);
		this.commandService = (ICommandService) serviceLocator
				.getService(ICommandService.class);
		this.handlerService = (IHandlerService) serviceLocator
				.getService(IHandlerService.class);
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Resource disposable : this.disposables) {
			disposable.dispose();
		}
	}
}
