/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *     Daniel Megert <daniel_megert@ch.ibm.com> - remove unnecessary @SuppressWarnings
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

class GitResourceVariantComparator implements IResourceVariantComparator {

	private final GitSynchronizeDataSet gsd;

	GitResourceVariantComparator(GitSynchronizeDataSet dataSet) {
		gsd = dataSet;
	}

	@Override
	public boolean compare(IResource local, IResourceVariant remote) {
		if (!local.exists() || remote == null) {
			return false;
		}

		if (local instanceof IFile) {
			if (remote.isContainer()) {
				return false;
			}

			InputStream stream = null;
			InputStream remoteStream = null;
			try {
				remoteStream = remote.getStorage(new NullProgressMonitor())
						.getContents();
				stream = getLocal(local);
				byte[] remoteBytes = new byte[8096];
				byte[] bytes = new byte[8096];

				int remoteRead = remoteStream.read(remoteBytes);
				int read = stream.read(bytes);
				if (remoteRead != read) {
					return false;
				}

				while (Arrays.equals(bytes, remoteBytes)) {
					remoteRead = remoteStream.read(remoteBytes);
					read = stream.read(bytes);
					if (remoteRead != read) {
						// didn't read the same amount, it's uneven
						return false;
					} else if (read == -1) {
						// both at EOF, check their contents
						return Arrays.equals(bytes, remoteBytes);
					}
				}
			} catch (IOException | CoreException e) {
				logException(e);
				return false;
			} finally {
				closeStream(stream);
				closeStream(remoteStream);
			}
		} else if (local instanceof IContainer) {
			GitRemoteFolder gitVariant = (GitRemoteFolder) remote;
			if (!remote.isContainer() || (local.exists() ^ gitVariant.exists()))
				return false;

			return local.getLocation().toString().equals(gitVariant.getCachePath());
		}
		return false;
	}

	@Override
	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		GitRemoteResource gitBase = (GitRemoteResource) base;
		GitRemoteResource gitRemote = (GitRemoteResource) remote;

		boolean exists = gitBase.exists() && gitRemote.exists();
		boolean equalType = !(gitBase.isContainer() ^ gitRemote.isContainer());
		boolean equalSha1 = gitBase.getObjectId().getName()
				.equals(gitRemote.getObjectId().getName());

		return equalType && exists && equalSha1;
	}

	@Override
	public boolean isThreeWay() {
		return true;
	}

	private InputStream getLocal(IResource resource) throws CoreException {
		if (gsd.getData(resource.getProject().getName()).shouldIncludeLocal())
			return getSynchronizedFile(resource).getContents();
		else
			try {
				if (resource.getType() == IResource.FILE)
					return getSynchronizedFile(resource).getContents();
				else
					return new ByteArrayInputStream(new byte[0]);
			} catch (TeamException e) {
				throw new CoreException(e.getStatus());
			}
	}

	private IFile getSynchronizedFile(IResource resource) throws CoreException {
		IFile file = ((IFile) resource);
		if (!file.isSynchronized(0))
			file.refreshLocal(0, null);

		return file;
	}

	private void logException(Exception e) {
		IStatus error = new Status(IStatus.ERROR, Activator.getPluginId(),
				e.getMessage(), e);
		Activator.getDefault().getLog().log(error);
	}

	private void closeStream(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				logException(e);
			}
		}
	}

}
