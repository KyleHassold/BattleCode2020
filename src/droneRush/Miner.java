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
		MapLocation[] newSoup = rc.senseNearbySoup();
		Collections.addAll(soup, newSoup);
		for(MapLocation s : newSoup) {
			map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
		}
		if(!soup.isEmpty()) {
			target = soup.first();
			rc.setIndicatorDot(target, 255, 0, 0);
		}

		while(true) {
			try {
				while(rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
					getSoup();
				}
				returnSoup();
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
			yield();
		}
	}
	
	private void getSoup() throws GameActionException {
		int giveUp = 0;
		boolean hasBeenRandom = target == null && soup.isEmpty();
		while(target == null || !rc.canSenseLocation(target)) {
			checkTransactions();
			if(target == null && !soup.isEmpty()) {
				target = soup.first();
				giveUp = 0;
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
			
			if(target != null && giveUp < 20) {
				rc.setIndicatorDot(target, 255, 0, 0);
				moveCloser(target, false);
				hasBeenRandom = false;
			} else {
				moveRandom();
				hasBeenRandom = true;
			}
			giveUp++;
			//Collections.addAll(soup, rc.senseNearbySoup());
			MapLocation[] newSoup = rc.senseNearbySoup();
			for(MapLocation s : newSoup) {
				if(!map.containsKey(s)) {
					soup.add(s);
					map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
				}
			}
			
			if(target != null && giveUp >= 20) {
				rc.setIndicatorDot(target, 255, 255, 255);
				soup.remove(target);
				target = null;
				giveUp = 0;
			}
			yield();
		}
		
		if(rc.senseSoup(target) == 0) {
			rc.setIndicatorDot(target, 255, 255, 255);
			soup.remove(target);
			target = null;
			return;
		}
		giveUp = 0;
		while(!loc.isAdjacentTo(target) && giveUp < 20 && rc.senseSoup(target) != 0) {
			checkTransactions();
			rc.setIndicatorDot(target, 0, 255, 255);
			moveCloser(target, false);
			giveUp++;
			yield();
		}
		
		Direction dir = getDirection(loc, target);
		rc.setIndicatorDot(target, 255, 0, 255);
		while(rc.canMineSoup(dir)) {
			checkTransactions();
			rc.mineSoup(dir);
			yield();
		}
		
		rc.setIndicatorDot(target, 255, 255, 0);
		if(rc.senseSoup(target) == 0) {
			rc.setIndicatorDot(target, 255, 255, 255);
			soup.remove(target);
			target = null;
		}
	}
	
	private void returnSoup() throws GameActionException {
		while(!loc.isAdjacentTo(HQs[0])) {
			checkTransactions();
			moveCloser(HQs[0], false);
			yield();
		}

		checkTransactions();
		Direction dir = getDirection(loc, HQs[0]);
		if(rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, RobotType.MINER.soupLimit);
		} else {
			System.out.println("Failed to deposit soup!");
		}
	}
}
