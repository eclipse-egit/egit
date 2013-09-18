/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test.rebase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.JoinedList;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ReversedList;
import org.junit.Before;
import org.junit.Test;

public class RebaseInteractivePlanTest {


	@Test
	public void parseTest() {
		// how to mock repository?
	}

	@Test
	public void persistTest() {
		// how to mock repository?
	}

	@Test
	public void moveUpTest() {
		// how to mock repository?
	}

	@Test
	public void moveDownTest() {
		// how to mock repository?
	}



	public static class ReversedListTest {
		private List<Integer> testData;

		private ReversedList<List<Integer>, Integer> reversedble;

		@SuppressWarnings("boxing")
		@Before
		public void beforeTest() {
			testData = new LinkedList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5,
					6, 7, 8, 9));
			reversedble = ReversedList.wrap(testData);
		}

		@Test
		public void testReverse() {
			assertEquals("[9, 8, 7, 6, 5, 4, 3, 2, 1, 0]",
					reversedble.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testAdd() {
			testData.add(10);
			reversedble.add(-1);
			assertEquals("[10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1]",
					reversedble.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testAddIndex() {
			testData.add(0, -1);
			assertEquals("[-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9]",
					testData.toString());
			reversedble.add(0, 10);
			assertEquals("[10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1]",
					reversedble.toString());
			reversedble.add(5, -5);
			assertEquals("[10, 9, 8, 7, 6, -5, 5, 4, 3, 2, 1, 0, -1]",
					reversedble.toString());
			assertEquals("[-1, 0, 1, 2, 3, 4, 5, -5, 6, 7, 8, 9, 10]",
					testData.toString());
			testData.add(5, -4);
			List<Integer> copyDoupleReversed = new ArrayList<Integer>(
					reversedble);
			Collections.reverse(copyDoupleReversed);
			assertEquals("[-1, 0, 1, 2, 3, -4, 4, 5, -5, 6, 7, 8, 9, 10]",
					copyDoupleReversed.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testGet() {
			testData.add(10);
			assertEquals(new Integer(9), testData.get(9));
			assertEquals(new Integer(5), testData.get(5));
			assertEquals(new Integer(0), testData.get(0));
			assertEquals(new Integer(10), testData.get(10));
			assertEquals(new Integer(10), reversedble.get(0));
			assertEquals(new Integer(0), reversedble.get(10));
			assertEquals(new Integer(4), reversedble.get(6));
			assertEquals(new Integer(9), reversedble.get(1));
		}

		@SuppressWarnings("boxing")
		@Test
		public void testSize() {
			assertEquals(testData.size(), reversedble.size());
			testData.add(10);
			assertEquals(testData.size(), reversedble.size());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testAddAll() {
			testData.addAll(Arrays.asList(10, 11, 12));
			assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]",
					testData.toString());
			assertEquals("[12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0]",
					reversedble.toString());
			reversedble.addAll(Arrays.asList(-1, -2, -3));
			assertEquals(
					"[12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3]",
					reversedble.toString());
			assertEquals(
					"[-3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]",
					testData.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testAddAllIndex() {
			testData.addAll(4, Arrays.asList(-4, -44, -44));
			assertEquals("[0, 1, 2, 3, -4, -44, -44, 4, 5, 6, 7, 8, 9]",
					testData.toString());
			assertEquals("[9, 8, 7, 6, 5, 4, -44, -44, -4, 3, 2, 1, 0]",
					reversedble.toString());

			beforeTest();

			reversedble.addAll(4, Arrays.asList(-6, -66, -66));
			assertEquals("[9, 8, 7, 6, -6, -66, -66, 5, 4, 3, 2, 1, 0]",
					reversedble.toString());
		}

		@SuppressWarnings("boxing")
		@Test
		public void testSet() {
			testData.set(0, -99);
			assertEquals(new Integer(-99), testData.get(0));
			assertEquals(new Integer(-99), reversedble.get(9));
			reversedble.set(0, -66);
			assertEquals(new Integer(-66), reversedble.get(0));
			assertEquals(new Integer(-66), testData.get(9));
		}

		@Test
		public void testSubList() {
			List<Integer> subList = reversedble.subList(2, 5);
			List<Integer> expected = new LinkedList<Integer>(testData.subList(
					5, 8));
			Collections.reverse(expected);
			assertEquals(expected, subList);
		}

		@Test
		public void testListIteratorIndex() {
			ListIterator<Integer> litrReversed = reversedble.listIterator(3);
			ListIterator<Integer> litrTestData = testData.listIterator(7);

			assertEquals(2, litrReversed.previousIndex());
			assertEquals(3, litrReversed.nextIndex());

			assertEquals(litrReversed.next(), litrTestData.previous());
			assertEquals(litrReversed.previous(), litrTestData.next());
			assertEquals(litrReversed.previous(), litrTestData.next());

		}

		@SuppressWarnings("boxing")
		@Test
		public void testToArray() {
			Integer[] expected2 = new Integer[] { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
			assertArrayEquals(expected2, reversedble.toArray());
		}

	}

	public static class JoinedListTest {
		private RebaseInteractivePlan.JoinedList<List<Integer>, Integer> joined;

		private List<Integer> testData1;

		private List<Integer> testData2;

		@Before
		public void beforeTest() {
			testData1 = new LinkedList<Integer>();
			for (int i = 1; i <= 4; i++) {
				testData1.add(new Integer(i));
			}
			testData2 = new LinkedList<Integer>();
			for (int i = 5; i <= 10; i++) {
				testData2.add(new Integer(i));
			}
			joined = JoinedList.wrap(testData1, testData2);
		}

		@Test
		public void addTest() {
			joined.add(new Integer(11));
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10, 11]", joined.getSecondList()
					.toString());
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]",
					joined.toString());

			joined.add(new Integer(12));
			assertEquals("[1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10, 11, 12]", joined.getSecondList()
					.toString());
			assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]",
					joined.toString());
		}

		@Test
		public void addToIndexTest() {
			joined.add(0, new Integer(0));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(5, new Integer(-5));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[-5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(5, new Integer(-5));
			assertEquals("[0, 1, 2, 3, 4]", joined.getFirstList().toString());
			assertEquals("[-5, -5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, 4, -5, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(4, new Integer(-4));
			assertEquals("[0, 1, 2, 3, -4, 4]", joined.getFirstList()
					.toString());
			assertEquals("[-5, -5, 5, 6, 7, 8, 9, 10]", joined.getSecondList()
					.toString());
			assertEquals("[0, 1, 2, 3, -4, 4, -5, -5, 5, 6, 7, 8, 9, 10]",
					joined.toString());

			joined.add(14, new Integer(11));
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
			Integer containedInFirst = new Integer(4);
			Integer containedInSecond = new Integer(6);
			Integer notContained = new Integer(-1);

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
			assertEquals(new Integer(1), joined.get(0));
			assertEquals(new Integer(1), joined.getFirstList().get(0));
			assertEquals(new Integer(10), joined.get(9));
			assertEquals(new Integer(10), joined.getSecondList().get(5));

			Integer toBeAdded = new Integer(-5);
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
			Integer lastInSecond = new Integer(10);
			Integer firstInSecond = new Integer(5);
			Integer lastInFirst = new Integer(4);
			Integer firstInFirst = new Integer(1);

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
			Integer lastInSecond = new Integer(10);
			Integer firstInSecond = new Integer(5);
			Integer lastInFirst = new Integer(4);
			Integer firstInFirst = new Integer(1);

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
			joined.remove(new Integer(1));
			joined.remove(new Integer(6));
			joined.remove(new Integer(10));

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
			joined.set(5, new Integer(-6));
			joined.set(4, new Integer(-5));
			joined.set(3, new Integer(-4));
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

		@Test
		public void toArrayTest() {

		}
	}
}
