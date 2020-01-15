package phases2Cleaned;

import battlecode.common.*;

public class HQ extends Building {

	protected HQ(RobotController rc) {
		super(rc);
		HQs[0] = rc.getLocation();
	}

	@Override
	protected void run() throws GameActionException {
		runHQInit();
		
		while(rc.getRobotCount() < 9) {
			buildRobot(RobotType.MINER, directions.get(0));

			map.putAll(senseInRange());
			
			Clock.yield();
		}
		
		Robot gun = new NetGun(rc);
		gun.run(); // Become a net gun
	}
	
	private void runHQInit() throws GameActionException {
		senseStopLoc = new MapLocation(0,0);

		MapLocation enemyHQGuess = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, HQs[0].y);
		Direction spawnDir = getDirection(HQs[0], enemyHQGuess);
		buildRobot(RobotType.MINER, spawnDir);

		map.putAll(senseInRange());

		Clock.yield();

		enemyHQGuess = new MapLocation(HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		spawnDir = getDirection(HQs[0], enemyHQGuess);
		buildRobot(RobotType.MINER, spawnDir);

		map.putAll(senseInRange());

		Clock.yield();

		enemyHQGuess = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		spawnDir = getDirection(HQs[0], enemyHQGuess);
		while(!buildRobot(RobotType.MINER, spawnDir)) {
			map.putAll(senseInRange());

			Clock.yield();
		}
	}
}
