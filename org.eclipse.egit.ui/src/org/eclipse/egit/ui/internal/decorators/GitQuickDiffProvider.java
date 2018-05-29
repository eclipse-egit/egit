/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

/**
 * This class provides input for the Eclipse Quick Diff feature.
 */
public class GitQuickDiffProvider implements IQuickDiffReferenceProvider {

	private String id;

	private GitDocument document;

	private IResource resource;

	static Map<Repository,String> baseline = new WeakHashMap<>();

	/**
	 * Create the GitQuickDiffProvider instance
	 */
	public GitQuickDiffProvider() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(GitTraceLocation.QUICKDIFF.getLocation());
	}

	@Override
	public void dispose() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(GitTraceLocation.QUICKDIFF.getLocation());
		if (document != null)
			document.dispose();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public IDocument getReference(IProgressMonitor monitor)
			throws CoreException {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitQuickDiffProvider) file: " + resource); //$NON-NLS-1$
		if (resource == null)
			return null;

		// Document must only be created once
		if (document == null)
			document = createDocument(resource);
		return document;
	}

	private static GitDocument createDocument(IResource resource) {
		try {
			return GitDocument.create(resource);
		} catch (IOException e) {
			Activator.error(UIText.QuickDiff_failedLoading, e);
			return null;
		}
	}

	@Override
	public boolean isEnabled() {
		return resource != null
				&& org.eclipse.egit.core.internal.util.ResourceUtil
						.isSharedWithGit(resource.getProject());
	}

	@Override
	public void setActiveEditor(ITextEditor editor) {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.QUICKDIFF.getLocation(), editor.getTitle());
		IEditorInput editorInput = editor.getEditorInput();
		resource = ResourceUtil.getResource(editorInput);
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Set a new baseline for quickdiff
	 *
	 * @param repository
	 * @param baseline any commit reference, ref, symref or sha-1
	 * @throws IOException
	 */
	public static void setBaselineReference(final Repository repository, final String baseline) throws IOException {
		GitQuickDiffProvider.baseline.put(repository, baseline);
		GitDocument.refreshRelevant(repository);
	}
}
