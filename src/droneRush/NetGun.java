package droneRush;

import battlecode.common.*;

public class NetGun extends Building {

	public NetGun(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {

		while(true) {
			checkTransactions();
			try {
				rc.isLocationOccupied(loc); // Just to give the catch something
				Clock.yield();
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
		}
	}
}
