package droneRush;

import battlecode.common.*;

public class NetGun extends Building {

	public NetGun(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() {
		checkTransactions();

		while(true) {
			RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
			for(RobotInfo robo : robots) {
				try {
					if(robo.type == RobotType.DELIVERY_DRONE && rc.canShootUnit(robo.ID)) {
						rc.setIndicatorLine(loc, robo.location, 120, 50, 50);
						rc.shootUnit(robo.ID);
						break;
					}
				} catch(GameActionException e) {
					System.out.println("Error: NetGun.run() Failed!\nrc.shootUnit(" + robo.ID + ") Failed!");
					e.printStackTrace();
				}
			}
			yield();
		}
	}
}