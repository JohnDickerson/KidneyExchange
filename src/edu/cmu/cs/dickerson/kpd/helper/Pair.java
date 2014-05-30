package edu.cmu.cs.dickerson.kpd.helper;

// From http://stackoverflow.com/questions/521171/a-java-collection-of-value-pairs-tuples
// From http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java
public class Pair<L,R> {

	private final L left;
	private final R right;
	
	/**
	 * Returns a Pair tuple (left, right)
	 * 
	 * @param left
	 * @param right
	 */
	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Gets the left element of tuple (left, right)
	 * @return left element
	 */
	public L getLeft() { return left; }
	/**
	 * Gets the right element of tuple (left, right)
	 * @return right element
	 */
	public R getRight() { return right; }

	public int hashCode() {
		int hashFirst = left != null ? left.hashCode() : 0;
		int hashSecond = right != null ? right.hashCode() : 0;
		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Pair)) return false;
		
		@SuppressWarnings("unchecked")
		Pair<L,R> pairo = (Pair<L,R>) o;
		return this.left.equals(pairo.getLeft()) &&
				this.right.equals(pairo.getRight());
	}

	@Override
	public String toString() {
		return "Pair [L=" + left + ", R=" + right + "]";
	}
}