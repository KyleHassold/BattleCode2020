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
			System.out.println("Error: FulfillmentCenter.FulfillmentCenter() Failed!\nrc.senseRobotAtLocation(...) Failed!");
			e.printStackTrace();
		}
	}

	@Override
	protected void run() {
		while(true) {
			boolean flag = true;
			for(Direction d : directions) {
				MapLocation landscaper = HQs[0].translate(d.dx, d.dy);
				rc.setIndicatorDot(landscaper, 255, 0, 0);
				try {
					if((rc.canSenseLocation(landscaper) && !(rc.senseRobotAtLocation(landscaper) != null && rc.senseRobotAtLocation(landscaper).type == RobotType.LANDSCAPER))) {
						flag = false;
						break;
					}
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
			try {
				if(flag && rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir.rotateLeft()) && rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost + 10) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, dir.rotateLeft());
				}
			} catch(GameActionException e) {
				System.out.println("Error: FulfillmentCenter.run() Failed!\nrc.buildRobot(Delivery Drone, " + dir.rotateLeft() + ") Failed!");
                e.printStackTrace();
			}
			yield();
		}
	}
}
