package droneRush;

import battlecode.common.*;
import java.util.*;

public class Drone extends Unit {
	String job;
	
	public Drone(RobotController rc) {
		super(rc);	
	}

	@Override
	protected void run() {
		//readTransactions();
		if HQs[1] == null:
			runScout();
		else 
			runAttack();
	}
	
	private void runScout() {
		ArrayList<MapLocation> targets = new ArrayList<MapLocation>();
		targets.add(new MapLocation(mapW - HQs[0].x - 1, HQs[0].y));
		targets.add(new MapLocation(mapW - HQs[0].x - 1, mapH - HQs[0].y - 1));
		targets.add(new MapLocation(HQs[0].x, mapH - HQs[0].y - 1));
		Collections.shuffle(targets);
		int count = 0;
			
		while(count < targets.size() && HQs[1] == null) {
			// See if the drone can sense the HQ
			if(rc.canSenseLocation(targets.get(count))) {
				// Sense the location
				RobotInfo robo = null;
				try {
					robo = rc.senseRobotAtLocation(targets.get(count));
				} catch (GameActionException e) {
					System.out.println("Error: Drone.runScout() Failed!\nrc.senseRobotAtLocation(" + targets.get(count) + ") Failed!");
					e.printStackTrace();
				}
				
				// Check if it the HQ
				if(robo != null && robo.type == RobotType.HQ && robo.team == rc.getTeam().opponent()) {
					HQs[1] = targets.get(count);
					map.put(targets.get(count), new int[] {0,0,0,-1});
					
					submitTransaction(new int[] {teamCode, targets.get(count).x, targets.get(count).y, -1, -1, -1, 1}, 10, true);
					return;
				} else {
					// Move on to the next potential spot
					count++;
				}
			} else {
				// Move to the target
				pathFindTo(targets.get(count), mapW + mapH, true, "In Range");
			}
		}
		
		System.out.println("Failure: Drone.runScout()\nFailed to find enemy HQ");
	}
}
