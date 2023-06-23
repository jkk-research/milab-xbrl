package hu.sze.milab.dust.brain;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;

@SuppressWarnings({ "unchecked" })
public class DustImpl extends Dust {

	interface BrainImpl extends Brain {
	};

	class BootBrain implements Brain {
		@Override
		public MindHandle createHandle() {
			return new DustBrainHandle();
		}
		
		public <RetType> RetType access(Object root, MindAccess cmd, Object val, Object... path) {
			return (RetType) DustException.wrap(null, "Should not call access on boot!");
		}
	};

	BootBrain bootBrain = new BootBrain();

	public static void main(String[] args) {
		DustImpl impl = new DustImpl();

		setBrain(impl.bootBrain);
		
		setBrain(new DustBrain());

		MindHandle h1 = Dust.createHandle();

		Dust.dumpObs(h1);

	}
}
