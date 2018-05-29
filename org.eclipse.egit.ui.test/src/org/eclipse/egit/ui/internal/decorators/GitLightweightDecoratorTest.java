/*******************************************************************************
 * Copyright (c) 2017 EclipseSource Services GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Martin Fleck - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import static org.eclipse.egit.ui.internal.decorators.DecoratableResourceMapping.RESOURCE_MAPPING;
import static org.eclipse.egit.ui.internal.decorators.DecoratableWorkingSet.WORKING_SET;
import static org.eclipse.jgit.junit.JGitTestUtil.write;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkingSet;
import org.junit.BeforeClass;

/**
 * An abstract base class providing several convenience methods for tests
 * involving the {@link GitLightweightDecorator}.
 *
 * @author Martin Fleck <mfleck@eclipsesource.com>
 */
public abstract class GitLightweightDecoratorTest
		extends LocalRepositoryTestCase {

	protected static final GitLightweightDecorator DECORATOR = new GitLightweightDecorator();

	@BeforeClass
	public static void showAllIcons() throws Exception {
		// ensure that all images are shown
		IPreferenceStore preferenceStore = Activator.getDefault()
				.getPreferenceStore();
		preferenceStore.putValue(
				UIPreferences.DECORATOR_SHOW_ASSUME_UNCHANGED_ICON,
				Boolean.toString(true));
		preferenceStore.putValue(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON,
				Boolean.toString(true));
		preferenceStore.putValue(UIPreferences.DECORATOR_SHOW_DIRTY_ICON,
				Boolean.toString(true));
		preferenceStore.putValue(UIPreferences.DECORATOR_SHOW_STAGED_ICON,
				Boolean.toString(true));
		preferenceStore.putValue(UIPreferences.DECORATOR_SHOW_TRACKED_ICON,
				Boolean.toString(true));
		preferenceStore.putValue(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON,
				Boolean.toString(true));
	}

	protected static void assertUndecorated(ResourceMapping resourceMapping)
			throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertUndecorated(decoration);
	}

	protected static void assertDecorationTracked(
			ResourceMapping resourceMapping) throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertTracked(decoration);
	}

	protected static void assertDecorationUntracked(
			ResourceMapping resourceMapping) throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertUntracked(decoration);
	}

	protected static void assertDecorationDirty(ResourceMapping resourceMapping)
			throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertDirty(decoration);
	}

	protected static void assertDecorationConflicts(
			ResourceMapping resourceMapping) throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertConflict(decoration);
	}

	protected static void assertDecorationStaged(
			ResourceMapping resourceMapping) throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertStaged(decoration);
	}

	protected static void assertDecorationAdded(ResourceMapping resourceMapping)
			throws Exception {
		DecorationResult decoration = decorate(resourceMapping);
		assertStagedAdd(decoration);
	}

	protected static DecorationResult decorate(
			ResourceMapping resourceMapping) {
		DecorationResult result = new DecorationResult();
		DECORATOR.decorate(resourceMapping, result);
		return result;
	}

	protected static void assertTracked(DecorationResult decoration) {
		assertTrue("Image does not reflect 'tracked' state.",
				isTracked(decoration));
	}

	protected static void assertUntracked(DecorationResult decoration) {
		assertTrue("Image does not reflect 'untracked' state.",
				isUntracked(decoration));
	}

	protected static void assertStaged(DecorationResult decoration) {
		assertTrue("Image does not reflect 'staged' state: "
				+ decoration.getOverlay(), isStaged(decoration));
	}

	protected static void assertStagedAdd(DecorationResult decoration) {
		assertTrue("Image does not reflect 'staged add' state.",
				isStagedAdd(decoration));
	}

	protected static void assertStagedRemove(DecorationResult decoration) {
		assertTrue("Image does not reflect 'staged remove' state.",
				isStagedRemove(decoration));
	}

	protected static void assertConflict(DecorationResult decoration) {
		assertTrue("Image does not reflect 'conflict' state.",
				isConflict(decoration));
	}

	protected static void assertAssumeUnchaged(DecorationResult decoration) {
		assertTrue("Image does not reflect 'assume unchaged' state.",
				isAssumeUnchanged(decoration));
	}

	protected static void assertDirty(DecorationResult decoration) {
		assertTrue("Image does not reflect 'dirty' state.",
				isDirty(decoration));
	}

	protected static void assertUndecorated(DecorationResult decoration) {
		assertTrue("Image is not undecorated.", isUndecorated(decoration));
	}

	protected static boolean isTracked(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.TRACKED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isUntracked(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.UNTRACKED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isStaged(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.STAGED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isStagedAdd(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.STAGED_ADDED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isStagedRemove(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.STAGED_REMOVED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isConflict(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.CONFLICT_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isAssumeUnchanged(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.ASSUME_UNCHANGED_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isDirty(DecorationResult decoration) {
		return Objects.equals(
				GitLightweightDecorator.DecorationHelper.DIRTY_IMAGE,
				decoration.getOverlay());
	}

	protected static boolean isUndecorated(DecorationResult decoration) {
		return decoration.getOverlay() == null;
	}

	protected static IFile createFile(IContainer container, String name,
			String content) throws IOException, CoreException {
		write(new File(container.getLocation().toFile(), name), content);
		container.refreshLocal(IResource.DEPTH_INFINITE, null);
		return findFile(container, name);
	}

	protected static IResource findResource(IContainer container, String name) {
		return container.findMember(name);
	}

	protected static IFile findFile(IContainer container, String name) {
		IResource resource = findResource(container, name);
		return AdapterUtils.adapt(resource, IFile.class);
	}

	protected static RevCommit gitCommit(Git git) throws Exception {
		return git.commit().setMessage("Commit staged changes.").call();
	}

	protected static void gitAdd(Git git, IFile file) throws Exception {
		String filePath = file.getFullPath().toString();
		// remove leading '/'
		git.add().addFilepattern(filePath.substring(1)).call();
	}

	protected static void gitRemove(Git git, IFile file) throws Exception {
		String filePath = file.getFullPath().toString();
		// remove leading '/'
		git.rm().addFilepattern(filePath.substring(1)).call();
	}

	protected static TestDecoratableResource newExpectedDecoratableResource(
			IResource resource) {
		return new TestDecoratableResource(resource);
	}

	protected static TestDecoratableResource newExpectedDecoratableResourceMapping() {
		return new TestDecoratableResource("<unknown>", RESOURCE_MAPPING);
	}

	protected static TestDecoratableResource newExpectedDecoratableWorkingSet(
			IWorkingSet workingSet) {
		return new TestDecoratableResource(workingSet.getLabel(), WORKING_SET);
	}

	protected static DecoratableResource newDecoratableResource(
			IndexDiffData indexDiffData, IResource resource)
			throws IOException {
		return new DecoratableResourceAdapter(indexDiffData, resource);
	}

	protected static DecoratableResource newDecoratableResourceMapping(
			ResourceMapping resourceMapping) throws IOException {
		return new DecoratableResourceMapping(resourceMapping);
	}

	protected static DecoratableResource newDecoratableWorkingSet(
			ResourceMapping resourceMapping) throws IOException {
		return new DecoratableWorkingSet(resourceMapping);
	}
}
