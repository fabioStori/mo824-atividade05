package solutions;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class Solution<E> extends ArrayList<E> {
	
	public Double cost = Double.POSITIVE_INFINITY;
	public Double usedCapacity = 0.0;
	
	public Solution() {
		super();
	}
	
	public Solution(Solution<E> sol) {
		super(sol);
		cost = sol.cost;
		usedCapacity = sol.usedCapacity;
	}

	@Override
	public String toString() {
		return "Solution: cost=[" + cost + "], capacity=[" + usedCapacity + "], size=[" + this.size() + "], elements=" + super.toString();
	}

}

