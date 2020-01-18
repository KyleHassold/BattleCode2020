package droneRush;

import battlecode.common.*;

public class FulfillmentCenter extends Building {
	Direction dir;

	public FulfillmentCenter(RobotController rc) {
		super(rc);
		try {
			if(rc.canSenseLocation(new MapLocation(loc.x + 2, loc.y + 2)) && rc.senseRobotAtLocation(new MapLocation(loc.x + 2, loc.y + 2)) != null && rc.senseRobotAtLocation(new MapLocation(loc.x + 2, loc.y + 2)).type == RobotType.HQ) {
				dir = Direction.NORTHEAST;
			} else if(rc.canSenseLocation(new MapLocation(loc.x + 2, loc.y - 2)) && rc.senseRobotAtLocation(new MapLocation(loc.x + 2, loc.y - 2)) != null  && rc.senseRobotAtLocation(new MapLocation(loc.x + 2, loc.y - 2)).type == RobotType.HQ) {
				dir = Direction.SOUTHEAST;
			} else if(rc.canSenseLocation(new MapLocation(loc.x - 2, loc.y + 2)) && rc.senseRobotAtLocation(new MapLocation(loc.x - 2, loc.y + 2)) != null  && rc.senseRobotAtLocation(new MapLocation(loc.x - 2, loc.y + 2)).type == RobotType.HQ) {
				dir = Direction.NORTHWEST;
			} else {
				dir = Direction.SOUTHWEST;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void run() throws GameActionException {
		int drones = 0;
		while(true) {
			try {
				if(drones < 1 && rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir.rotateLeft())) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, dir.rotateLeft());
					drones++;
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
			yield();
		}
	}
}
