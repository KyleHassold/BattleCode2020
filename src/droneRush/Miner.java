package droneRush;

import java.util.Collections;

import battlecode.common.*;

public class Miner extends Unit {
	boolean builder = false;

	public Miner(RobotController rc) {
		super(rc);

		if(rc.getRoundNum() == 2) {
			builder = true;
		}

		try {
			checkTransactions();
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void run() throws GameActionException {
		// Sense for initial soup
		MapLocation[] newSoup = rc.senseNearbySoup();
		Collections.addAll(soup, newSoup);
		for(MapLocation s : newSoup) {
			map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
		}

		// Get initial target if any soup was found
		if(!soup.isEmpty()) {
			target = soup.first();
			rc.setIndicatorDot(target, 255, 0, 0);
		}

		// Find, mine, and refine soup forever or until can build
		while(!builder || rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost + 10) {
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
		}
		
		System.out.println("Building!");

		Direction awayCenter = center.directionTo(HQs[0]);
		if(awayCenter == Direction.NORTH || awayCenter == Direction.EAST || awayCenter == Direction.SOUTH || awayCenter == Direction.WEST) {
			awayCenter = awayCenter.rotateRight();
		}
		MapLocation buildSpot = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy);
		rc.setIndicatorDot(buildSpot, 255, 0, 0);

		while(!loc.isAdjacentTo(buildSpot)) {
			moveCloser(buildSpot, false);
			yield();
		}
		
		while(!rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, loc.directionTo(buildSpot))) {
			yield();
		}
		rc.buildRobot(RobotType.FULFILLMENT_CENTER, loc.directionTo(buildSpot));
		int[] message = new int[] {117294, buildSpot.x, buildSpot.y, -1, -1, -1, -1};
		if(rc.canSubmitTransaction(message, 10)) {
			rc.submitTransaction(message, 10);
		}

		// Find, mine, and refine soup forever
		while(!builder || rc.getTeamSoup() < RobotType.DESIGN_SCHOOL.cost + 10) {
			try {
				// Find and mine soup until full
				while(rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
					getSoup();
				}
				// Return soup to HQ
				returnSoup();
			} catch(GameActionException e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
			yield();
		}

		System.out.println("Building!");
		
		if(awayCenter == Direction.NORTHEAST) {
			buildSpot = new MapLocation(HQs[0].x + 2 * awayCenter.dx + 1, HQs[0].y + 2 * awayCenter.dy);
		} else if(awayCenter == Direction.SOUTHEAST) {
			buildSpot = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy - 1);
		} else if(awayCenter == Direction.SOUTHWEST) {
			buildSpot = new MapLocation(HQs[0].x + 2 * awayCenter.dx - 1, HQs[0].y + 2 * awayCenter.dy);
		} else {
			buildSpot = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy + 1);
		}

		while(!loc.isAdjacentTo(buildSpot)) {
			moveCloser(buildSpot, false);
			yield();
		}

		while(!rc.canBuildRobot(RobotType.DESIGN_SCHOOL, loc.directionTo(buildSpot))) {
			yield();
		}
		rc.buildRobot(RobotType.DESIGN_SCHOOL, loc.directionTo(buildSpot));
		message = new int[] {117295, buildSpot.x, buildSpot.y, -1, -1, -1, -1};
		if(rc.canSubmitTransaction(message, 10)) {
			rc.submitTransaction(message, 10);
		}

		// Find, mine, and refine soup forever
		while(true) {
			try {
				// Find and mine soup until full
				while(rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
					getSoup();
				}
				if(builder && buildRobot(RobotType.REFINERY, awayCenter)) {
					builder = false;
					ref = findAdjacentRobot(RobotType.REFINERY, rc.getTeam());
					map.put(ref, new int[] {0,0,0,2});
					message = new int[] {117292, ref.x, ref.y, -1, -1, -1, -1};
					while(!rc.canSubmitTransaction(message, 1)) {
						yield();
					}
					rc.submitTransaction(message, 1);
				}
				// Return soup to HQ
				returnSoup();
			} catch(GameActionException e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
			yield();
		}
	}

	private void getSoup() throws GameActionException {
		int giveUp = 0; // So they stop going after the same soup if they can reach it
		boolean hasBeenRandom = target == null && soup.isEmpty(); // If they find new soup for potentially everyone

		// While not in range of soup
		while(target == null || !rc.canSenseLocation(target)) {
			// If targeting can be done
			System.out.println(target);
			if(target == null && !soup.isEmpty()) {
				System.out.println("New Target");
				target = soup.first();
				giveUp = 0;

				// If this is potentially new soup for everyone
				if(hasBeenRandom) {
					int[] message = new int[7];
					message[0] = 117290;
					message[1] = soup.first().x;
					message[2] = soup.first().y;
					message[3] = map.get(soup.first())[2];
					if(rc.canSubmitTransaction(message, 1)) {
						rc.submitTransaction(message, 1);
					}
				}
			}
			System.out.println(target);

			// If youre still trying to reach the soup, move closer
			if(target != null && giveUp < 20) {
				System.out.println("Moving to Target");
				rc.setIndicatorDot(target, 255, 0, 0);
				moveCloser(target, false);
				hasBeenRandom = false;
			} else {
				System.out.println("Moving randomly");
				moveRandom(); // Make this better
				hasBeenRandom = true;
			}
			giveUp++;
			System.out.println(target);

			// Sense for new soup
			MapLocation[] newSoup = rc.senseNearbySoup();
			for(MapLocation s : newSoup) {
				if(!map.containsKey(s)) {
					soup.add(s);
					map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
				}
			}
			System.out.println(target);

			// If you cant reach the soup
			if(target != null && giveUp >= 20) {
				System.out.println("Giving up");
				rc.setIndicatorDot(target, 255, 255, 255);
				soup.remove(target);
				target = null;
				giveUp = 0;
			}
			System.out.println(target);
			yield();
			System.out.println(target);
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
}
