/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.GitProvider;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.eclipse.jgit.lib.Repository;

/**
 * This class provides input for the Eclipse Quick Diff feature.
 */
public class GitQuickDiffProvider implements IQuickDiffReferenceProvider {

	private String id;

	private GitDocument document;

	private IResource resource;

	static Map<Repository,String> baseline = new WeakHashMap<Repository,String>();

	/**
	 * Create the GitQuickDiffProvider instance
	 */
	public GitQuickDiffProvider() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(GitTraceLocation.QUICKDIFF.getLocation());
	}

	public void dispose() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(GitTraceLocation.QUICKDIFF.getLocation());
		if (document != null)
			document.dispose();
	}

	public String getId() {
		return id;
	}

	public IDocument getReference(IProgressMonitor monitor)
			throws CoreException {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitQuickDiffProvider) file: " + resource); //$NON-NLS-1$
		if (resource == null)
			return null;
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject());
		if (provider != null) {
			try {
				document = GitDocument.create(resource);
			} catch (IOException e) {
				Activator.error(UIText.QuickDiff_failedLoading, e);
			}
			return document;
		} else {
			return null;
		}
	}

	public boolean isEnabled() {
		return resource != null
				&& RepositoryProvider.getProvider(resource.getProject(),
						GitProvider.ID) != null;
	}

	public void setActiveEditor(ITextEditor editor) {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.QUICKDIFF.getLocation(), editor.getTitle());
		IEditorInput editorInput = editor.getEditorInput();
		resource = ResourceUtil.getResource(editorInput);
	}

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
