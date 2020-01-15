package phases2Cleaned;

import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Building extends Robot {
	MapLocation senseStopLoc;

	protected Building(RobotController rc) {
		super(rc);
	}
	
	protected HashMap<MapLocation, int[]> senseInRange() throws GameActionException {
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		if(senseStopLoc == new MapLocation(-1, -1)) {
			return results;
		}
		
		MapLocation loc = rc.getLocation();
		int rSq = rc.getType().sensorRadiusSquared;
		MapLocation senseLoc;

		for(int y = Math.max(loc.y - (int) (Math.pow(rSq, 0.5)), senseStopLoc.y); y <= Math.min(loc.y + (int) (Math.pow(rSq, 0.5)), rc.getMapHeight()); y++) {
			for(int x = Math.max(loc.x - (int) (Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5)), senseStopLoc.x); x <= Math.min(loc.x + (int) ((Math.pow(rSq - Math.pow(y - loc.y, 2), 0.5))), rc.getMapWidth()); x++) {
				senseLoc = new MapLocation(x, y);
				if(!map.containsKey(senseLoc) && rc.canSenseLocation(senseLoc)) {
					results.put(senseLoc, new int[] {rc.senseFlooding(senseLoc) ? 1 : 0, rc.senseElevation(senseLoc), rc.senseSoup(senseLoc), 0});
				}
				if(Clock.getBytecodesLeft() < 500) {
					senseStopLoc = new MapLocation(senseLoc.x, senseLoc.y);
					return results;
				}
			}
		}
		senseStopLoc = new MapLocation(-1, -1);
		return results;
	}

}
