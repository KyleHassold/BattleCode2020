package phases2Cleaned;

import battlecode.common.*;

public class NetGun extends Robot {

	protected NetGun(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void run() throws GameActionException {
		RobotInfo[] robots;
		while(true) {
			robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam());
			for(RobotInfo robo : robots) {
				if(robo.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robo.ID)) {
					rc.shootUnit(robo.ID);
					break;
				}
			}
			
			Clock.yield();
		}
	}

}
