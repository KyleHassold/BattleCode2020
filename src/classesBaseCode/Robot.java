package classesBaseCode;

import battlecode.common.*;

public abstract class Robot {
	// Variables for all Robots
	RobotController rc;
	
	
	// Super constructor
	protected Robot(RobotController rc) {
		this.rc = rc;
	}
	
	// Force all sub-classes to implement a run() method
	protected abstract void run() throws GameActionException;
	
	// Methods for all Robots
	protected void yield() {
		while(rc.getCooldownTurns() >= 1) {
			Clock.yield();
		}
	}
}
