/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Common helper methods for the history command handlers
 */
abstract class AbstractHistoryCommanndHandler extends AbstractHandler {

	/** The team ui plugin ID which is not accessible */
	private static final String TEAM_UI_PLUGIN = "org.eclipse.team.ui"; //$NON-NLS-1$

	/**
	 * A copy of the non-accessible preference constant
	 * IPreferenceIds.REUSE_OPEN_COMPARE_EDITOR from the team ui plug in
	 */
	private static final String REUSE_COMPARE_EDITOR_PREFID = "org.eclipse.team.ui.reuse_open_compare_editors"; //$NON-NLS-1$

	protected IWorkbenchPart getPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}

	protected IStructuredSelection getSelection(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return new StructuredSelection();
	}

	protected Object getInput(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = getPart(event);
		if (!(part instanceof IHistoryView))
			throw new ExecutionException(
					UIText.AbstractHistoryCommanndHandler_NoInputMessage);
		return (((IHistoryView) part).getHistoryPage().getInput());
	}

	protected void openInCompare(ExecutionEvent event, CompareEditorInput input)
			throws ExecutionException {
		IWorkbenchPage workBenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		IEditorPart editor = findReusableCompareEditor(input, workBenchPage);
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			}
		} else {
			CompareUI.openCompareEditor(input);
		}
	}

	private IEditorPart findReusableCompareEditor(CompareEditorInput input,
			IWorkbenchPage page) {
		IEditorReference[] editorRefs = page.getEditorReferences();
		// first loop looking for an editor with the same input
		for (int i = 0; i < editorRefs.length; i++) {
			IEditorPart part = editorRefs[i].getEditor(false);
			if (part != null
					&& (part.getEditorInput() instanceof GitCompareFileRevisionEditorInput)
					&& part instanceof IReusableEditor
					&& part.getEditorInput().equals(input)) {
				return part;
			}
		}
		// if none found and "Reuse open compare editors" preference is on use
		// a non-dirty editor
		if (isReuseOpenEditor()) {
			for (int i = 0; i < editorRefs.length; i++) {
				IEditorPart part = editorRefs[i].getEditor(false);
				if (part != null
						&& (part.getEditorInput() instanceof SaveableCompareEditorInput)
						&& part instanceof IReusableEditor && !part.isDirty()) {
					return part;
				}
			}
		}
		// no re-usable editor found
		return null;
	}

	/**
	 * @return the flag
	 */
	private boolean isReuseOpenEditor() {
		boolean defaultReuse = new DefaultScope().getNode(TEAM_UI_PLUGIN)
				.getBoolean(REUSE_COMPARE_EDITOR_PREFID, false);
		return new InstanceScope().getNode(TEAM_UI_PLUGIN).getBoolean(
				REUSE_COMPARE_EDITOR_PREFID, defaultReuse);
	}

	protected Repository getRepository(ExecutionEvent event)
			throws ExecutionException {
		Object input = getInput(event);
		return RepositoryMapping.getMapping((IResource) input).getRepository();
	}

	/**
	 * @param event
	 * @return the tags
	 * @throws ExecutionException
	 */
	protected List<Tag> getRevTags(ExecutionEvent event)
			throws ExecutionException {
		Repository repo = getRepository(event);
		Collection<Ref> revTags = repo.getTags().values();
		List<Tag> tags = new ArrayList<Tag>();
		RevWalk walk = new RevWalk(repo);
		for (Ref ref : revTags) {
			try {
				Tag tag = walk.parseTag(repo.resolve(ref.getName()))
						.asTag(walk);
				tags.add(tag);
			} catch (IOException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		}
		return tags;
	}

	protected GitHistoryPage getPage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		if (window.getActivePage() == null)
			return null;
		IWorkbenchPart part = window.getActivePage().getActivePart();
		if (!(part instanceof IHistoryView))
			return null;
		IHistoryView view = (IHistoryView) part;
		IHistoryPage page = view.getHistoryPage();
		if (page instanceof GitHistoryPage)
			return (GitHistoryPage) page;
		return null;
	}

	protected IStructuredSelection getSelection(GitHistoryPage page) {
		ISelection pageSelection = page.getSelectionProvider().getSelection();
		if (pageSelection instanceof IStructuredSelection) {
			return (IStructuredSelection) pageSelection;
		} else
			return new StructuredSelection();
	}
}
