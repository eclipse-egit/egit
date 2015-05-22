/*******************************************************************************
 * Copyright (C) 2014, 2016 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 486857
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.revision.FileRevisionEditorInput;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * Utilities for working with selections.
 */
public class SelectionUtils {

	/**
	 * @param selection
	 * @return the single selected repository, or <code>null</code>
	 */
	@Nullable
	public static Repository getRepository(
			@NonNull IStructuredSelection selection) {
		return getRepository(false, selection, null);
	}

	/**
	 * @param evaluationContext
	 * @return the single selected repository, or <code>null</code>
	 */
	@Nullable
	public static Repository getRepository(
			@Nullable IEvaluationContext evaluationContext) {
		return getRepository(false, getSelection(evaluationContext), null);
	}

	/**
	 * Get the single selected repository or warn if no repository or multiple
	 * different repositories could be found.
	 *
	 * @param selection
	 * @param shell
	 *            the shell for showing the warning
	 * @return the single selected repository, or <code>null</code>
	 */
	@Nullable
	public static Repository getRepositoryOrWarn(
			@NonNull IStructuredSelection selection, @NonNull Shell shell) {
		return getRepository(true, selection, shell);
	}

	/**
	 * @param context
	 * @return the structured selection of the evaluation context
	 */
	@NonNull
	public static IStructuredSelection getSelection(
			@Nullable IEvaluationContext context) {
		if (context == null)
			return StructuredSelection.EMPTY;

		Object selection = context
				.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		if (!(selection instanceof ISelection))
			selection = context
					.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);

