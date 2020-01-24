package phases2Cleaned;

import battlecode.common.*;

public class FulfillmentCenter extends Robot {

	protected FulfillmentCenter(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		while(true) {
			buildRobot(RobotType.DELIVERY_DRONE, Direction.NORTH);
		}
	}

}
