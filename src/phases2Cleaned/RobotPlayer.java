package phases2Cleaned;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	
	@SuppressWarnings("incomplete-switch")
	public static void run(RobotController rc) throws GameActionException {
		
		RobotPlayer.rc = rc;
		Robot minion = null;

		while (minion == null) {
			try {
				switch (rc.getType()) {
				case HQ:                 minion = new HQ(rc);					break;
				case MINER:              minion = new Miner(rc);				break;
				case LANDSCAPER:         minion = new Landscaper(rc);			break;
				case DELIVERY_DRONE:     minion = new Drone(rc);				break;
				case REFINERY:           minion = new Refinery(rc);				break;
				case VAPORATOR:          minion = new Vaporator(rc);			break;
				case DESIGN_SCHOOL:      minion = new DesignSchool(rc);			break;
				case FULFILLMENT_CENTER: minion = new FulfillmentCenter(rc);	break;
				case NET_GUN:            minion = new NetGun(rc);				break;
				}

				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
		
		minion.run();
	}
}