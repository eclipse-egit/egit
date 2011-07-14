/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ContentMerger;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.MergeChunk.ConflictState;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.team.core.mapping.DelegatingStorageMerger;
import org.eclipse.team.core.mapping.IStorageMerger;

/**
 * Content merger that will ask available {@link IStorageMerger} and
 * <code>IStreamMerger</code>
 */
public class StorageMerger extends ContentMerger {

	/**
	 * @param db
	 */
	public StorageMerger(Repository db) {
		super(db);
	}

	private class EntryStorage implements IStorage {

		private final CanonicalTreeParser tree;

		private EntryStorage(CanonicalTreeParser tree) {
			this.tree = tree;
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public InputStream getContents() throws CoreException {
			try {
				RawText rawText = getRawText(tree.getEntryObjectId());
				String content = rawText.getString(0, rawText.size(), false);
				return new ByteArrayInputStream(content.getBytes());
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.getPluginId(), CoreText.StorageMerger_failedToGetContentForMerge, e));
			}
		}

		public IPath getFullPath() {
			Path path = new Path(tree.getEntryPathString());
			return path;
		}

		public String getName() {
 			String name = getFullPath().lastSegment();
			return name;
		}

		public boolean isReadOnly() {
			return false;
		}

	}

	@Override
	public MergeResult<RawText> merge(RawTextComparator cmp,
			CanonicalTreeParser base, CanonicalTreeParser ours,
			CanonicalTreeParser theirs) throws IOException {

		IStorageMerger instance = DelegatingStorageMerger.getInstance();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MergeResult<RawText> mergeResult = null;
		try {
			EntryStorage baseStorage = new EntryStorage(base);
			RawText rawText = getRawText(base.getEntryObjectId());
			Charset encoding = RawParseUtils.parseEncoding(rawText.getString(0, rawText.size(), true).getBytes());
			IStatus merge = instance.merge(output, encoding.name(), baseStorage,
					new EntryStorage(ours), new EntryStorage(theirs), null);
			if(merge.isOK()) {
				RawText mergedText = new RawText(output
						.toByteArray());
				List<RawText> sequences = Collections.singletonList(mergedText);

				mergeResult = new MergeResult<RawText>(sequences);
				mergeResult.add(0, 0, mergedText.size(), ConflictState.NO_CONFLICT);
			} else {
				return null;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return mergeResult;

	}

	private RawText getRawText(ObjectId id)
			throws IOException {
		if (id.equals(ObjectId.zeroId()))
			return new RawText(new byte[] {});
		return new RawText(db.open(id, Constants.OBJ_BLOB).getCachedBytes());
	}

}
