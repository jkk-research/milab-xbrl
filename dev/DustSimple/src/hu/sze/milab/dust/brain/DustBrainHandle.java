package hu.sze.milab.dust.brain;

import hu.sze.milab.dust.DustConsts.MindHandle;

public class DustBrainHandle implements MindHandle {
	
	@Override
	public String toString() {
		return DustBrain.handleToString(this);
	}
}
