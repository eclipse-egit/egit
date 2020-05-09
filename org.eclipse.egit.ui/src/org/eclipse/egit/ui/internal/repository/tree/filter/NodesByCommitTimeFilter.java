/*******************************************************************************
 * Copyright (c) 2020, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Filter class holding repository tree nodes filtered by commit time. Currently
 * it supports the filter pattern #n where n is the maximal number nodes to
 * show.
 */
public class NodesByCommitTimeFilter {

	private static final Pattern FILTER_ACTIVE_PATTERN = Pattern
			.compile("#\\d*"); //$NON-NLS-1$

	private final boolean active;

	private final int maxCount;

	private final PriorityQueue<TimedNode> nodes = new PriorityQueue<>(
			Comparator.comparing(TimedNode::getTime));

	private final List<RepositoryTreeNode<?>> allNodes = new ArrayList<>();

	private int thresholdTime = Integer.MIN_VALUE;

	/**
	 * @param filterText
	 *            user defined filter pattern
	 */
	public NodesByCommitTimeFilter(String filterText) {
		boolean doFilter = false;
		int count = -1;
		if (filterText != null) {
			doFilter = FILTER_ACTIVE_PATTERN.matcher(filterText).matches();
			if (doFilter) {
				if (filterText.length() > 1) {
					try {
						count = Integer.parseInt(filterText.substring(1));
					} catch (NumberFormatException e) {
						// ignore - the number must be so large that filtering
						// is not necessary; count stays negative
					}
				}
			}
		}
		active = doFilter;
		maxCount = count;
	}

	/**
	 * @param treeNode
	 *            node to be filtered
	 * @param timeCarrier
	 *            object from which the commit time associated with the node can
	 *            be extracted (currently only RevCommit)
	 */
	public void addNode(RepositoryTreeNode<?> treeNode, Object timeCarrier) {
		if (isFiltering()) {
			if (maxCount > 0) {
				int time = Integer.MIN_VALUE;
				if (timeCarrier instanceof RevCommit) {
					time = ((RevCommit) timeCarrier).getCommitTime();
				}
				if (time >= thresholdTime) {
					TimedNode node = new TimedNode(treeNode, time);
					nodes.add(node);
					if (nodes.size() > maxCount) {
						nodes.poll();
						thresholdTime = nodes.peek().getTime();
					}
				}
			}
		} else {
			allNodes.add(treeNode);
		}
	}

	/**
	 * @return filtered list of added nodes; all if the filter text did not
	 *         activate the filter
	 */
	public List<RepositoryTreeNode<?>> getFilteredNodes() {
		if (isFiltering()) {
			return nodes.stream().map(TimedNode::getNode)
					.collect(Collectors.toList());
		} else {
			return allNodes;
		}
	}

	/**
	 * @return is this filter activated by the filtertext
	 */
	public boolean isFilterActive() {
		return active;
	}

	private boolean isFiltering() {
		return maxCount >= 0; // implies active==true
	}

	private static class TimedNode {

		private final RepositoryTreeNode<?> node;

		private final int time;

		TimedNode(RepositoryTreeNode<?> node, int time) {
			this.node = node;
			this.time = time;
		}

		int getTime() {
			return time;
		}

		RepositoryTreeNode<?> getNode() {
			return node;
		}
	}
}