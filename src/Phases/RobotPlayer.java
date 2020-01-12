package Phases;

import java.util.ArrayList;
import java.util.HashMap;
import battlecode.common.*;

//-------------------------------------------------- INFO --------------------------------------
/*
BLOCKCHAIN CODES:
  117290 = Soup
  117291 = HQ Location
  117292 = Mine Location
  117293 = Design School Location
  117294 = Vaporator Location
BLOCKCHAIN PROTOCOL:
  for soup:
      [code, x, y, amountOfSoup, -1, -1, -1]
  for other stuff:
      [code, x, y, -1, -1, -1, -1]
MAP INT[]:
	Water, Dirt, Soup, Base
	Building:
		Positive: Friendly
		Negative: Enemy
		HQ: 1
		Refinery: 2
		Net Gun: 3
		Vaporator: 4
		Design School: 5
		Fulfillment Center: 6
*/

public strictfp class RobotPlayer {
	
	// --------------------------------------- PRIVATE DATA ---------------------------------------
	static RobotController rc;

	static ArrayList<Direction> directions = new ArrayList<Direction>();
	static HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();
	static HashMap<MapLocation, Integer> soup = new HashMap<MapLocation, Integer>();
	static MapLocation[] HQs = new MapLocation[2];
	static Direction prevSpot;

	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		directions.add(Direction.NORTH);
		directions.add(Direction.NORTHEAST);
		directions.add(Direction.EAST);
		directions.add(Direction.SOUTHEAST);
		directions.add(Direction.SOUTH);
		directions.add(Direction.SOUTHWEST);
		directions.add(Direction.WEST);
		directions.add(Direction.NORTHWEST);
		
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
			buildRobot(RobotType.MINER, directions.get(0));

			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}

			Clock.yield();
		}
	}

	private static MapLocation runHQInit() throws GameActionException {
		MapLocation senseStopLoc = new MapLocation(0,0);
		HQs[0] = rc.getLocation();
		
		MapLocation enemyHQGuess = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, HQs[0].y);
		Direction spawnDir = getDirection(HQs[0], enemyHQGuess);
		buildRobot(RobotType.MINER, spawnDir);
		
		map.putAll(senseInRange(senseStopLoc));
		
		Clock.yield();
		
		enemyHQGuess = new MapLocation(HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		spawnDir = getDirection(HQs[0], enemyHQGuess);
		buildRobot(RobotType.MINER, spawnDir);
		
		if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
			map.putAll(senseInRange(senseStopLoc));
		}
		
		Clock.yield();
		
		enemyHQGuess = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		spawnDir = getDirection(HQs[0], enemyHQGuess);
		while(!buildRobot(RobotType.MINER, spawnDir)) {
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}
			
			Clock.yield();
		}
		
		return senseStopLoc;
	}

	private static void runMiner() throws GameActionException {
		HQs[0] = findAdjacentRobot(RobotType.HQ);
		
		if(rc.getRobotCount() == 2) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, HQs[0].y);
			runSearchMiner(target);
		} else if(rc.getRobotCount() == 3) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
			runSearchMiner(target);
		} else if(rc.getRobotCount() == 4) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
			runSearchMiner(target);
		}
		Clock.yield();
		
		runSoupMiner(); // If a Search Miner finishes their job, they return and get brought to the Soup Miner sub-class
	}

	private static void runSearchMiner(MapLocation target) throws GameActionException {
		System.out.println("I'm a Search Miner!");
		System.out.println(target);
		while(true) {
			///// Check Transactions for base found /////
			checkTransactions();
			if(HQs[1] != null) {
				runBuilderMiner();
				break;
			}
			
			///// Move based on going along wall if colliding with obstacle toward center preferably
			moveCloser(target);
			
			if(rc.canSenseLocation(target)) {
				RobotInfo enemyHQ = rc.senseRobotAtLocation(target);
				if(enemyHQ == null || enemyHQ.team != rc.getTeam().opponent() || enemyHQ.type != RobotType.HQ) {
					runBuilderMiner();
				} else {
					int[] message = new int[] {21, enemyHQ.location.x, enemyHQ.location.y};
					if(rc.canSubmitTransaction(message, 1)) {
						rc.submitTransaction(message, 1);
					}
				}
				
				break;
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

	private static void runSoupMiner() {
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
	
	//------------------------------------- Aux Functions -------------------------------------------------
	
	
	private static boolean buildRobot(RobotType rType, Direction dir) throws GameActionException {
		if(rc.canBuildRobot(rType, dir)) {
			rc.buildRobot(rType, dir);
			return true;
		}
		return false;
		
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

	private static MapLocation findAdjacentRobot(RobotType type) throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		for(Direction dir : directions) {
			RobotInfo robo = rc.senseRobotAtLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy));
			if(robo != null && robo.getType() == type) {
				return robo.location;
			}
		}
		return null;
	}

	private static boolean moveCloser(MapLocation target) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}
		
		int[] baseMove = {0,1,2,-1,-2,3,-3,4};
		int dir = directions.indexOf(getDirection(rc.getLocation(), target));
		
		Direction moveDir;
		for(int dDir : baseMove) {
			moveDir = directions.get((dir + dDir + 8) % 8);
			if(prevSpot != moveDir && rc.canMove(moveDir) && avoidWalls(moveDir)) {
				rc.move(moveDir);
				prevSpot = directions.get((dir + dDir + 4) % 8);
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
	
	private static Direction getDirection(MapLocation start, MapLocation end) {
		double angle = (Math.atan2(end.y - start.y, end.x - start.x))/Math.PI + 1;
		
		if(angle > 15.0/8 || angle <= 1.0/8) {
			return Direction.WEST;
		} else if(angle > 13.0/8) {
			return Direction.NORTHWEST;
		} else if(angle > 11.0/8) {
			return Direction.NORTH;
		} else if(angle > 9.0/8) {
			return Direction.NORTHEAST;
		} else if(angle > 7.0/8) {
			return Direction.EAST;
		} else if(angle > 5.0/8) {
			return Direction.SOUTHEAST;
		} else if(angle > 3.0/8) {
			return Direction.SOUTH;
		} else {
			return Direction.SOUTHWEST;
		}
	}
	
	private static void checkTransactions() throws GameActionException {
		Transaction[] trans = rc.getBlock(rc.getRoundNum()-1);
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}

	private static void analyzeTransaction(Transaction t) {
		if(t.getMessage()[0] == 21) {
			HQs[1] = new MapLocation(t.getMessage()[1],  t.getMessage()[2]);
			map.put(new MapLocation(t.getMessage()[1],  t.getMessage()[2]), new int[] {0,0,0,-1});
		}
	}
}