package phases2Cleaned;

import battlecode.common.*;

public class SoupMiner extends Miner {
	boolean buildVap;

	protected SoupMiner(RobotController rc) throws GameActionException {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		while(true) {
			Clock.yield();
		}
	}

}
