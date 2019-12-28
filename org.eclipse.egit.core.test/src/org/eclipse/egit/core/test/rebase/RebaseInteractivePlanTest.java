/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test.rebase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.JoinedList;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.MoveHelper;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RebaseInteractivePlanTest extends GitTestCase {

	private RebaseInteractivePlan plan;

	private ArrayList<PlanElement> toDoElements;

	private MoveHelper moveHelper;

	private TestRepository testRepository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
		plan = RebaseInteractivePlan.getPlan(testRepository.getRepository());
		toDoElements = new ArrayList<>();
		moveHelper = new RebaseInteractivePlan.MoveHelper(toDoElements, plan);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		plan.dispose();
		testRepository.dispose();
		super.tearDown();
	}


	@Test
	public void parseTest() {
		// how to mock repository?
	}

	@Test
	public void persistTest() {
		// how to mock repository?
	}

	@SuppressWarnings("boxing")
	private PlanElement createPlanElement(boolean isComment) {
		PlanElement element1 = Mockito.mock(PlanElement.class);
		Mockito.when(element1.isComment()).thenReturn(isComment);
		return element1;
	}

	@Test
	public void moveUpTestOneElement() throws Exception {
		PlanElement element1 = createPlanElement(false);
		toDoElements.add(element1);
		moveHelper.moveTodoEntryUp(element1);
		assertEquals(element1, toDoElements.get(0));
	}

	@Test
	public void moveUpTestTwoElements() throws Exception {
		PlanElement element1 = createPlanElement(false);
		PlanElement element2 = createPlanElement(false);
		toDoElements.add(element1);
		toDoElements.add(element2);
		moveHelper.moveTodoEntryUp(element2);
		assertEquals(element2, toDoElements.get(0));
		assertEquals(element1, toDoElements.get(1));
		moveHelper.moveTodoEntryUp(element2);
		assertEquals(element2, toDoElements.get(0));
		assertEquals(element1, toDoElements.get(1));
		moveHelper.moveTodoEntryUp(element1);
		assertEquals(element1, toDoElements.get(0));
		assertEquals(element2, toDoElements.get(1));
	}

	@Test
	public void moveUpTestThreeElementsWithOneComment() throws Exception {
		PlanElement element1 = createPlanElement(false);
		PlanElement element2 = createPlanElement(true);
		PlanElement element3 = createPlanElement(false);
		toDoElements.add(element1);
		toDoElements.add(element2);
		toDoElements.add(element3);
		moveHelper.moveTodoEntryUp(element3);
		assertEquals(element3, toDoElements.get(0));
		assertEquals(element1, toDoElements.get(1));
		assertEquals(element2, toDoElements.get(2));
	}

	@Test
	public void moveDownTestOneElement() throws Exception {
		PlanElement element1 = createPlanElement(false);
		toDoElements.add(element1);
		moveHelper.moveTodoEntryDown(element1);
		assertEquals(element1, toDoElements.get(0));
	}

	@Test
	public void moveDownTestTwoElements() throws Exception {
		PlanElement element1 = createPlanElement(false);
		PlanElement element2 = createPlanElement(false);
		toDoElements.add(element1);
		toDoElements.add(element2);
		moveHelper.moveTodoEntryDown(element1);
		assertEquals(element2, toDoElements.get(0));
		assertEquals(element1, toDoElements.get(1));
		moveHelper.moveTodoEntryDown(element1);
		assertEquals(element2, toDoElements.get(0));
		assertEquals(element1, toDoElements.get(1));
		moveHelper.moveTodoEntryDown(element2);
		assertEquals(element1, toDoElements.get(0));
		assertEquals(element2, toDoElements.get(1));
	}

	@Test
	public void moveDownTestThreeElementsWithOneComment() throws Exception {
		PlanElement element1 = createPlanElement(false);
		PlanElement element2 = createPlanElement(true);
		PlanElement element3 = createPlanElement(false);
		toDoElements.add(element1);
		toDoElements.add(element2);
		toDoElements.add(element3);
		moveHelper.moveTodoEntryDown(element1);
		assertEquals(element2, toDoElements.get(0));
		assertEquals(element3, toDoElements.get(1));
		assertEquals(element1, toDoElements.get(2));
	}

	public static class JoinedListTest {
		private RebaseInteractivePlan.JoinedList<List<Integer>, Integer> joined;

		private List<Integer> testData1;

		private List<Integer> testData2;

		@Before
		public void beforeTest() {
			testData1 = new LinkedList<>();
			for (int i = 1; i <= 4; i++) {
				testData1.add(Integer.valueOf(i));
			}
			testData2 = new LinkedList<>();
			for (int i = 5; i <= 10; i++) {
				testData2.add(Integer.valueOf(i));
			}
			joined = JoinedList.wrap(testData1, testData2);
		}

		@Test
		public void addTest() {
			joined.add(Integer.valueOf(11));
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10, 11]", joined.getSecondList()
					.toString());
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]",
					joined.toString());

			joined.add(Integer.valueOf(12));
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10, 11, 12]", joined.getSecondList()
					.toString());
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]",
					joined.toString());
		}

		@Test
		public void addToIndexTest() {
			joined.add(0, Integer.valueOf(0));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(5, Integer.valueOf(-5));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[-5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(5, Integer.valueOf(-5));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[-5, -5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, -5, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(4, Integer.valueOf(-4));
			assertEquals("[0, 1, 2, 3, -4, 4]", joined.getFirstList()
					.toString());
			assertEquals("[-5, -5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, -4, 4, -5, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(14, Integer.valueOf(11));
			assertEquals("[0, 1, 2, 3, -4, 4]", joined.getFirstList()
					.toString());
			assertEquals("[-5, -5, 5, 6, 7, 8, 9, 10, 11]", joined
					.getSecondList().toString());
			assertEquals("[0, 1, 2, 3, -4, 4, -5, -5, 5, 6, 7, 8, 9, 10, 11]",
					joined.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void addAllTest() {
			joined.addAll(Arrays.asList(11, 12, 13));
			assertEquals("[5, 6, 7, 8, 9, 10, 11, 12, 13]", joined
					.getSecondList().toString());
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13]",
					joined.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void addAllToIndexInFirstListTest() {
			joined.addAll(3, Arrays.asList(-4, -44, -44));
			assertEquals("[1, 2, 3, -4, -44, -44, 4]", joined.getFirstList()
					.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void addAllToIndexInSecondeListTest() {
			joined.addAll(4, Arrays.asList(-5, -55, -55));
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[-5, -55, -55, 5, 6, 7, 8, 9, 10]", joined
					.getSecondList().toString());
		}

		@Test
		public void clearTest() {
			joined.clear();
			assertTrue(testData1.isEmpty());
			assertTrue(testData2.isEmpty());
		}

		@Test
		public void containsTest() {
			Integer containedInFirst = Integer.valueOf(4);
			Integer containedInSecond = Integer.valueOf(6);
			Integer notContained = Integer.valueOf(-1);

			assertTrue(joined.contains(containedInFirst));
			assertTrue(joined.contains(containedInSecond));
			assertFalse(joined.contains(notContained));
			checkOriginalDataHasNotBeenAltered();
		}

		@SuppressWarnings("boxing")
		@Test
		public void containsAllTest() {
			List<Integer> containedInFirst = Arrays.asList(1, 4);
			List<Integer> containedInSecond = Arrays.asList(5, 10);
			List<Integer> containedBoth = Arrays.asList(1, 4, 5, 10);
			List<Integer> notContained = Arrays.asList(1, 4, 5, 10, -1);

			assertTrue(joined.containsAll(containedInFirst));
			assertTrue(joined.containsAll(containedInSecond));
			assertTrue(joined.containsAll(containedBoth));
			assertFalse(joined.containsAll(notContained));
			checkOriginalDataHasNotBeenAltered();
		}

		@Test
		public void getTest() {
			assertEquals(Integer.valueOf(1), joined.get(0));
			assertEquals(Integer.valueOf(1), joined.getFirstList().get(0));
			assertEquals(Integer.valueOf(10), joined.get(9));
			assertEquals(Integer.valueOf(10), joined.getSecondList().get(5));

			Integer toBeAdded = Integer.valueOf(-5);
			joined.getSecondList().add(0, toBeAdded);
			assertEquals(toBeAdded, joined.get(4));
		}

		@Test
		public void isEmptyTest() {
			testData1.clear();
			testData2.clear();
			assertTrue(joined.isEmpty());
			assertEquals(0, joined.size());
		}

		@Test
		public void indexOfTest() {
			Integer lastInSecond = Integer.valueOf(10);
			Integer firstInSecond = Integer.valueOf(5);
			Integer lastInFirst = Integer.valueOf(4);
			Integer firstInFirst = Integer.valueOf(1);

			testData1.addAll(testData1);
			testData2.addAll(testData2);
			// 1-2-3-4-1-2-3-4-5-6-7-8-9-10-5-6-7-8-9-10 --list
			// 0-1-2-3-4-5-6-7-8-9-0-1-2-.3-4-5-6-7-8-.9 --index

			assertEquals(13, joined.indexOf(lastInSecond));
			assertEquals(8, joined.indexOf(firstInSecond));
			assertEquals(3, joined.indexOf(lastInFirst));
			assertEquals(0, joined.indexOf(firstInFirst));
		}

		@Test
		public void lastIndexOfTest() {
			Integer lastInSecond = Integer.valueOf(10);
			Integer firstInSecond = Integer.valueOf(5);
			Integer lastInFirst = Integer.valueOf(4);
			Integer firstInFirst = Integer.valueOf(1);

			testData1.addAll(testData1);
			testData2.addAll(testData2);
			// 1-2-3-4-1-2-3-4-5-6-7-8-9-10-5-6-7-8-9-10 --list
			// 0-1-2-3-4-5-6-7-8-9-0-1-2-.3-4-5-6-7-8-.9 --index

			assertEquals(19, joined.lastIndexOf(lastInSecond));
			assertEquals(14, joined.lastIndexOf(firstInSecond));
			assertEquals(7, joined.lastIndexOf(lastInFirst));
			assertEquals(4, joined.lastIndexOf(firstInFirst));
		}

		@Test
		public void listIteratorTest() {
			ListIterator<Integer> litr = joined.listIterator(3);
			assertEquals(2, litr.previousIndex());
			assertEquals(3, litr.nextIndex());

			Integer previous = litr.previous();
			litr.next();
			Integer next = litr.next();

			assertEquals(next, litr.previous());
			assertEquals(previous, litr.previous());
		}

		@Test
		public void removeElementTest() {
			joined.remove(Integer.valueOf(1));
			joined.remove(Integer.valueOf(6));
			joined.remove(Integer.valueOf(10));

			assertEquals("[2, 3, 4, 5, 7, 8, 9]", joined.toString());
			assertEquals("[2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 7, 8, 9]", joined.getSecondList().toString());
		}

		@Test
		public void removeIndexTest() {
			joined.remove(9);
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9]", joined.toString());
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9]", joined.getSecondList().toString());

			joined.remove(3);
			assertEquals("[1, 2, 3, 5, 6, 7, 8, 9]", joined.toString());
			assertEquals("[1, 2, 3]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9]", joined.getSecondList().toString());

			joined.remove(3);
			assertEquals("[1, 2, 3, 6, 7, 8, 9]", joined.toString());
			assertEquals("[1, 2, 3]", joined.getFirstList().toString());
			assertEquals("[6, 7, 8, 9]", joined.getSecondList().toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void removeAllTest() {
			joined.removeAll(Arrays.asList(2, 3, 6));
			assertEquals("[1, 4, 5, 7, 8, 9, 10]", joined.toString());
			assertEquals("[1, 4]", joined.getFirstList().toString());
			assertEquals("[5, 7, 8, 9, 10]", joined.getSecondList().toString());
		}


		@Test
		public void setTest() {
			joined.set(5, Integer.valueOf(-6));
			joined.set(4, Integer.valueOf(-5));
			joined.set(3, Integer.valueOf(-4));
			assertEquals("[1, 2, 3, -4, -5, -6, 7, 8, 9, 10]",
					joined.toString());
			assertEquals("[1, 2, 3, -4]", joined.getFirstList().toString());
			assertEquals("[-5, -6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
		}

		private void checkOriginalDataHasNotBeenAltered() {
			assertEquals("[1, 2, 3, 4]", testData1.toString());
			assertEquals("[5, 6, 7, 8, 9, 10]", testData2.toString());
		}

		@Test
		public void sizeTest() {
			assertEquals(testData1.size() + testData2.size(), joined.size());
			checkOriginalDataHasNotBeenAltered();
		}

		@Test
		public void subListTest() {
			List<Integer> subList1 = joined.subList(0, 4);
			assertEquals("[1, 2, 3, 4]", subList1.toString());
			List<Integer> subList2 = joined.subList(4, 10);
			assertEquals("[5, 6, 7, 8, 9, 10]", subList2.toString());
			List<Integer> subList3 = joined.subList(3, 7);
			assertEquals("[4, 5, 6, 7]", subList3.toString());
			checkOriginalDataHasNotBeenAltered();
		}

		@SuppressWarnings("boxing")
		@Test
		public void toArrayTest() {
			assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
					joined.toArray());
		}
	}
}
