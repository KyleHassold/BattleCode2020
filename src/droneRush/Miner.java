package droneRush;

import java.util.Collections;

import battlecode.common.*;

public class Miner extends Unit {

	public Miner(RobotController rc) {
		super(rc);
		try {
			HQs[0] = findAdjacentRobot(RobotType.HQ, null);
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
		
		// Find, mine, and refine soup forever
		while(true) {
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
	}
	
	private void getSoup() throws GameActionException {
		int giveUp = 0; // So they stop going after the same soup if they can reach it
		boolean hasBeenRandom = target == null && soup.isEmpty(); // If they find new soup for potentially everyone
		
		// While not in range of soup
		while(target == null || !rc.canSenseLocation(target)) {
			checkTransactions();
			// If targeting can be done
			if(target == null && !soup.isEmpty()) {
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
		while(!loc.isAdjacentTo(target) && giveUp < 20 && rc.senseSoup(target) != 0) {
			checkTransactions();
			rc.setIndicatorDot(target, 0, 255, 255);
			moveCloser(target, false);
			giveUp++;
			yield();
		}
		
		// Mine the soup
		Direction dir = getDirection(loc, target);
		rc.setIndicatorDot(target, 255, 0, 255);
		while(rc.canMineSoup(dir)) {
			checkTransactions();
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
		// Move to HQ
		while(!loc.isAdjacentTo(HQs[0])) {
			checkTransactions();
			moveCloser(HQs[0], false);
			yield();
		}
		
		// Deposit soup to be refined
		checkTransactions();
		Direction dir = getDirection(loc, HQs[0]);
		if(rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, RobotType.MINER.soupLimit);
		} else {
			System.out.println("Failed to deposit soup!");
		}
	}
}
