package droneRush;

import java.util.Collections;

import battlecode.common.*;

public class HQ extends Building {
	int miners = 0;

	public HQ(RobotController rc) {
		super(rc);
		HQs[0] = loc;
	}

	@Override
	protected void run() throws GameActionException {
		System.out.println("Start: " + Clock.getBytecodesLeft());
		MapLocation[] newSoup = rc.senseNearbySoup();
		System.out.println("end: " + Clock.getBytecodesLeft());
		Collections.addAll(soup, newSoup);
		Direction dir = loc.directionTo(center);
		if(!soup.isEmpty()) {
			dir = loc.directionTo(bestSoup(0));
		}
		while(miners < 6) {
			loc = rc.getLocation();
			try {
				if(buildRobot(RobotType.MINER, dir)) {
					miners++;
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
			
			yield();
		}
		
		Robot netGun = new NetGun(rc);
		netGun.run();
	}
}
