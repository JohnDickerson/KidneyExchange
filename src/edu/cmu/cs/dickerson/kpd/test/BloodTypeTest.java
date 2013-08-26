package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class BloodTypeTest {

	/**
	 * Tests ABO-model "can blood type X give to blood type Y?"
	 */
	@Test
	public void testGive() {

		// O can give to anyone
		assertTrue(BloodType.O.canGiveTo(BloodType.O));
		assertTrue(BloodType.O.canGiveTo(BloodType.A));
		assertTrue(BloodType.O.canGiveTo(BloodType.B));
		assertTrue(BloodType.O.canGiveTo(BloodType.AB));

		// A can give to {A,AB}
		assertFalse(BloodType.A.canGiveTo(BloodType.O));
		assertTrue(BloodType.A.canGiveTo(BloodType.A));
		assertFalse(BloodType.A.canGiveTo(BloodType.B));
		assertTrue(BloodType.A.canGiveTo(BloodType.AB));

		// B can give to {B,AB}
		assertFalse(BloodType.B.canGiveTo(BloodType.O));
		assertFalse(BloodType.B.canGiveTo(BloodType.A));
		assertTrue(BloodType.B.canGiveTo(BloodType.B));
		assertTrue(BloodType.B.canGiveTo(BloodType.AB));

		// A can give to AB
		assertFalse(BloodType.AB.canGiveTo(BloodType.O));
		assertFalse(BloodType.AB.canGiveTo(BloodType.A));
		assertFalse(BloodType.AB.canGiveTo(BloodType.B));
		assertTrue(BloodType.AB.canGiveTo(BloodType.AB));

	}
	
	/**
	 * Tests ABO-model "can blood type X give to blood type Y?"
	 */
	@Test
	public void testGet() {
		
		// O can only get from O
		assertTrue(BloodType.O.canGetFrom(BloodType.O));
		assertFalse(BloodType.O.canGetFrom(BloodType.A));
		assertFalse(BloodType.O.canGetFrom(BloodType.B));
		assertFalse(BloodType.O.canGetFrom(BloodType.AB));

		// A can get from {O,A}
		assertTrue(BloodType.A.canGetFrom(BloodType.O));
		assertTrue(BloodType.A.canGetFrom(BloodType.A));
		assertFalse(BloodType.A.canGetFrom(BloodType.B));
		assertFalse(BloodType.A.canGetFrom(BloodType.AB));

		// B can get from {O,B}
		assertTrue(BloodType.B.canGetFrom(BloodType.O));
		assertFalse(BloodType.B.canGetFrom(BloodType.A));
		assertTrue(BloodType.B.canGetFrom(BloodType.B));
		assertFalse(BloodType.B.canGetFrom(BloodType.AB));

		// AB can get from anyone
		assertTrue(BloodType.AB.canGetFrom(BloodType.O));
		assertTrue(BloodType.AB.canGetFrom(BloodType.A));
		assertTrue(BloodType.AB.canGetFrom(BloodType.B));
		assertTrue(BloodType.AB.canGetFrom(BloodType.AB));

	}

	@Test
	public void testStringInputs() {
		
		assertEquals(BloodType.O, BloodType.getBloodType("o"));
		assertEquals(BloodType.O, BloodType.getBloodType(" O  \t "));
		assertEquals(BloodType.O, BloodType.getBloodType("O"));
		
		assertEquals(BloodType.A, BloodType.getBloodType("a"));
		assertEquals(BloodType.A, BloodType.getBloodType("A"));
		
		assertEquals(BloodType.B, BloodType.getBloodType("b"));
		assertEquals(BloodType.B, BloodType.getBloodType("B"));
		
		assertEquals(BloodType.AB, BloodType.getBloodType("ab"));
		assertEquals(BloodType.AB, BloodType.getBloodType("AB"));
		
	}
	
}
