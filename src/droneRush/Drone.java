package droneRush;

import battlecode.common.*;
import java.util.*;

public class Drone extends Unit {
	String job;
	MapLocation water;

	public Drone(RobotController rc) {
		super(rc);	
	}

	@Override
	protected void run() {
		//readTransactions();
		if (HQs[1] == null)
			runScout();
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
	
	private void runAttack() {
		MapLocation target = null;
		MapLocation water = null;
		Team opp = rc.getTeam().opponent();
		int rob_id = -1;
		boolean carrying = false;
		Direction heading = null;

		while (true) {
			try {
				MapLocation myLoc = rc.getLocation();
				
				System.out.println("Water Status: " + water);
				if (water == null)
					for (Direction dir : directions)
						if (rc.canSenseLocation(myLoc.add(dir)) &&  rc.senseFlooding(myLoc.add(dir)))
							water = myLoc.add(dir);				
				
				System.out.println("Target Status: " + target);
				if (target == null) {
					RobotInfo[] info = rc.senseNearbyRobots();
					for (RobotInfo rob : info)
						if (rob.getTeam() == opp && (rob.getType() == RobotType.MINER || rob.getType() == RobotType.LANDSCAPER)) {
							target = rob.getLocation();
							rob_id = rob.getID();
						}
				}
			
				System.out.println("Carrying Status: " + carrying);	
				if (!carrying && rc.canPickUpUnit(rob_id)) {
					rc.pickUpUnit(rob_id);
					carrying = true;
				}

				if (target != null && myLoc.distanceSquaredTo(target) <= 2) 
					target = null;

				if (carrying) {
					for (Direction dir : directions) {
						MapLocation dest = myLoc.add(dir);
						if (rc.canSenseLocation(dest) && rc.senseFlooding(dest) 
							&& rc.canDropUnit(dir)) {
							rc.dropUnit(dir);
							target = null;
							carrying = false;
						}
					}
				}

				if (carrying && water != null) {
					Direction dir = myLoc.directionTo(water);
					if (!pathFindTo(myLoc.add(dir).add(dir), 3, false, "Adj"))
						water = null;

				} else if (!carrying && target != null) {
					Direction dir = myLoc.directionTo(target);
					if (!pathFindTo(myLoc.add(dir).add(dir), 3, false, "Adj"))
						target = null;

				} else {
					if (heading == null) 
						heading = randomValidDirection();
					
					System.out.println("Heading Status: " + heading);
					
					if (!pathFindTo(myLoc.add(heading), 3, false, "On"))
						heading = null;
				}

			} catch (GameActionException e) {
				System.out.println("Failure: Drone.runAttack()\nFailed to attack miners");
				e.printStackTrace();
			}
		}
	}
	
	protected Direction randomValidDirection() {
    	ArrayList<Direction> list = new ArrayList<Direction>();
        for (Direction dir : directions) 
			if (rc.canMove(dir)) list.add(dir);  
		
 		if (list.isEmpty()) {
			System.out.println("Oh no I am stuck"); 
			return randomDirection();
		} else
			return list.get((int) (Math.random() * list.size()));	
    }

	protected Direction randomDirection() {
        return directions.get((int) (Math.random() * directions.size()));
    }
}
