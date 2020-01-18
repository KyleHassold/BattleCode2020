package droneRush;

import battlecode.common.*;

public class Drone extends Unit {
	String job;

	public Drone(RobotController rc) {
		super(rc);
		RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
		boolean nearHq = false;
		boolean nearDs = false;
		for(RobotInfo robo : robots) {
			if(robo.type == RobotType.HQ) {
				nearHq = true;
			} else if(robo.type == RobotType.DESIGN_SCHOOL) {
				nearDs = true;
			}
		}
		if(nearHq && nearDs) {
			job = "Scout";
		} else if(nearHq) {
			job = "Defense";
		} else if(nearDs) {
			job = "Attacker";
		} else {
			job = "Scout";
		}
		System.out.println(job);
	}

	@Override
	protected void run() throws GameActionException {

		while(true) {
			try {
				if(job.equals("Scout")) {
					runScout();
				} else if(job.equals("Defense")) {
					runDefend();
				} else if(job.equals("Attacker")) {
					runAttack();
				} else {
					System.out.println("Failed");
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
		}
	}
	
	private void runScout() throws GameActionException {
		MapLocation[] targets = new MapLocation[3];
		targets[0] = new MapLocation(mapW - HQs[0].x - 1, HQs[0].y);
		targets[1] = new MapLocation(mapW - HQs[0].x - 1, mapH - HQs[0].y - 1);
		targets[2] = new MapLocation(HQs[0].x, mapH - HQs[0].y - 1);
		int count = 0;
		
		while(count < targets.length) {
			if(rc.canSenseLocation(targets[count])) {
				RobotInfo robo = rc.senseRobotAtLocation(targets[count]);
				if(robo != null && robo.type == RobotType.HQ && robo.team == rc.getTeam().opponent()) {
					HQs[1] = targets[count];
					map.put(targets[count], new int[] {0,0,0,-1});
					int[] message = new int[] {117291, targets[count].x, targets[count].y, -1, -1, -1, -1};
					while(!rc.canSubmitTransaction(message, 10)) {
						yield();
					}
					rc.submitTransaction(message, 10);
					job = "Defense";
					break;
				} else {
					System.out.println("Next");
					count++;
				}
			} else {
				moveCloser(targets[count], true);
				yield();
			}
		}
	}
	
	private void runDefend() {
		while(true) {
			Clock.yield();
		}
	}
	
	private void runAttack() {
		
	}
}
