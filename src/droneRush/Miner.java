package droneRush;

import java.util.Collections;

import battlecode.common.*;

public class Miner extends Unit {
	boolean builder = false;
	RobotType[] buildings = new RobotType[] {RobotType.FULFILLMENT_CENTER, RobotType.DESIGN_SCHOOL, RobotType.REFINERY, RobotType.VAPORATOR};
	MapLocation[] buildSpots = new MapLocation[buildings.length];

	public Miner(RobotController rc) {
		super(rc);

		if(rc.getRoundNum() == 3) {
			builder = true;
			System.out.println("Builder");
		}

		try {
			checkTransactions();
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		
		Direction awayCenter = center.directionTo(HQs[0]);
		if(awayCenter == Direction.NORTH || awayCenter == Direction.EAST || awayCenter == Direction.SOUTH || awayCenter == Direction.WEST) {
			awayCenter = awayCenter.rotateRight();
		}
		buildSpots[0] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy);
		if(awayCenter == Direction.NORTHEAST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx + 1, HQs[0].y + 2 * awayCenter.dy);
		} else if(awayCenter == Direction.SOUTHEAST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy - 1);
		} else if(awayCenter == Direction.SOUTHWEST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx - 1, HQs[0].y + 2 * awayCenter.dy);
		} else {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy + 1);
		}
		buildSpots[2] = null;
		buildSpots[3] = HQs[0].translate(-1, 0);
	}

	@Override
	protected void run() throws GameActionException {
		// Sense for initial soup
		MapLocation[] newSoup = rc.senseNearbySoup();
		Collections.addAll(soup, newSoup);
		for(MapLocation s : newSoup) {
			if(rc.canSenseLocation(s)) {
				map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
			}
		}

		// Get initial target if any soup was found
		if(!soup.isEmpty()) {
			target = bestSoup();
			rc.setIndicatorDot(target, 255, 0, 0);
		}
		
		int buildCount = 0;
		// Find, mine, and refine soup forever or until can build
		while(true) {
			try {
				// Find and mine soup until full
				while(rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
					getSoup();
					yield();
				}
				// Return soup to HQ
				returnSoup();
			} catch(GameActionException e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
			yield();
			
			while(builder && rc.getTeamSoup() >= buildings[buildCount].cost + 10) {
				System.out.println("Building!");
				build(buildings[buildCount], buildSpots[buildCount]);
				buildCount++;
				yield();
			}
		}
	}

	private void getSoup() throws GameActionException {
		int giveUp = 0; // So they stop going after the same soup if they can reach it
		boolean hasBeenRandom = target == null && soup.isEmpty(); // If they find new soup for potentially everyone

		// While not in range of soup
		while(target == null || !rc.canSenseLocation(target)) {
			// If targeting can be done
			if(target == null && !soup.isEmpty()) {
				target = bestSoup();
				giveUp = 0;

				// If this is potentially new soup for everyone
				if(hasBeenRandom) {
					int[] message = new int[7];
					message[0] = 117290;
					message[1] = target.x;
					message[2] = target.y;
					message[3] = map.get(target)[2];
					if(rc.canSubmitTransaction(message, 1)) {
						rc.submitTransaction(message, 1);
					}
				}
			}

			// If youre still trying to reach the soup, move closer
			if(target != null && giveUp < 20) {
				rc.setIndicatorDot(target, 255, 0, 0);
				moveCloser(target, false);
				hasBeenRandom = false;
			} else {
				moveRandom(); // Make this better
				hasBeenRandom = true;
			}
			giveUp++;

			// Sense for new soup
			MapLocation[] newSoup = rc.senseNearbySoup();
			for(MapLocation s : newSoup) {
				if(!map.containsKey(s)) {
					soup.add(s);
					map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
				}
			}

			// If you cant reach the soup
			if(target != null && giveUp >= 20) {
				rc.setIndicatorDot(target, 255, 255, 255);
				soup.remove(target);
				target = null;
				giveUp = 0;
			}
			yield();
		}
		// Should be in range of soup now
		giveUp = 0;

		// Move up to soup
		while((!loc.equals(target) || !loc.isAdjacentTo(target)) && giveUp < 20 && rc.senseSoup(target) != 0) {
			rc.setIndicatorDot(target, 0, 255, 255);
			moveCloser(target, false);
			giveUp++;
			yield();
		}

		// Mine the soup
		Direction dir = loc.directionTo(target);
		rc.setIndicatorDot(target, 255, 0, 255);
		while(rc.canMineSoup(dir)) {
			rc.mineSoup(dir);
			yield();
		}

		// If the soup is gone (also check if soup is reachable preferably)
		rc.setIndicatorDot(target, 255, 255, 0);
		if(rc.senseSoup(target) == 0) {
			rc.setIndicatorDot(target, 255, 255, 255);
			soup.remove(target);
			target = null;
		}
	}

	private void returnSoup() throws GameActionException {
		MapLocation returnTo;
		if(ref == null) {
			returnTo = HQs[0];
		} else {
			returnTo = ref;
		}
		
		// Move to HQ
		while(!loc.isAdjacentTo(returnTo)) {
			moveCloser(returnTo, false);
			yield();
		}

		// Deposit soup to be refined
		Direction dir = loc.directionTo(returnTo);
		if(rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, RobotType.MINER.soupLimit);
		} else {
			System.out.println("Failed to deposit soup!");
		}
	}
	
	private void build(RobotType robo, MapLocation buildSpot) throws GameActionException {
		if(buildSpot == null) {
			buildSpot = bestSoup();
		}
		while(!loc.isAdjacentTo(buildSpot)) {
			moveCloser(buildSpot, false);
			yield();
		}
		Direction dir = loc.directionTo(buildSpot);
		while(!rc.canBuildRobot(robo, dir)) {
			yield();
		}
		rc.buildRobot(robo, dir);
		int code;
		if(robo == RobotType.FULFILLMENT_CENTER) {
			code = 117294;
		} else if(robo == RobotType.DESIGN_SCHOOL) {
			code = 117295;
		} else if(robo == RobotType.REFINERY) {
			code = 117292;
		} else if(robo == RobotType.VAPORATOR) {
			code = 117293;
		} else {
			code = 0;
			System.out.println("Failed to code");
		}
		int[] message = new int[] {code, buildSpot.x, buildSpot.y, -1, -1, -1, -1};
		if(rc.canSubmitTransaction(message, 10)) {
			rc.submitTransaction(message, 10);
		}
	}
	
	private MapLocation bestSoup() {
		orderedSoup.clear();
		orderedSoup.addAll(soup);
		return orderedSoup.first();
	}
}
