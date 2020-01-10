package Phases;

import java.util.HashMap;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	static Direction[] directions = {Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST};

	static HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();

	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

		///// Reorder directions based on starting location /////
		MapLocation startLoc = rc.getLocation();
		double angle = Math.atan2(rc.getMapHeight()/2 - startLoc.y, rc.getMapWidth()/2 - startLoc.x) + Math.PI;

		if(angle <= Math.PI * 15/8 && angle > Math.PI * 1/8) {
			int offset = 8 - (int) (4*angle/Math.PI + 0.5);
			Direction[] temp = new Direction[8];
			for(int i = 0; i < 8; i++) {
				temp[i] = directions[(i+offset)%8];
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
			if(rc.canBuildRobot(RobotType.MINER, directions[3])) {
				rc.buildRobot(RobotType.MINER, directions[3]);
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
			runSearchMiner(moveDir);
		}
		runSoupMiner(moveDir); // If a Search Miner finishes their job, they return and get brought to the Soup Miner sub-class
	}



	private static void runSearchMiner(Direction moveDir) throws GameActionException {
		while(true) {
			///// Move based on going along wall if colliding with obstacle toward center preferably
			searchMinerMove(moveDir);

			Clock.yield();
		}

	}

	private static void runSoupMiner(Direction moveDir) {
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
			System.out.println((int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)));
			for(int x = Math.max(loc.x - (int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)), stopLoc.x); x <= Math.min(loc.x + (int) ((Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5))), rc.getMapWidth()); x++) {
				senseLoc = new MapLocation(x, y);
				if(!map.containsKey(senseLoc)) {
					results.put(senseLoc, new int[] {rc.senseElevation(senseLoc), rc.senseFlooding(senseLoc) ? 1 : 0, rc.senseSoup(senseLoc)});
				}
				if(Clock.getBytecodesLeft() < 200) {
					stopLoc = new MapLocation(senseLoc.x, senseLoc.y);
					return results;
				}
			}
		}
		stopLoc = new MapLocation(-1, -1);
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

	private static boolean searchMinerMove(Direction moveDir) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}
		
		if(rc.canMove(moveDir)) {
			rc.move(moveDir);
		} else {
			
		}
		return true;
	}
}