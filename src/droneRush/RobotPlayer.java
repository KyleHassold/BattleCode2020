package droneRush;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	
	@SuppressWarnings("incomplete-switch")
	public static void run(RobotController rc) {
		
		RobotPlayer.rc = rc;
		Robot minion = null;

		while (minion == null) {
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
		}
		
		minion.run();
	}
}