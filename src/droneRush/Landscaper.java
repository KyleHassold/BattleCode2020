package droneRush;

import battlecode.common.*;

public class Landscaper extends Unit {

	public Landscaper(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		try {
			int spot = moveToSpot();
			barricade(spot);
		} catch(GameActionException e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
	}
	
	private int moveToSpot() throws GameActionException {
		MapLocation[] spots = new MapLocation[4];
		spots[0] = new MapLocation(HQs[0].x + 1, HQs[0].y);
		spots[1] = new MapLocation(HQs[0].x - 1, HQs[0].y + 1);
		spots[2] = new MapLocation(HQs[0].x, HQs[0].y - 1);
		spots[3] = new MapLocation(HQs[0].x - 2, HQs[0].y);
		int currSpot = 0;
		while(!loc.equals(spots[currSpot])) {
			rc.setIndicatorDot(spots[currSpot], 255, 0, 0);
			if(!(rc.canSenseLocation(spots[currSpot]) && rc.senseRobotAtLocation(spots[currSpot]) != null && rc.senseRobotAtLocation(spots[currSpot]).type == RobotType.LANDSCAPER)) {
				moveCloser(spots[currSpot], false);
			} else {
				currSpot = (currSpot + 1) % 4;
				Clock.yield();
			}
			yield();
		}
		
		return currSpot;
	}
	
	private void barricade(int spot) throws GameActionException {
		Direction[] placeSpots = new Direction[3];
		Direction mineFrom;
		if(spot == 1 || spot == 2) {
			placeSpots[0] = Direction.EAST;
			placeSpots[1] = Direction.CENTER;
			placeSpots[2] = Direction.WEST;
			if(spot == 1) {
				mineFrom = Direction.NORTHEAST;
			} else {
				mineFrom = Direction.SOUTH;
			}
		} else {
			placeSpots[0] = Direction.NORTH;
			placeSpots[1] = Direction.CENTER;
			placeSpots[2] = Direction.SOUTH;
			if(spot == 0) {
				mineFrom = Direction.EAST;
			} else {
				mineFrom = Direction.WEST;
			}
		}
		
		Direction needDirt = placeSpots[0];
		while(true) {
			for(Direction dir : placeSpots) {
				if(rc.senseElevation(rc.adjacentLocation(dir)) < rc.senseElevation(rc.adjacentLocation(needDirt))) {
					needDirt = dir;
				}
			}
			
			while(rc.canDigDirt(mineFrom)) {
				rc.digDirt(mineFrom);
				yield();
			}
			
			while(rc.canDepositDirt(needDirt)) {
				rc.depositDirt(needDirt);
				yield();
			}
		}
	}
}
