package Phases;

import java.util.ArrayList;
import java.util.Comparator;
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
	static int[][][] minerNewSense = {{{3, -5},{5, 3},{4, -4},{5, 1},{5, -1},{3, 5},{5, -3},{5, 2},{4, 4},{5, 0},{5, -2}},
			{{-3, 5},{2, 5},{5, 2},{4, 4},{5, -2},{0, 5},{5, 1},{4, 3},{-2, 5},{3, 5},{5, -3},{1, 5},{5, 0},{3, 4},{5, 3},{-1, 5},{5, -1}}};
	static HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();
	static HashMap<MapLocation, Integer> soup = new HashMap<MapLocation, Integer>();
	static MapLocation[] HQs = new MapLocation[2];
	static MapLocation[] refs = new MapLocation[2];
	static MapLocation desSch;
	static MapLocation vaporator;
	static boolean buildVap = false;
	static Direction prevSpot;
	static int phase = 1;
	static int miners = 0;

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
		miners += 3;

		while(miners < 9) {
			if(buildRobot(RobotType.MINER, directions.get(0))) {
				miners++;
			}

			///// Use remaining ByteCode to sense surroundings /////
			if(senseStopLoc != new MapLocation(-1, -1)) { // If the sensing has not finished, continue where left off
				map.putAll(senseInRange(senseStopLoc));
			}

			Clock.yield();
		}
		
		while(true) {
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
		HQs[0] = findAdjacentRobot(RobotType.HQ, null);
		
		// fix miner counter
		if(miners == 2) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, HQs[0].y);
			runSearchMiner(target);
		} else if(miners == 3) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
			runSearchMiner(target);
		} else if(miners == 4) { // The first 3 miners are the Search Miner sub-class
			MapLocation target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
			runSearchMiner(target);
		}
		Clock.yield();

		runSoupMiner(); // If a Search Miner finishes their job, they return and get brought to the Soup Miner sub-class
	}

	private static void runSearchMiner(MapLocation target) throws GameActionException {
		System.out.println("I'm a Search Miner!");
		MapLocation refLoc = target;
		System.out.println(target);
		int refScore = 0;
		while(true) {
			///// Check Transactions for base found /////
			checkTransactions();
			if(HQs[1] != null) {
				runBuilderMiner(target, refLoc, 0);
				break;
			}

			///// Move based on going along wall if colliding with obstacle toward center preferably
			moveCloser(target, true);
			
			HashMap<MapLocation, int[]> sensed = newSensor();
			map.putAll(sensed);
			for(MapLocation loc : sensed.keySet()) {
				if(sensed.get(loc)[2] != 0) {
					soup.put(loc, sensed.get(loc)[2]);
					if(refs[1] != null && rc.canSubmitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)[2]}, 1)) {
						rc.submitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)[2]}, 1);
					}
					int potScore = getRefineryScore(target, loc, soup.get(loc));
					System.out.println(potScore);
					if(getRSquared(HQs[0], loc) > RobotType.REFINERY.pollutionRadiusSquared * 2 && potScore > refScore) {
						refLoc = loc;
						refScore = potScore;
					}
				}
			}

			if(rc.canSenseLocation(target)) {
				RobotInfo enemyHQ = rc.senseRobotAtLocation(target);
				if(enemyHQ == null || enemyHQ.team == rc.getTeam() || enemyHQ.type != RobotType.HQ) {
					runBuilderMiner(target, refLoc, 0);
				} else {
					HQs[1] = new MapLocation(enemyHQ.location.x, enemyHQ.location.y);

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
	
	private static void runBuilderMiner(MapLocation target, MapLocation refLoc, double score) throws GameActionException {
		System.out.println("I'm a Builder Miner!");
		while(miners < 9) {
			checkTransactions();
			
			if(Math.random() > 0.25 || HQs[1] == null) {
				moveRandom();
			} else {
				moveCloser(HQs[1], true);
			}
			
			HashMap<MapLocation, int[]> sensed = newSensor();
			map.putAll(sensed);
			for(MapLocation loc : sensed.keySet()) {
				if(sensed.get(loc)[2] != 0) {
					soup.put(loc, sensed.get(loc)[2]);
					if(refs[1] != null && rc.canSubmitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)[2]}, 1)) {
						rc.submitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)[2]}, 1);
					}
					int potScore = getRefineryScore(target, loc, soup.get(loc));
					System.out.println(potScore);
					if(getRSquared(HQs[0], loc) > RobotType.REFINERY.pollutionRadiusSquared * 2 && potScore > score) {
						refLoc = loc;
						score = potScore;
					}
				}
			}
		}
		while(getRSquared(rc.getLocation(), refLoc) > 2) {
			checkTransactions();
			
			moveCloser(refLoc, false);
		}
		while(true) {
			checkTransactions();
			
			if(rc.getTeamSoup() >= 210 && buildRobot(RobotType.REFINERY, Direction.NORTH)) {
				MapLocation loc = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
				map.put(loc, new int[] {0, map.get(loc)[1], map.get(loc)[2], 2});
				if(rc.canSubmitTransaction(new int[] {22, loc.x, loc.y}, 10)) {
					rc.submitTransaction(new int[] {22, loc.x, loc.y}, 10);
				}
				if(refs[0] == null) {
					refs[0] = loc;
				} else {
					refs[1] = loc;
					runDesignMiner();
					buildVap = true;
				}
				break;
			}
			Clock.yield();
		}
	}

	private static void runDesignMiner() throws GameActionException {
		Direction dir = getDirection(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2), HQs[0]);
		MapLocation target = new MapLocation(HQs[0].x + dir.dx * 2, HQs[0].y + dir.dy * 2);
		while(!rc.getLocation().equals(target)) {
			moveCloser(target, false);
			Clock.yield();
		}
		while(rc.getTeamSoup() <= RobotType.DESIGN_SCHOOL.cost + 10 || !rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
			Clock.yield();
		}
		rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
		if(rc.canSubmitTransaction(new int[] {117293, target.x + dir.dx, target.y + dir.dy}, 10)) {
			rc.submitTransaction(new int[] {117293, target.x + dir.dx, target.y + dir.dy}, 10);
		}
	}

	private static void runSoupMiner() throws GameActionException {
		System.out.println("I'm a Soup Miner!");
		MapLocation target = null;
		while(true) {
			checkTransactions();
			if(buildVap && desSch != null) {
				MapLocation moveTo = new MapLocation(HQs[0].x - 1, HQs[0].y - 1);
				while(!rc.getLocation().equals(moveTo)) {
					moveCloser(moveTo, false);
					Clock.yield();
				}
				while(!rc.canBuildRobot(RobotType.VAPORATOR, Direction.NORTH)) {
					Clock.yield();
				}
				rc.buildRobot(RobotType.VAPORATOR, Direction.NORTH);
				vaporator = rc.adjacentLocation(Direction.NORTH);
				Clock.yield();
				if(rc.canSubmitTransaction(new int[] {117294, vaporator.x, vaporator.y}, 8)) {
					rc.submitTransaction(new int[] {117294, vaporator.x, vaporator.y}, 9);
				}
				rc.move(Direction.SOUTHEAST);
				Clock.yield();
			}
			target = getSoup(target);
			returnSoup();

			
		}
	}

	private static MapLocation getSoup(MapLocation target) throws GameActionException {
		MapLocation loc;

		while(true) {
			checkTransactions();
			loc = rc.getLocation();
			
			if(!soup.isEmpty() && target == null) {
				target = getBestSoup(); //Check this
				moveCloser(target, false);
				System.out.println(target);
			} else if(soup.isEmpty()) {
				moveRandom();
			} else if(getRSquared(loc, target) <= 2){
				Direction dir = getDirection(loc, target);
				if(rc.canMineSoup(dir)) {
					rc.mineSoup(dir);
				} else {
					if(rc.senseSoup(loc) == 0) {
						soup.remove(loc);
						map.put(loc, new int[] {rc.senseFlooding(loc) ? 1 : 0, rc.senseElevation(loc), 0, 0});
						return null;
					} else {
						return target;
					}
				}
			} else {
				moveCloser(target, false);
			}
			
			HashMap<MapLocation, int[]> sensed = newSensor();
			map.putAll(sensed);
			for(MapLocation s : sensed.keySet()) {
				if(sensed.get(s)[2] != 0) {
					soup.put(s, sensed.get(s)[2]);
				}
			}
			
			Clock.yield();
		}
	}

	private static void returnSoup() throws GameActionException {
		MapLocation ref = findAdjacentRobot(RobotType.REFINERY, rc.getTeam());
		while(ref == null) {
			checkTransactions();
			moveCloser(closestRef(), false);
			System.out.println("Closest Refinery: " + closestRef());
			ref = findAdjacentRobot(RobotType.REFINERY, rc.getTeam());
			Clock.yield();
		}
		while(rc.getSoupCarrying() > 0) {
			checkTransactions();
			if(rc.canDepositSoup(getDirection(rc.getLocation(), ref))) {
				rc.depositSoup(getDirection(rc.getLocation(), ref), 100);
			}
			Clock.yield();
		}
	}

	private static void runRefinery() {
		while(true) {
			Clock.yield();
		}
	}

	private static void runVaporator() {
		while(true) {
			Clock.yield();
		}
	}

	private static void runDesignSchool() throws GameActionException {
		int count = 0;
		MapLocation loc = rc.getLocation();
		Direction dir = getDirection(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2), loc);
		HQs[0] = new MapLocation(loc.x - 3 * dir.dx, loc.y - 3 * dir.dy);
		
		
		// Spawn in 5 Landscapers for phase 2
		while(count < 9) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		
		// Wait for phase 3
		while(vaporator == null) {
			checkTransactions();
			Clock.yield();
		}
		
		while(count < 10) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		
		// Prevent future code
		while(phase < 3) {
			checkTransactions();
			Clock.yield();
		}
		
		while(count < 33) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		while(true) {
			Clock.yield();
		}
	}

	private static void runFulfillmentCenter() throws GameActionException {
		int count = 0;
		MapLocation loc = rc.getLocation();
		Direction dir = getDirection(loc, new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
		HQs[0] = new MapLocation(loc.x + 3 * dir.dx, loc.y + 3 * dir.dy);
		
		// Spawn in 4 Drones for phase 2
		while(count < 4) {
			checkTransactions();
			if(buildRobot(RobotType.DELIVERY_DRONE, dir)) {
				count++;
			}
			Clock.yield();
		}

	}

	private static void runLandscaper() throws GameActionException {
		MapLocation loc = rc.getLocation();
		Direction dir = getDirection(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2), loc);
		HQs[0] = new MapLocation(loc.x - 4 * dir.dx, loc.y - 4 * dir.dy);
		// Phase 3 landscaper locations
		MapLocation[] wallBuilding = new MapLocation[] {
				new MapLocation(HQs[0].x - 2, HQs[0].y - 1), new MapLocation(HQs[0].x - 2, HQs[0].y - 0),
				new MapLocation(HQs[0].x - 2, HQs[0].y + 1), new MapLocation(HQs[0].x - 1, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 0, HQs[0].y + 1), new MapLocation(HQs[0].x + 1, HQs[0].y + 1),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 0), new MapLocation(HQs[0].x + 1, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 0, HQs[0].y - 1), new MapLocation(HQs[0].x - 1, HQs[0].y - 1),
		};
				/*
				new MapLocation(HQs[0].x - 1, HQs[0].y - 2), new MapLocation(HQs[0].x - 2, HQs[0].y - 2),
				new MapLocation(HQs[0].x - 3, HQs[0].y - 2), new MapLocation(HQs[0].x - 3, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 3, HQs[0].y - 0), new MapLocation(HQs[0].x - 3, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 3, HQs[0].y + 2), new MapLocation(HQs[0].x - 2, HQs[0].y + 2),
				new MapLocation(HQs[0].x - 1, HQs[0].y + 2), new MapLocation(HQs[0].x - 0, HQs[0].y + 2),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 2), new MapLocation(HQs[0].x + 2, HQs[0].y + 2),
				new MapLocation(HQs[0].x + 2, HQs[0].y + 1), new MapLocation(HQs[0].x + 2, HQs[0].y + 0),
				new MapLocation(HQs[0].x + 2, HQs[0].y - 1), new MapLocation(HQs[0].x + 2, HQs[0].y - 2),
				new MapLocation(HQs[0].x + 1, HQs[0].y - 2), new MapLocation(HQs[0].x - 2, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 1, HQs[0].y + 1), new MapLocation(HQs[0].x - 0, HQs[0].y + 1),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 1), new MapLocation(HQs[0].x + 1, HQs[0].y + 0),
				new MapLocation(HQs[0].x - 2, HQs[0].y + 0), new MapLocation(HQs[0].x - 2, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 1, HQs[0].y - 1), new MapLocation(HQs[0].x + 1, HQs[0].y - 1),
				new MapLocation(HQs[0].x + 0, HQs[0].y - 1), new MapLocation(HQs[0].x + 0, HQs[0].y - 2)
		};
				*/
		int currPos = 0;
		while(!rc.getLocation().equals(wallBuilding[currPos])) {
			if(rc.canSenseLocation(wallBuilding[currPos]) && rc.senseRobotAtLocation(wallBuilding[currPos]) != null) {
				currPos = (currPos + 1) % wallBuilding.length;
			} else {
				moveCloser(wallBuilding[currPos], false);
				Clock.yield();
			}
		}
		Direction dig = getDirection(HQs[0], rc.getLocation());
		Direction deposit = Direction.CENTER;
		/*
		if(findAdjacentRobot(RobotType.HQ, rc.getTeam()) != null) {
			deposit = dig;
			dig = Direction.CENTER;
		}
		*/
		
		while(true) {
			if(rc.canDigDirt(dig)) {
				rc.digDirt(dig);
			} else if(rc.canDepositDirt(deposit)){
				rc.depositDirt(deposit);
			}
			Clock.yield();
		}
	}

	private static void runDeliveryDrone() throws GameActionException {
		while(true) {
			moveCloser(new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() -1), false);
			Clock.yield();
		}

	}

	private static void runNetGun() throws GameActionException {
		// Likely not going to use
		Team opp = rc.getTeam().opponent();
		
		while(true) {
			for(RobotInfo robo : rc.senseNearbyRobots(15, opp)) {
				if(robo.type == RobotType.DELIVERY_DRONE && rc.canShootUnit(robo.ID)) {
					rc.shootUnit(robo.ID);
					break;
				}
			}
			Clock.yield();
		}

	}

	//------------------------------------- Aux Functions -------------------------------------------------

	// Building robots
	private static boolean buildRobot(RobotType rType, Direction prefDir) throws GameActionException {
		if(rc.getTeamSoup() < rType.cost) {
			return false;
		}
		
		int[] baseDir = {0,1,2,-1,-2,3,-3,4};
		int dirI = directions.indexOf(prefDir);

		Direction dir;
		for(int dDir : baseDir) {
			dir = directions.get((dirI + dDir + 8) % 8);
			if(rc.canBuildRobot(rType, dir)) {
				rc.buildRobot(rType, dir);
				return true;
			}
		}
		return false;

	}
	
	// Sensing
	private static HashMap<MapLocation, int[]> senseInRange(MapLocation stopLoc) throws GameActionException {
		MapLocation loc = rc.getLocation();
		int rSq = rc.getType().sensorRadiusSquared;
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		MapLocation senseLoc;

		for(int y = Math.max(loc.y - (int) (Math.pow(rSq, 0.5)), stopLoc.y); y <= Math.min(loc.y + (int) (Math.pow(rSq, 0.5)), rc.getMapHeight()); y++) {
			for(int x = Math.max(loc.x - (int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)), stopLoc.x); x <= Math.min(loc.x + (int) ((Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5))), rc.getMapWidth()); x++) {
				senseLoc = new MapLocation(x, y);
				if(!map.containsKey(senseLoc) && rc.canSenseLocation(senseLoc)) {
					results.put(senseLoc, new int[] {rc.senseFlooding(senseLoc) ? 1 : 0, rc.senseElevation(senseLoc), rc.senseSoup(senseLoc), 0});
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

	private static HashMap<MapLocation, int[]> newSensor() throws GameActionException {
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		Direction moveDir = directions.get((directions.indexOf(prevSpot) + 4) % 8);
		MapLocation loc = rc.getLocation();
		MapLocation curr;
		int genDir;

		if(moveDir == Direction.NORTH || moveDir == Direction.EAST || moveDir == Direction.SOUTH || moveDir == Direction.WEST) {
			genDir = 0;
		} else {
			genDir = 1;
		}
		for(int[] coords : minerNewSense[genDir]) {
			int dx = 0;
			int dy = 0;
			if(moveDir.dx == -1) {
				dx = -1 * coords[0];
			} else {
				dx = coords[0];
			}
			if(moveDir.dy == -1) {
				dy = -1 * coords[1];
			} else {
				dy = coords[1];
			}
			curr = new MapLocation(loc.x + dx, loc.y + dy);
			if(rc.canSenseLocation(curr)) {
				results.put(curr, new int[] {rc.senseFlooding(curr) ? 1 : 0, rc.senseElevation(curr), rc.senseSoup(curr)});
			}
		}

		return results;
	}

	private static MapLocation findAdjacentRobot(RobotType type, Team team) throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		for(Direction dir : directions) {
			if(rc.canSenseLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy))) {
				RobotInfo robo = rc.senseRobotAtLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy));
				if(robo != null && (robo.getType() == type || robo.getType() == RobotType.HQ) && (team == null || robo.getTeam() == team)) {
					return robo.location;
				}
			}
		}
		return null;
	}
	
	// Moving
	private static boolean moveCloser(MapLocation target, boolean avoidWalls) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}

		int[] baseMove = {0,1,2,-1,-2,3,-3,4};
		int dir = directions.indexOf(getDirection(rc.getLocation(), target));
		if(dir == -1) { // On spot already
			return false;
		}

		Direction moveDir;
		for(int dDir : baseMove) {
			moveDir = directions.get((dir + dDir + 8) % 8);
			if(canMoveComplete(moveDir, avoidWalls)) {
				rc.move(moveDir);
				prevSpot = directions.get((dir + dDir + 4) % 8);
				return true;
			}
		}
		return false;
	}

	private static boolean moveRandom() throws GameActionException {
		Direction rand;
		int count = 0;
		while(rc.isReady() && count < 16) {
			rand = directions.get((int) (Math.random() * directions.size()));
			if(canMoveComplete(rand, true)) {
				rc.move(rand);
				prevSpot = directions.get((directions.indexOf(rand) + 4) % 8);
				return true;
			}
			count++;
		}
		return false;
	}
	
	private static boolean moveAwayFromHQ(Direction dir) {
		MapLocation loc = rc.getLocation();
		MapLocation adjLoc = rc.adjacentLocation(dir);
		if(loc.x <= HQs[0].x + 1 && loc.x >= HQs[0].x - 2 && loc.x <= HQs[0].y + 1 && loc.y >= HQs[0].y - 1) {
			return Math.abs(directions.indexOf(dir) - directions.indexOf(getDirection(HQs[0], loc)) - 4) <= 2;
		}
		return adjLoc.x <= HQs[0].x + 1 && adjLoc.x >= HQs[0].x - 2 && adjLoc.x <= HQs[0].y + 1 && adjLoc.y >= HQs[0].y - 1;
	}

	private static boolean canMoveComplete(Direction moveDir, boolean avoidWalls) throws GameActionException {
		return prevSpot != moveDir && rc.canMove(moveDir) && !rc.senseFlooding(rc.adjacentLocation(moveDir))
				&& (!avoidWalls || avoidWalls(moveDir)) /*&& (buildVap || desSch == null || rc.getType() != RobotType.MINER && moveAwayFromHQ(moveDir))*/;
	}

	private static boolean potFlooding(MapLocation loc) throws GameActionException {
		int round = rc.getRoundNum() + 5;
		int waterLevel = (int) (Math.pow(Math.E, 0.0028*round - 1.38*Math.sin(0.00157*round - 1.73) + 1.38*Math.sin(-1.78)) - 1);
		return rc.senseElevation(loc) <= waterLevel;
	}
	
	private static boolean avoidWalls(Direction dir) {
		MapLocation loc = rc.adjacentLocation(dir);
		if(loc.x < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.x > rc.getMapWidth() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		} else if (loc.y < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.y > rc.getMapHeight() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		}
		return true;
	}
	
	// Targeting
	private static Direction getDirection(MapLocation start, MapLocation end) {
		if(start.equals(end)) {
			return Direction.CENTER;
		}
		
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
	
	private static MapLocation getBestSoup() {
		ArrayList<MapLocation> locs = new ArrayList<MapLocation>();
		locs.addAll(soup.keySet());
		locs.sort(new Comparator<MapLocation>() {
			final MapLocation loc = rc.getLocation();
			int kd = 10;
			double ks = 0.1;
			
			@Override
			public int compare(MapLocation arg0, MapLocation arg1) {
				return (getRSquared(loc, arg1) - getRSquared(loc, arg0)) * kd - (int)((soup.get(arg1) - soup.get(arg0)) * ks);
			}
		});
		return locs.get(0);
	}

	private static MapLocation closestRef() {
		MapLocation loc = rc.getLocation();
		MapLocation closest = HQs[0];
		System.out.println(refs[0] + " " + refs[1]);
		if(desSch != null || refs[0] != null && getRSquared(loc, refs[0]) <= getRSquared(loc, closest)) {
			closest = refs[0];
		}
		if(refs[1] != null && getRSquared(loc, refs[1]) <= getRSquared(loc, closest)) {
			closest = refs[1];
		}
		return closest;
	}

	private static int getRefineryScore(MapLocation target, MapLocation loc, int soup) {
		return soup / Math.max(getRSquared(target, loc) - 10, 1);
	}
	
	private static int getRSquared(MapLocation start, MapLocation end) {
		return (int) (Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
	
	// Blockchain
	private static void checkTransactions() throws GameActionException {
		Transaction[] trans = rc.getBlock(rc.getRoundNum()-1);
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}

	private static void analyzeTransaction(Transaction t) {
		MapLocation loc;
		if(t.getMessage()[0] == 21) {
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[1] = loc;
			map.put(loc, new int[] {0,0,0,-1});
		} else if(t.getMessage()[0] == 22) {
			System.out.println("Refinery Found");
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			if(refs[0] == null) {
				refs[0] = loc;
			} else {
				refs[1] = loc;
			}
			map.put(loc, new int[] {0,0,0,2});
		} else if(t.getMessage()[0] == 117290) {
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			soup.put(loc, t.getMessage()[3]);
			map.put(loc, new int[] {0,0,t.getMessage()[3],0});
		} else if(t.getMessage()[0] == 117293) {
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			desSch = loc;
			map.put(loc, new int[] {0,0,0,5});
		} else if(t.getMessage()[0] == 117294) {
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,4});
		}
	}
}