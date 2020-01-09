package strat1Begin;
import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	static Direction[] directions = {Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST};
	static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
			RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN}; // Useless?
	static HashMap<MapLocation, int[]> sensed = new HashMap<MapLocation, int[]>();

	static int turnCount;

	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/

	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		turnCount = 0;

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

	static void runHQ() throws GameActionException {
		int curr = 0;
		int[] order = {0,1,7,2,6,3,5,4};
		while(curr < 8) {
			if (rc.getTeamSoup() >= 70) {
				rc.buildRobot(RobotType.MINER, directions[order[curr]]); //Check if can build
				curr++;
			}

			Clock.yield();
		}

		while(true) {
			// Check for enemy drones
			// Attack most dangerous drones
			///// Danger based on nearness to allies

			// Should HQ do anything else? Building miners wastes soup unless requests transmitted
			// Can HQ be used for calculation? Building saved map based on low cost transactions?

			Clock.yield();
		}
	}

	static void runMiner() throws GameActionException {
		Direction moveDir = Direction.CENTER;
		for(int i = 0; i < 8; i++) {
			if(findAdjacentRobot(RobotType.HQ, directions[i], rc.getLocation())) {
				moveDir = directions[(i + 4) % 8];
			}
		}
		while(true) {
			if(rc.canMove(moveDir)) {
				rc.move(moveDir);
			}
			
			HashMap<MapLocation, int[]> newSensed = senseInRange(rc.getLocation(), RobotType.MINER.sensorRadiusSquared);
			//MapLocation[] inRange = getInRange(rc.getLocation(), RobotType.MINER.sensorRadiusSquared);
			//HashMap<MapLocation, Integer> soupFound = senseAllSoup(inRange);
			System.out.println(newSensed.keySet().size());
			if(!newSensed.isEmpty()) {
				int[] message = new int[newSensed.keySet().size() * 3];
				int count = 0;
				for(MapLocation soupLoc : newSensed.keySet()) {
					message[count] = soupLoc.x; // Change to prevent enemy getting data
					message[count+1] = soupLoc.y;
					message[count+2] = newSensed.get(soupLoc)[2];
					count += 3;
				}
				if(rc.canSubmitTransaction(message, 1)) {
					rc.submitTransaction(message, 1);
				}
				sensed.putAll(newSensed);
			}
			
			Clock.yield();
		}
	}

	private static HashMap<MapLocation, int[]> senseInRange(MapLocation loc, int rSq) throws GameActionException {
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		MapLocation curr;
		for(int y = loc.y - (int) (Math.pow(rSq, 0.5)); y < loc.y + (int) (Math.pow(rSq, 0.5)); y++) {
			for(int x = loc.x - (int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)); x < loc.x + (int) ((Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5))); x++) {
				curr = new MapLocation(x, y);
				if(!sensed.containsKey(curr)) {
					results.put(curr, new int[] {rc.senseElevation(curr), rc.senseFlooding(curr) ? 1 : 0, rc.senseSoup(curr)});
				}
			}
		}
		return results;
	}

	private static HashMap<MapLocation, Integer> senseAllSoup(MapLocation[] inRange) throws GameActionException {
		HashMap<MapLocation, Integer> soup = new HashMap<MapLocation, Integer>();
		for(MapLocation loc : inRange) {
			int soupAmount = rc.senseSoup(loc);
			if(soupAmount != 0) {
				soup.put(loc, soupAmount);
			}
		}
		return soup;
	}

	private static MapLocation[] getInRange(MapLocation loc, int rSq) {
		ArrayList<MapLocation> results = new ArrayList<MapLocation>();

		for(int y = loc.y - (int) (Math.pow(rSq, 0.5)); y < loc.y + (int) (Math.pow(rSq, 0.5)); y++) { // Get circle from double for loop
			for(int x = loc.x - (int) (Math.pow(rSq, 0.5)); x < loc.x + (int) (Math.pow(rSq, 0.5)); x++) {
				if(rSq >= Math.pow(x-loc.x, 2) + Math.pow(y-loc.y, 2)) {
					results.add(new MapLocation(x, y));
				}
			}
		}

		return results.toArray(new MapLocation[results.size()]);
	}

	private static boolean findAdjacentRobot(RobotType hq, Direction dir, MapLocation currLoc) {
		try {
			RobotInfo robo = rc.senseRobotAtLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy));
			if(robo != null && robo.getType() == RobotType.HQ) {
				return true;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	static void runRefinery() throws GameActionException {
		// System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
	}

	static void runVaporator() throws GameActionException {

	}

	static void runDesignSchool() throws GameActionException {

	}

	static void runFulfillmentCenter() throws GameActionException {
		for (Direction dir : directions)
			tryBuild(RobotType.DELIVERY_DRONE, dir);
	}

	static void runLandscaper() throws GameActionException {

	}

	static void runDeliveryDrone() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		if (!rc.isCurrentlyHoldingUnit()) {
			// See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
			RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

			if (robots.length > 0) {
				// Pick up a first robot within range
				rc.pickUpUnit(robots[0].getID());
				System.out.println("I picked up " + robots[0].getID() + "!");
			}
		} else {
			// No close robots, so search for robots within sight radius
			tryMove(randomDirection());
		}
	}

	static void runNetGun() throws GameActionException {

	}

	/**
	 * Returns a random Direction.
	 *
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return directions[(int) (Math.random() * directions.length)];
	}

	/**
	 * Returns a random RobotType spawned by miners.
	 *
	 * @return a random RobotType
	 */
	static RobotType randomSpawnedByMiner() {
		return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
	}

	static boolean tryMove() throws GameActionException {
		for (Direction dir : directions)
			if (tryMove(dir))
				return true;
		return false;
		// MapLocation loc = rc.getLocation();
		// if (loc.x < 10 && loc.x < loc.y)
		//	 return tryMove(Direction.EAST);
		// else if (loc.x < 10)
		//	 return tryMove(Direction.SOUTH);
		// else if (loc.x > loc.y)
		//	 return tryMove(Direction.WEST);
		// else
		//	 return tryMove(Direction.NORTH);
	}

	/**
	 * Attempts to move in a given direction.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		// System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.isReady() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to build a given robot in a given direction.
	 *
	 * @param type The type of the robot to build
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canBuildRobot(type, dir)) {
			rc.buildRobot(type, dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to mine soup in a given direction.
	 *
	 * @param dir The intended direction of mining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canMineSoup(dir)) {
			rc.mineSoup(dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to refine soup in a given direction.
	 *
	 * @param dir The intended direction of refining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryRefine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, rc.getSoupCarrying());
			return true;
		} else return false;
	}


	static void tryBlockchain() throws GameActionException {
		if (turnCount < 3) {
			int[] message = new int[10];
			for (int i = 0; i < 10; i++) {
				message[i] = 123;
			}
			if (rc.canSubmitTransaction(message, 10))
				rc.submitTransaction(message, 10);
		}
		// System.out.println(rc.getRoundMessages(turnCount-1));
	}
}
