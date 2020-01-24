package droneRush;

import battlecode.common.*;

public class HQ extends Building {
	int miners = 0;
	boolean doneSensing;

	public HQ(RobotController rc) {
		super(rc);
		HQs[0] = loc;
	}

	@Override
	protected void run() throws GameActionException {
		doneSensing = senseNewSoup(true);
		System.out.println("End: " + Clock.getBytecodesLeft());
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
			System.out.println("Next: " + Clock.getBytecodesLeft());
			if(!doneSensing) {
				doneSensing = senseNewSoup(false);
			}
			System.out.println("End: " + Clock.getBytecodesLeft());
			yield();
		}
		
		Robot netGun = new NetGun(rc);
		netGun.run();
	}
}
