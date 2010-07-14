/*
 * Copyright (C) 2010, Red Hat Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.egit.core;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.ignore.SimpleIgnoreCache;
/**
 * EGit implementation of IgnoreCache has options to partially initialize the
 * cache using IResource
 *
 *
 */
public class IgnoreCache extends SimpleIgnoreCache {


	/**
	 * Creates a base implementation of an ignore cache. This default implementation
	 * will search for all .gitignore files in all children of the base directory,
	 * and grab the exclude file from baseDir/.git/info/exclude.
	 * <br><br>
	 * Call {@link #initialize()} to fetch the ignore information relevant
	 * to a target file.
	 * @param mapping
	 * 			  RepositoryMapping to associate this cache with. The cache's base directory will
	 * 			  be set to the GIT_DIR of this mapping's repository.
	 *
	 */
	public IgnoreCache(RepositoryMapping mapping) {
		super(mapping.getRepository());
	}

	/**
	 * Exposes {@link SimpleIgnoreCache#addIgnoreNode(File)} for public usage.
	 *
	 * @param rsrc
	 * 			  The .gitignore resource that needs to be updated
	 */
	public void refreshNode(IResource rsrc) {
		//TODO: Add a function/secondary clause if rsrc is the exclude file
		addIgnoreNode(new File(rsrc.getParent().getRawLocationURI()));
	}

}
