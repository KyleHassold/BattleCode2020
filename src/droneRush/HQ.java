package droneRush;

import battlecode.common.*;

public class HQ extends Building {
	int miners = 0;
	boolean doneSensing;
	Direction dir;

	public HQ(RobotController rc) {
		super(rc);
		HQs[0] = loc;
		dir = loc.directionTo(center);
	}

	@Override
	protected void run() {
		// Sense for the surrounding soup
		doneSensing = senseNewSoup(true, false, 5000);
		
		// If there is soup sensed, get the best
		if(!soup.isEmpty()) {
			dir = loc.directionTo(bestSoup(0));
		}
		
		// Spawn in 6 miners
		while(miners < 6) {
			// Build one if possible
			if(buildRobot(RobotType.MINER, dir)) {
				miners++;
			}
			
			// Sense for more soup if there is some left
			if(!doneSensing) {
				doneSensing = senseNewSoup(false, false, 5000);
			}

			yield();
		}
		
		// Become a NetGun after finished spawning in miners
		Robot netGun = new NetGun(rc);
		netGun.run();
	}
}