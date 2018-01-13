package cl.uchile.dcc.utils;

public class PredAndDyn implements Comparable<PredAndDyn> {
	private final String pred;
	private final Double dyn;
	
	public PredAndDyn(String pred, Double dyn) {
		super();
		this.pred = pred;
		this.dyn = dyn;
	}
	public String getPred() {
		return pred;
	}
	public Double getDyn() {
		return dyn;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dyn == null) ? 0 : dyn.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PredAndDyn other = (PredAndDyn) obj;
		if (dyn == null) {
			if (other.dyn != null)
				return false;
		} else if (!dyn.equals(other.dyn))
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(PredAndDyn swm) {
		int comp = dyn.compareTo(swm.dyn);
		
		if(comp!=0)
			return comp;
		
		return pred.compareTo(swm.pred);
	}

}