		if (selection instanceof ITextSelection)
			return getSelectionFromEditorInput(context);
		else if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		return StructuredSelection.EMPTY;
	}

	/**
	 * Tries to convert the passed selection to a structured selection.
	 * <p>
	 * E.g. in case of a text selection, it is converted to be a selection of
	 * the resource that is in the editor.
	 *
	 * @param selection
	 * @return the structured selection, or an empty selection
	 */
	@NonNull
	public static IStructuredSelection getStructuredSelection(
			@NonNull ISelection selection) {
		if (selection instanceof ITextSelection) {
			IEvaluationContext evaluationContext = getEvaluationContext();
			if (evaluationContext == null) {
				return StructuredSelection.EMPTY;
			}
			return getSelectionFromEditorInput(evaluationContext);
		} else if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * @param selection
	 * @return the selected locations
	 */
	@NonNull
	public static IPath[] getSelectedLocations(
			@NonNull IStructuredSelection selection) {
		Set<IPath> result = new LinkedHashSet<>();
		for (Object o : selection.toList()) {
			IResource resource = AdapterUtils.adaptToAnyResource(o);
			if (resource != null) {
				IPath location = resource.getLocation();
				if (location != null)
					result.add(location);
			} else {
				IPath location = AdapterUtils.adapt(o, IPath.class);
				if (location != null)
					result.add(location);
				else
					for (IResource r : extractResourcesFromMapping(o)) {
						IPath l = r.getLocation();
						if (l != null)
							result.add(l);
					}
			}
		}
		return result.toArray(new IPath[result.size()]);
	}

	/**
	 * Returns the resources contained in the given selection.
	 *
	 * @param selection
	 * @return the resources in the selection
	 */
	@NonNull
	public static IResource[] getSelectedResources(
			@NonNull IStructuredSelection selection) {
		Set<IResource> result = getSelectedResourcesSet(selection);
		return result.toArray(new IResource[result.size()]);
	}

	/**
	 * Returns the resources contained in the given selection.
	 *
	 * @param selection
	 * @return the resources in the selection
	 */
	@NonNull
	private static Set<IResource> getSelectedResourcesSet(
			@NonNull IStructuredSelection selection) {
		Set<IResource> result = new LinkedHashSet<>();
		for (Object o : selection.toList()) {
			IResource resource = AdapterUtils.adaptToAnyResource(o);
			if (resource != null)
				result.add(resource);
			else
				result.addAll(extractResourcesFromMapping(o));
		}
		return result;
	}

	private static List<IResource> extractResourcesFromMapping(Object o) {
		ResourceMapping mapping = AdapterUtils.adapt(o, ResourceMapping.class);
		if (mapping == null)
			return Collections.emptyList();

		ResourceTraversal[] traversals;
		try {
			traversals = mapping.getTraversals(null, null);
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
			return Collections.emptyList();
		}

		if (traversals.length == 0)
			return Collections.emptyList();

		List<IResource> result = new ArrayList<>();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			result.addAll(Arrays.asList(resources));
		}
		return result;
	}

	/**
	 * Determines the most fitting {@link HistoryPageInput} for the given
	 * {@link IStructuredSelection}. The {@code mandatoryObject} must be
	 * contained in the selection and in a repository.
	 * <p>
	 * Most fitting means that the input will contain all selected resources
	 * which are contained in the same repository as the given
	 * {@code mandatoryObject}.
	 * </p>
	 *
	 * @param selection
	 *            The selection for which the most fitting HistoryPageInput is
	 *            to be determined.
	 * @param mandatoryObject
	 *            The object to which the HistoryPageInput is tailored. Must be
	 *            contained in the given selection and in a repository.
	 * @return The most fitting HistoryPageInput. Will return {@code null} when
	 *         the {@code mandatoryObject} is not contained in the given
	 *         selection or in a repository.
	 */
	@Nullable
	public static HistoryPageInput getMostFittingInput(
			@NonNull IStructuredSelection selection, Object mandatoryObject) {
		Set<IResource> resources = getSelectedResourcesSet(selection);
		if (!resources.contains(mandatoryObject)) {
			return null;
		}

		Repository repository = ResourceUtil
				.getRepository((IResource) mandatoryObject);
		if (repository == null) {
			return null;
		}

		for (Iterator<IResource> it = resources.iterator(); it.hasNext();) {
			IResource resource = it.next();
			if (ResourceUtil.getRepository(resource) != repository) {
				it.remove();
			}
		}

		IResource[] resourceArray = resources.toArray(new IResource[resources
				.size()]);
		return new HistoryPageInput(repository, resourceArray);
	}

	/**
	 * Determines a set of either {@link Repository}, {@link IResource}s or
	 * {@link IPath}s from a selection. For selection contents that adapt to
	 * {@link Repository}, {@link IResource} or {@link ResourceMapping}, the
	 * containing {@link Repository}s or {@link IResource}s are included in the
	 * result set; otherwise for selection contents that adapt to {@link IPath}
	 * these paths are included.
	 *
	 * @param selection
	 *            to process
	 * @return the set of {@link Repository}, {@link IResource} and
	 *         {@link IPath} objects from the selection; not containing
	 *         {@code null} values
	 */
	@NonNull
	private static Set<Object> getSelectionContents(
			@NonNull IStructuredSelection selection) {
		Set<Object> result = new HashSet<>();
		for (Object o : selection.toList()) {
			Repository r = AdapterUtils.adapt(o, Repository.class);
			if (r != null) {
				result.add(r);
				continue;
			}
			IResource resource = AdapterUtils.adaptToAnyResource(o);
			if (resource != null) {
				result.add(resource);
				continue;
			}
			ResourceMapping mapping = AdapterUtils.adapt(o,
					ResourceMapping.class);
			if (mapping != null) {
				result.addAll(extractResourcesFromMapping(mapping));
			} else {
				IPath location = AdapterUtils.adapt(o, IPath.class);
				if (location != null) {
					result.add(location);
				}
			}
		}
		return result;
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @param warn
	 *            Put up a message dialog to warn why a resource was not
	 *            selected
	 * @param selection
	 * @param shell
	 *            must be provided if warn = true
	 * @return repository for current project, or null
	 */
	@Nullable
	private static Repository getRepository(boolean warn,
			@NonNull IStructuredSelection selection, Shell shell) {
		if (selection.isEmpty()) {
			return null;
		}
		Set<Object> elements = getSelectionContents(selection);
		if (GitTraceLocation.SELECTION.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.SELECTION.getLocation(), "selection=" //$NON-NLS-1$
							+ selection + ", elements=" + elements.toString()); //$NON-NLS-1$

		boolean hadNull = false;
		Repository result = null;
		for (Object location : elements) {
			Repository repo = null;
			if (location instanceof Repository) {
				repo = (Repository) location;
			} else if (location instanceof IResource) {
				repo = ResourceUtil.getRepository((IResource) location);
			} else if (location instanceof IPath) {
				repo = ResourceUtil.getRepository((IPath) location);
			}
			if (repo == null) {
				hadNull = true;
			}
			if (result == null) {
				result = repo;
			}
			boolean mismatch = hadNull && result != null;
			if (mismatch || result != repo) {
				if (warn) {
					MessageDialog.openError(shell,
							UIText.RepositoryAction_multiRepoSelectionTitle,
							UIText.RepositoryAction_multiRepoSelection);
				}
				return null;
			}
		}

		if (result == null) {
			for (Object o : selection.toArray()) {
				Repository nextRepo = AdapterUtils.adapt(o, Repository.class);
				if (nextRepo != null && result != null && result != nextRepo) {
					if (warn)
						MessageDialog
								.openError(
										shell,
										UIText.RepositoryAction_multiRepoSelectionTitle,
										UIText.RepositoryAction_multiRepoSelection);
					return null;
				}
				result = nextRepo;
			}
		}

		if (result == null) {
			if (warn)
				MessageDialog.openError(shell,
						UIText.RepositoryAction_errorFindingRepoTitle,
						UIText.RepositoryAction_errorFindingRepo);
			return null;
		}

		return result;
	}

	private static IStructuredSelection getSelectionFromEditorInput(
			IEvaluationContext context) {
		Object part = context.getVariable(ISources.ACTIVE_PART_NAME);
		if (!(part instanceof IEditorPart)) {
			return StructuredSelection.EMPTY;
		}
		Object object = context.getVariable(ISources.ACTIVE_EDITOR_INPUT_NAME);
		Object editor = context.getVariable(ISources.ACTIVE_EDITOR_NAME);
		if (editor instanceof MultiPageEditorPart) {
			Object nestedEditor = ((MultiPageEditorPart) editor)
					.getSelectedPage();
			if (nestedEditor instanceof IEditorPart) {
				object = ((IEditorPart) nestedEditor).getEditorInput();
			}
		}
		if (!(object instanceof IEditorInput)
				&& (editor instanceof IEditorPart)) {
			object = ((IEditorPart) editor).getEditorInput();
		}
		if (object instanceof IEditorInput) {
			IEditorInput editorInput = (IEditorInput) object;
			// Note that there is both a getResource(IEditorInput) as well as a
			// getResource(Object), which don't do the same thing. We explicitly
			// want the first here.
			IResource resource = org.eclipse.ui.ide.ResourceUtil
					.getResource(editorInput);
			if (resource != null)
				return new StructuredSelection(resource);
			if (editorInput instanceof FileRevisionEditorInput) {
				FileRevisionEditorInput fileRevisionEditorInput = (FileRevisionEditorInput) editorInput;
				IFileRevision fileRevision = fileRevisionEditorInput
						.getFileRevision();
				if (fileRevision != null)
					return new StructuredSelection(fileRevision);
			}
		}

		return StructuredSelection.EMPTY;
	}

	private static IEvaluationContext getEvaluationContext() {
		IEvaluationContext ctx;
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		// no active window during Eclipse shutdown
		if (activeWorkbenchWindow == null)
			return null;
		IHandlerService hsr = CommonUtils.getService(activeWorkbenchWindow, IHandlerService.class);
		ctx = hsr.getCurrentState();
		return ctx;
	}

}
