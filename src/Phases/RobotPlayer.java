package Phases;

import java.util.HashMap;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	static Direction[] directions = {Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST};
	static int dirOffset = 0;
	
	static HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();
	static boolean enemyBaseFound = false;

	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

		///// Reorder directions based on starting location /////
		MapLocation startLoc = rc.getLocation();
		double angle = Math.atan2(rc.getMapHeight()/2 - startLoc.y, rc.getMapWidth()/2 - startLoc.x) + Math.PI;

		if(angle <= Math.PI * 15/8 && angle > Math.PI * 1/8) {
			dirOffset = 8 - (int) (4*angle/Math.PI + 0.5);
			Direction[] temp = new Direction[8];
			for(int i = 0; i < 8; i++) {
				temp[i] = directions[(i+dirOffset)%8];
			}
			directions = temp;
		}

		///// Call run function for specific entity type /////
		System.out.println("I'm a " + rc.getType() + " and I just got created!");
		try {
			switch (rc.getType()) {
			case HQ:					runHQ();				break;
			case MINER:			  		runMiner();			 	break;
			case REFINERY:		   		runRefinery();		  	break;
			case VAPORATOR:		  		runVaporator();		 	break;
			case DESIGN_SCHOOL:	  		runDesignSchool();	  	break;
			case FULFILLMENT_CENTER: 	runFulfillmentCenter(); break;
			case LANDSCAPER:		 	runLandscaper();		break;
			case DELIVERY_DRONE:	 	runDeliveryDrone();	 	break;
			case NET_GUN:				runNetGun();			break;
			case COW:				 /* Do Nothing */			break;
			}
		} catch (Exception e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
	}

	private static void runHQ() throws GameActionException {
		///// Spawn in initial miners /////
		MapLocation senseStopLoc = runHQInit();

		while(true) {
			if(rc.canBuildRobot(RobotType.MINER, directions[0])) {
				rc.buildRobot(RobotType.MINER, directions[0]);
			}

			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}

			Clock.yield();
		}
	}

	private static MapLocation runHQInit() throws GameActionException {
		MapLocation senseStopLoc = new MapLocation(0,0);
		int hor = 0;
		
		///// Spawn in initial miners /////
		if(directions[0] == Direction.WEST || directions[0] == Direction.NORTH || directions[0] == Direction.EAST || directions[0] == Direction.SOUTH) {
			hor = 1;
		}
		
		if(rc.canBuildRobot(RobotType.MINER, directions[1 + hor])) {
			rc.buildRobot(RobotType.MINER, directions[1 + hor]);
			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}
			Clock.yield();
		}
		
		if(rc.canBuildRobot(RobotType.MINER, directions[7 - hor])) {
			rc.buildRobot(RobotType.MINER, directions[7 - hor]);
			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}
			Clock.yield();
		}
		
		while(!rc.canBuildRobot(RobotType.MINER, directions[0])) {
			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}
			Clock.yield();
		}
		rc.buildRobot(RobotType.MINER, directions[0]);
		
		return senseStopLoc;
	}

	private static void runMiner() throws GameActionException {
		Direction moveDir = Direction.CENTER;
		for(int i = 0; i < 8; i++) {
			if(findAdjacentRobot(RobotType.HQ, directions[i])) {
				moveDir = directions[(i + 4) % 8];
			}
		}
		
		if(rc.getRobotCount() <= 4) { // The first 3 miners are the Search Miner sub-class
			int x = rc.getLocation().x - moveDir.dx;
			int y = rc.getLocation().y - moveDir.dy;
			if(moveDir.dx != 0) {
				x = rc.getMapWidth() - 1 - x;
			}
			if(moveDir.dy != 0) {
				y = rc.getMapHeight() - 1 - y;
			}
			MapLocation target = new MapLocation(x, y);
			runSearchMiner(target);
			
			Clock.yield();
		}
		runSoupMiner(moveDir); // If a Search Miner finishes their job, they return and get brought to the Soup Miner sub-class
	}



	private static void runSearchMiner(MapLocation target) throws GameActionException {
		System.out.println("I'm a Search Miner!");
		System.out.println(target);
		while(true) {
			///// Check Transactions for base found /////
			checkTransactions();
			if(enemyBaseFound) {
				runBuilderMiner();
				break;
			}
			
			///// Move based on going along wall if colliding with obstacle toward center preferably
			moveCloser(target);
			
			MapLocation enemyHQ = senseEnemyHQ();
			if(enemyHQ != null) {
				int[] message = new int[] {21, enemyHQ.x, enemyHQ.y};
				if(rc.canSubmitTransaction(message, 1)) {
					rc.submitTransaction(message, 1);
					break;
				}
			}
			
			Clock.yield();
		}

	}

	private static void runBuilderMiner() {
		System.out.println("I'm a Builder Miner!");
		while(true) {

			Clock.yield();
		}
	}

	private static void runSoupMiner(Direction moveDir) {
		System.out.println("I'm a Soup Miner!");
		while(true) {

			Clock.yield();
		}
	}

	private static void runRefinery() {
		// TODO Auto-generated method stub

	}

	private static void runVaporator() {
		// TODO Auto-generated method stub

	}

	private static void runDesignSchool() {
		// TODO Auto-generated method stub

	}

	private static void runFulfillmentCenter() {
		// TODO Auto-generated method stub

	}

	private static void runLandscaper() {
		// TODO Auto-generated method stub

	}

	private static void runDeliveryDrone() {
		// TODO Auto-generated method stub

	}

	private static void runNetGun() {
		// TODO Auto-generated method stub

	}


	private static HashMap<MapLocation, int[]> senseInRange(MapLocation stopLoc) throws GameActionException {
		MapLocation loc = rc.getLocation();
		int rSq = rc.getType().sensorRadiusSquared;
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		MapLocation senseLoc;

		for(int y = Math.max(loc.y - (int) (Math.pow(rSq, 0.5)), stopLoc.y); y <= Math.min(loc.y + (int) (Math.pow(rSq, 0.5)), rc.getMapHeight()); y++) {
			for(int x = Math.max(loc.x - (int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)), stopLoc.x); x <= Math.min(loc.x + (int) ((Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5))), rc.getMapWidth()); x++) {
				senseLoc = new MapLocation(x, y);
				if(!map.containsKey(senseLoc) && rc.canSenseLocation(senseLoc)) {
					results.put(senseLoc, new int[] {rc.senseElevation(senseLoc), rc.senseFlooding(senseLoc) ? 1 : 0, rc.senseSoup(senseLoc)});
				}
				if(Clock.getBytecodesLeft() < 500) {
					stopLoc = new MapLocation(senseLoc.x, senseLoc.y);
					System.out.println(stopLoc);
					return results;
				}
			}
		}
		stopLoc = new MapLocation(-1, -1);
		System.out.println(map.size());
		return results;
	}

	private static boolean findAdjacentRobot(RobotType type, Direction dir) {
		MapLocation currLoc = rc.getLocation();
		try {
			RobotInfo robo = rc.senseRobotAtLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy));
			if(robo != null && robo.getType() == type) {
				return true;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean moveCloser(MapLocation target) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}
		
		int[] baseMove = {0,1,2,-1,-2,3,-3,4};
		int dir = getDirection(rc.getLocation(), target);
		System.out.println(directions[dir]);
		
		for(int dDir : baseMove) {
			if(rc.canMove(directions[(dir + dDir + 8) % 8]) && avoidWalls(directions[(dir + dDir + 8) % 8])) {
				rc.move(directions[(dir + dDir + 8) % 8]);
				return true;
			}
		}
		return false;
	}
	
	private static boolean avoidWalls(Direction dir) {
		MapLocation loc = rc.getLocation();
		if(loc.x + dir.dx < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.x + dir.dx > rc.getMapWidth() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		} else if (loc.y + dir.dy < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.y + dir.dy > rc.getMapHeight() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		}
		return true;
	}
	
	private static int getDirection(MapLocation start, MapLocation end) {
		double angle = (Math.atan2(end.y - start.y, end.x - start.x))/Math.PI + 1;
		
		if(angle > 15.0/8 || angle <= 1.0/8) {
			return (0 + dirOffset) % 8;
		} else if(angle > 13.0/8) {
			return (1 + dirOffset) % 8;
		} else if(angle > 11.0/8) {
			return (2 + dirOffset) % 8;
		} else if(angle > 9.0/8) {
			return (3 + dirOffset) % 8;
		} else if(angle > 7.0/8) {
			return (4 + dirOffset) % 8;
		} else if(angle > 5.0/8) {
			return (5 + dirOffset) % 8;
		} else if(angle > 3.0/8) {
			return (6 + dirOffset) % 8;
		} else {
			return (7 + dirOffset) % 8;
		}
	}

	private static MapLocation senseEnemyHQ() {
		RobotInfo[] sensed = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam().opponent());
		for(RobotInfo robo : sensed) {
			if(robo.type == RobotType.HQ) {
				return robo.location;
			}
		}
		return null;
	}
	
	private static void checkTransactions() throws GameActionException {
		Transaction[] trans = rc.getBlock(rc.getRoundNum()-1);
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}

	private static void analyzeTransaction(Transaction t) {
		if(t.getMessage()[0] == 21) {
			enemyBaseFound = true;
			map.put(new MapLocation(t.getMessage()[1],  t.getMessage()[2]), new int[] {0,0,0,-1});
		}
	}
}