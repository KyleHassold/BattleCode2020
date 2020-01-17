package phases2Cleaned;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import battlecode.common.*;

public class Miner extends Unit {
	MapLocation loc;
	MapLocation target;
	MapLocation bestRefLoc;
	int bestRefScore = 0;

	protected Miner(RobotController rc) throws GameActionException {
		super(rc);
		HQs[0] = findAdjacentRobot(RobotType.HQ, null);
		int robots = 2; // Fix
		if(robots == 2) {
			target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, HQs[0].y);
		} else if(robots == 3) {
			target = new MapLocation(HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		} else if(robots == 4) {
			target = new MapLocation(rc.getMapWidth() - 1 - HQs[0].x, rc.getMapHeight() - 1 - HQs[0].y);
		} else {
			System.out.println("Error: Targeting systems have failed!");
		}
	}

	@Override
	protected void run() throws GameActionException {
		if(target != null) {
			runSearcher();
		}
		
		runSoup(false);
	}
	
	private void runSearcher() throws GameActionException {
		System.out.println("I'm a Searcher!");
		checkTransactions(); // Always check transactions when round starts for most updated data
		bestRefLoc = target;
		
		while(HQs[1] == null && !rc.canSenseLocation(target)) {
			moveCloser(target, true);
			
			HashMap<MapLocation, Integer> sensed = newSensor();
			for(MapLocation loc : sensed.keySet()) {
				if(refs[1] != null && rc.canSubmitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)}, 1)) {
					rc.submitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)}, 1);
				}
				
				int potScore = getRefineryScore(target, loc, soup.get(loc));
				if(getRSquared(HQs[0], loc) > RobotType.REFINERY.pollutionRadiusSquared * 2 && potScore > bestRefScore) {
					bestRefLoc = loc;
					bestRefScore = potScore;
				}
			}
			
			Clock.yield();
			
			checkTransactions(); // Always check transactions when round starts for most updated data
		}
		
		RobotInfo enemyHQ = rc.senseRobotAtLocation(target);
		if(enemyHQ != null && enemyHQ.team != rc.getTeam() && enemyHQ.type == RobotType.HQ) {
			HQs[1] = new MapLocation(enemyHQ.location.x, enemyHQ.location.y);

			int[] message = new int[] {21, enemyHQ.location.x, enemyHQ.location.y};
			if(rc.canSubmitTransaction(message, 1)) {
				rc.submitTransaction(message, 1);
			}
		} else {
			runBuilderMiner();
		}
	}
	
	private void runBuilderMiner() throws GameActionException {
		System.out.println("I'm a Builder!");
		while(phase == 1) {
			checkTransactions();
			
			if(Math.random() > 0.25 || HQs[1] == null) {
				moveRandom();
			} else {
				moveCloser(HQs[1], true);
			}
			
			HashMap<MapLocation, Integer> sensed = newSensor();
			for(MapLocation loc : sensed.keySet()) {
				if(refs[1] != null && rc.canSubmitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)}, 1)) {
					rc.submitTransaction(new int[] {117290, loc.x, loc.y, sensed.get(loc)}, 1);
				}
				
				int potScore = getRefineryScore(target, loc, soup.get(loc));
				if(getRSquared(HQs[0], loc) > RobotType.REFINERY.pollutionRadiusSquared * 2 && potScore > bestRefScore) {
					bestRefLoc = loc;
					bestRefScore = potScore;
				}
			}
			
			Clock.yield();
		}
		
		while(getRSquared(rc.getLocation(), bestRefLoc) > 2) {
			checkTransactions();
			
			moveCloser(bestRefLoc, false);
		}
		
		while(true) {
			checkTransactions();
			
			if(rc.getTeamSoup() >= 210 && buildRobot(RobotType.REFINERY, Direction.NORTH)) {
				MapLocation loc = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
				map.put(loc, new int[] {0, 0, 0, 2});
				if(rc.canSubmitTransaction(new int[] {22, loc.x, loc.y}, 10)) {
					rc.submitTransaction(new int[] {22, loc.x, loc.y}, 10);
				}
				if(refs[0] == null) {
					refs[0] = loc;
				} else {
					refs[1] = loc;
					runSoup(true);
				}
				break;
			}
			Clock.yield();
		}
	}
	
	private void runSoup(boolean build) throws GameActionException {
		System.out.println("I'm a Miner");
		target = null;
		while(!build || refs[1] == null) {
			getSoup();
			returnSoup();
		}
		
		buildDesSch();
		
		while(desSch == null) {
			getSoup();
			returnSoup();
		}
		
		buildVap();
		
		while(true) {
			getSoup();
			returnSoup();
		}
	}

	// Aux
	
	private void getSoup() throws GameActionException {
		//System.out.println("Soup target: " + target);
		while(target == null || !rc.canSenseLocation(target)) {
			checkTransactions();
			loc = rc.getLocation();
			
			if(!soup.isEmpty() && target == null) {
				target = getBestSoup(); //Check this
				moveCloser(target, false);
				System.out.println("Soup target: " + target);
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
						target = null;
					}
				}
			} else {
				moveCloser(target, false);
			}
			
			HashMap<MapLocation, Integer> newSoup = newSensor();
			if(!newSoup.isEmpty() && (target == null || !rc.canSenseLocation(target))) {
				target = newSoup.keySet().toArray(new MapLocation[0])[0];
			}
			
			Clock.yield();
		}
		
	}

	private void returnSoup() throws GameActionException {
		MapLocation ref = findAdjacentRobot(RobotType.REFINERY, rc.getTeam());
		while(ref == null) {
			checkTransactions();
			moveCloser(closestRef(), false);
			//System.out.println("Closest Refinery: " + closestRef());
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

	private void buildDesSch() throws GameActionException {
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
	
	private void buildVap() throws GameActionException {
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
	
	private MapLocation getBestSoup() {
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
	
	private MapLocation closestRef() {
		MapLocation loc = rc.getLocation();
		MapLocation closest = HQs[0];
		
		if(desSch != null || refs[0] != null && getRSquared(loc, refs[0]) <= getRSquared(loc, closest)) {
			closest = refs[0];
		}
		if(refs[1] != null && getRSquared(loc, refs[1]) <= getRSquared(loc, closest)) {
			closest = refs[1];
		}
		return closest;
	}

	private int getRefineryScore(MapLocation target, MapLocation loc, int soup) {
		return soup / Math.max(getRSquared(target, loc) - 10, 1);
	}

}
