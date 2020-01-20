package droneRush;

import battlecode.common.*;

public class NetGun extends Building {

	public NetGun(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		checkTransactions();

		while(true) {
			try {
				RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
				for(RobotInfo robo : robots) {
					if(robo.type == RobotType.DELIVERY_DRONE && rc.canShootUnit(robo.ID)) {
						rc.shootUnit(robo.ID);
						break;
					}
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
			yield();
		}
	}
}
