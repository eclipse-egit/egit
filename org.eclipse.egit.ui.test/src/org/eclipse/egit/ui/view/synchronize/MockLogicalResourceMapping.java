/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.view.synchronize.MockLogicalModelProvider.MOCK_LOGICAL_FILE_EXTENSION;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * This resource mapping requires a remote context to get the logical model. It
 * will parse the content of its file which is supposed to be a list of files in
 * the same folder and aggregate all these files in its logical model. It does
 * that for every side, remote and base included.
 */
public class MockLogicalResourceMapping extends ResourceMapping {
	private final IFile file;

	private final String providerId;

	public MockLogicalResourceMapping(IFile file, String providerId) {
		this.file = file;
		this.providerId = providerId;
	}

	@Override
	public Object getModelObject() {
		return file;
	}

	@Override
	public String getModelProviderId() {
		return providerId;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor sm = SubMonitor.convert(monitor, 3);
		Set<IFile> result = new LinkedHashSet<IFile>();
		result.add(file);
		try {
			List<IFile> dependencies = getDependencies(file, file.getParent());
			result.addAll(dependencies);
			sm.worked(1);
			if (context instanceof RemoteResourceMappingContext) {
				RemoteResourceMappingContext rmc = (RemoteResourceMappingContext) context;
				IStorage baseContents = rmc.fetchBaseContents(file,
						sm.newChild(1));
				if (baseContents != null) {
					result.addAll(
							getDependencies(baseContents, file.getParent()));
				}
				IStorage remoteContents = rmc.fetchRemoteContents(file,
						sm.newChild(1));
				if (remoteContents != null) {
					result.addAll(
							getDependencies(remoteContents, file.getParent()));
				}
			}
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, "org.eclipse.egit.ui.test",
							"Exception while computing logical model", e));
		}
		final IResource[] resourceArray = result
				.toArray(new IResource[0]);
		return new ResourceTraversal[] { new ResourceTraversal(resourceArray,
				IResource.DEPTH_ONE, IResource.NONE), };
	}

	private List<IFile> getDependencies(IStorage storage, IContainer c)
			throws CoreException, IOException {
		List<IFile> result = new ArrayList<>();
		try (InputStream contents = storage.getContents();) {
			BufferedReader r = new BufferedReader(
					new InputStreamReader(contents, Charset.forName("UTF-8")));
			try {
				while (true) {
					String line = r.readLine();
					IFile dep = c.getFile(new Path(line));
					result.add(dep);
				}
			} catch (Exception e) {
				// over
			}
		}
		return result;
	}

	protected void addLogicalModelFiles(IResource[] resources,
			Set<IFile> sampleSiblings) {
		if (resources == null) {
			return;
		}
		for (IResource res : resources) {
			if (res instanceof IFile && MOCK_LOGICAL_FILE_EXTENSION
					.equals(res.getFileExtension())) {
				sampleSiblings.add((IFile) res);
			}
		}
	}

	@Override
	public IProject[] getProjects() {
		return new IProject[] { file.getProject(), };
	}
}
