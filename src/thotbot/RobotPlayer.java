package thotbot;
import battlecode.common.*;

import java.util.*;


// ----------------------------- INFO ---------------------------

/*
BLOCKCHAIN CODES:
  117290 = Soup
  117291 = HQ Location
  117292 = Mine Location
  117293 = Design School Location
  117294 = Vaporator Location
BLOCKCHAIN PROTOCOL:
  for soup:
      [code, x, y, amountOfSoup, -1, -1, -1]
  for other stuff:
      [code, x, y, -1, -1, -1, -1]
MAP INT[]:
	Water, Dirt, Soup, Base
	Building:
		Positive: Friendly
		Negative: Enemy
		HQ: 1
		Refinery: 2
		Net Gun: 3
		Vaporator: 4
		Design School: 5
		Fulfillment Center: 6
 */








public strictfp class RobotPlayer {

    static RobotController rc;
    static List<Direction> directions;
//    static Direction[] dArr = {
//            Direction.NORTH,
//            Direction.NORTHEAST,
//            Direction.EAST,
//            Direction.SOUTHEAST,
//            Direction.SOUTH,
//            Direction.SOUTHWEST,
//            Direction.WEST,
//            Direction.NORTHWEST
//    };
    static Direction[] dArr = {
            Direction.NORTHWEST,
            Direction.WEST,
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.EAST,
            Direction.NORTHEAST,
            Direction.NORTH
    };

    static int turnCount;
    static Random rand = new Random();
    static MapLocation target = null;
    static String purpose = "";
    static Direction startDirection = null;

    static MapLocation HQ = null;
    static List<MapLocation> pathBack = new ArrayList<MapLocation>();
    static List<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static Map<MapLocation, Integer> soupCounts = new HashMap<MapLocation, Integer>();
    static List<MapLocation> refLocs = new ArrayList<MapLocation>();

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        turnCount = 0;
        directions = Arrays.asList(dArr);

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {

            turnCount += 1;
            try {
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                checkTransactions();
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {

        while (rc.getRobotCount() <= 5)
        {
            // Get list of possible directions where miners can be spawned
            List<Direction> directionsToSpawn= getSpawnableDirections(RobotType.MINER);

            // If you can spawn any miners at all
            if (directionsToSpawn.size() > 0)
            {
                // Pick a random direction and spawn it
                Direction directionChoice = directionsToSpawn.get(rand.nextInt(directionsToSpawn.size()));
                System.out.println("Spawning miner in direction: " + directionChoice.toString());
                rc.buildRobot(RobotType.MINER, directionChoice);
            }
            else
            {
                System.out.println("Cannot spawn any miners yet. Standing by.");
            }

            Clock.yield();
        }

    }

    static void runMiner() throws GameActionException {

        // Get start direction if already null
        if (startDirection == null)
            startDirection = getStartDirection();
        System.out.println("Miner start direction: " + startDirection.toString());

        if (rc.getRoundNum() > 300 && refLocs.size() <= 1)
        {
            if (rc.canBuildRobot(RobotType.REFINERY, Direction.NORTH))
            {
                rc.buildRobot(RobotType.REFINERY, Direction.NORTH);
                refLocs.add(new MapLocation(rc.getLocation().x, rc.getLocation().y + 1));
            }
        }

        System.out.println("Known soup locations: " + soupLocs.toString());

        // If soup locations exist:
        if (!soupLocs.isEmpty()) {
            System.out.println("Soup location found! Calculating best soup location...");

            // Get best soup location, set it to target
            target = getBestSoup();
            // Remove it from the list of locations
            System.out.println("Best soup location found at: " + target.toString());


            // Go to soup
            pathfindTo(target);

            // Get which direction the soup is in
            Direction dirToMine = Direction.CENTER;
            for (Direction d : directions) {
                MapLocation dl = new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy);
                if (dl.equals(target)) {
                    dirToMine = d;
                    break;
                }
            }
            System.out.println("Soup has been located in direction: " + dirToMine + " relative to robot location");

            //Keep mining soup until at capacity
            System.out.println("Beginning to mine soup.");

            while (rc.getSoupCarrying() < RobotType.MINER.soupLimit && rc.senseSoup(target) != 0)
            {
                if (rc.canMineSoup(dirToMine))
                {
                    System.out.println("Still mining soup. Capacity at: " + Integer.toString(rc.getSoupCarrying()) + " / " + RobotType.MINER.soupLimit);
                    rc.mineSoup(dirToMine);
                }
                else
                {
                    System.out.println("Can't mine here yet!");
                }
                checkTransactions();
                Clock.yield();
            }
            System.out.println("At capcity! Finding refinery now.");

            if (rc.senseSoup(target) == 0)
                soupLocs.remove(target);





            // Get closest refinery
            MapLocation closestRefinery = getClosestRefinery();
            target = closestRefinery;
            System.out.println("Closest refinery located at: " + target.toString());

            // Go to nearest refinery
            System.out.println("Pathing to nearest refinery");
            pathfindTo(closestRefinery);

            // Get where the refinery is
            Direction dirToDeposit = Direction.CENTER;
            for (Direction d : directions) {
                MapLocation dl = new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy);
                if (dl.equals(target)) {
                    dirToDeposit = d;
                    break;
                }
            }
            System.out.println("Refinery has been located in direction: " + dirToDeposit + " relative to robot location");

            // Deposit soup to refinery
            while (rc.getSoupCarrying() > 0)
            {
                if (rc.canDepositSoup(dirToDeposit)) {
                    System.out.println("Depositing soup to refinery!");
                    rc.depositSoup(dirToDeposit, rc.getSoupCarrying());
                }
            }

            // Set target to null, yield clock
            target = null;
            checkTransactions();
            Clock.yield();

        }
        else // If no soup has been found
        {
            System.out.println("No soup locations currently known.");

            // Scan for some more soup around you
            System.out.println("Looking for soup...");
            checkTransactions();
            checkForSoup();

            // Pick move based on chance

            System.out.println("Moving somewhere else...");
            double chance = rand.nextDouble();

            if (chance < 0.25)
            {
                // Move in starting direction
                System.out.println("Moving in starting direction.");
                if (canReallyMove(startDirection))
                    rc.move(startDirection);
            }
            else
            {
                // Move in random direction [FIX]
                System.out.println("Moving in random direction.");
                Direction randomDir = directions.get(rand.nextInt(directions.size()));
                if (canReallyMove(randomDir))
                    rc.move(randomDir);
            }
        }
        checkTransactions();
        Clock.yield();
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    // ---------------------------------------- HELPER FUNCTIONS --------------------------------

    private static Direction getDirection(MapLocation start, MapLocation end) {
        double angle = (Math.atan2(end.y - start.y, end.x - start.x))/Math.PI + 1;

        if(angle > 15.0/8 || angle <= 1.0/8) {
            return Direction.WEST;
        } else if(angle > 13.0/8) {
            return Direction.NORTHWEST;
        } else if(angle > 11.0/8) {
            return Direction.NORTH;
        } else if(angle > 9.0/8) {
            return Direction.NORTHEAST;
        } else if(angle > 7.0/8) {
            return Direction.EAST;
        } else if(angle > 5.0/8) {
            return Direction.SOUTHEAST;
        } else if(angle > 3.0/8) {
            return Direction.SOUTH;
        } else {
            return Direction.SOUTHWEST;
        }
    }

    private static List<Direction> getSpawnableDirections(RobotType rt)
    {
        List<Direction> retDir = new ArrayList<Direction>();
        for (Direction d : directions)
        {
            if (rc.canBuildRobot(rt, d))
            {
                retDir.add(d);
            }
        }
        return retDir;
    }

    private static int getRSquared(MapLocation start, MapLocation end) {
        return (int) (Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
    }

    private static MapLocation getBestSoup()
    {
        soupLocs.sort(new Comparator<MapLocation>() {
            double ks= 0.1;
            double kd = 1.0;
            public int compare(MapLocation o1, MapLocation o2) {
                return (int)((getRSquared(o1, o2))*kd) - (int)((soupCounts.get(o2) - soupCounts.get(o1))*ks);
            }
        });
        return soupLocs.get(0);
    }

    private static boolean canReallyMove(Direction d) throws GameActionException
    {
        if (rc.canMove(d))
        {
            return !rc.senseFlooding(new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy));
        }
        return false;
    }

    private static Direction getStartDirection()
    {
        for (RobotInfo rb : rc.senseNearbyRobots())
        {
            if (rb.getType() == RobotType.HQ)
            {
                HQ = rb.getLocation();
                refLocs.add(rb.getLocation());
                Direction opDir = getDirection(rc.getLocation(), rb.getLocation());
                Direction retDir = directions.get((directions.indexOf(opDir) + 4) % 8);
                return retDir;
            }
        }
        return null;
    }

    private static MapLocation getClosestRefinery()
    {
        refLocs.sort(new Comparator<MapLocation>() {
            public int compare(MapLocation o1, MapLocation o2) {
                return getRSquared(rc.getLocation(), o2) - getRSquared(rc.getLocation(), o1);
            }
        });
        return refLocs.get(0);
    }

    private static List<MapLocation> getAdjacencies()
    {
        List<MapLocation> retAdj = new ArrayList<MapLocation>();
        for (Direction d : directions)
        {
            retAdj.add(new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy));
        }
        return retAdj;
    }


    private static void pathfindTo(MapLocation tg) throws GameActionException
    {
        while (true)
        {
            // Get adjacent spots, check if target is one of them, stop pathing if so
            List<MapLocation> adjacencies = getAdjacencies();
            if (adjacencies.contains(tg))
                break;

            // Map adjacencies to their locations
            Map<MapLocation, Direction> locToDir = new HashMap<MapLocation, Direction>();
            int ct = 0;
            for (MapLocation adj : adjacencies)
            {
                locToDir.put(adj, directions.get(ct));
                ct++;
            }

            // Sort adjacencies by how far RSquared they are from the target
            adjacencies.sort(new Comparator<MapLocation>() {
                public int compare(MapLocation o1, MapLocation o2) {
                    return getRSquared(o1, tg) - getRSquared(o2, tg);
                }
            });


            // Get current distance from target, the adjacency with smallest distance, and its distance value
            int currentDistance = getRSquared(rc.getLocation(), tg);
            int possiblyCloser = getRSquared(adjacencies.get(0), tg);
            Direction dirToMove = locToDir.get(adjacencies.get(0));

            System.out.println("Directional mode! Best direction to go is: " + dirToMove.toString());

            // Wait for any cooldown
            waitForCooldown();


            // Check if there are no obstacles on the smallest adjacency
            if (canReallyMove(dirToMove))
            {
                // Move if possible, continue loop
                rc.move(dirToMove);
                checkTransactions();
                Clock.yield();
            }
            else
            {
                System.out.println("Oh no! There is an obstacle due " + dirToMove.toString());
                // Turn left until there is an open space
                Direction currentOrientation = Direction.CENTER;
                for (int i = 0; i < directions.size(); i++)
                {
                    Direction nc = directions.get((directions.indexOf(dirToMove) + i)%8);
                    System.out.println("Checking " + Integer.toString(i) + "turn to the left in direction " + nc.toString());
                    if (canReallyMove(nc))
                    {
                        System.out.println("New current orientation is " + nc.toString());
                        currentOrientation = nc;
                        waitForCooldown();
                        rc.move(currentOrientation);
                        break;
                    }
                }


                // Follow the wall/obstacle until you are closer than before


                while (getRSquared(rc.getLocation(), tg) > currentDistance)
                {

                    if (!obstaclesAroundMe())
                        break;

                    System.out.println("Following left wall! Current forward orientation is: " + currentOrientation.toString());
                    System.out.println("My distance is " + Integer.toString(getRSquared(rc.getLocation(), tg)) + ", the distance to beat is " + Integer.toString(currentDistance));

                    // Get left and right directions relative to current orientation
                    Direction leftDir = directions.get((directions.indexOf(currentOrientation) + 2)%8);
                    Direction rightDir = directions.get((directions.indexOf(currentOrientation) + 6)%8);
                    System.out.println("Left of me is " + leftDir.toString() + ", and right of me is " + rightDir.toString());


                    boolean mf = canReallyMove(currentOrientation);
                    boolean ml = canReallyMove(leftDir);
                    boolean mr = canReallyMove(rightDir);

                    if (ml && !mr && mf)
                    {
                        currentOrientation = leftDir;
                        currentOrientation = directions.get((directions.indexOf(currentOrientation) + 2)%8);
                    }
                    else if (ml) // If you can go left
                    {
                        // Go left
                        System.out.println("Moving left");
                        currentOrientation = leftDir;
                        waitForCooldown();
                        rc.move(leftDir);
                    }
                    else if (!mf && !ml) // If you can only go right
                    {
                        // Turn right
                        System.out.println("Moving right");
                        currentOrientation = rightDir;
                    }
                    else if (mf) // If you can go straight
                    {
                        // Go straight
                        System.out.println("Moving straight");
                        waitForCooldown();
                        rc.move(currentOrientation);
                    }

                    checkTransactions();
                    Clock.yield();
                }
            }

            checkTransactions();
            Clock.yield();
        }
    }

    private static void checkTransactions () throws GameActionException
    {
        //System.out.println("Looking for current data...");
        Transaction[] transList = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction t : transList)
        {
            int[] message = t.getMessage();
            if (message[0] == 117290)
            {
                MapLocation mark = new MapLocation(message[1], message[2]);
                if (!soupLocs.contains(mark))
                {
                    System.out.println("Soup location received! Adding to currently known locations...");
                    soupLocs.add(mark);
                    soupCounts.put(mark, message[3]);
                }
            }
        }
    }

    private static void waitForCooldown() throws GameActionException
    {
        while (rc.getCooldownTurns() != 0)
        {
            System.out.println("Waiting for cooldown!");
            checkTransactions();
            Clock.yield();
        }
    }

    private static boolean obstaclesAroundMe() throws GameActionException
    {
        List<MapLocation> adjlist = getAdjacencies();
        for (MapLocation a : adjlist)
        {
            if (!canReallyMove(getDirection(rc.getLocation(), a)))
            return true;
        }
        return false;
    }

    private static void checkForSoup() throws GameActionException
    {
        for (int i = -1; i < 2; i++)
        {
            for (int j = -1; j < 2; j++)
            {
                MapLocation sl = new MapLocation(rc.getLocation().x+i, rc.getLocation().y+j);
                if (sl.x >= 0 && sl.x < rc.getMapWidth() && sl.y >= 0 && sl.y < rc.getMapHeight())
                {
                    System.out.println("Looking for soup at location: " + sl.toString());
                    int soupcount = rc.senseSoup(sl);
                    if (soupcount > 0) {
                        System.out.println(Integer.toString(soupcount) + " soup found!");
                        int[] message = {117290, sl.x, sl.y, soupcount};
                        if (rc.canSubmitTransaction(message, 15))
                        {
                            rc.submitTransaction(message, 15);
                            System.out.println("Notified other miners of soup!");
                        }


                        if (!soupLocs.contains(sl))
                        {
                            System.out.println("Adding to currently known locations...");
                            soupLocs.add(sl);
                            soupCounts.put(sl, soupcount);
                        }

                    }
                }
            }
        }
    }





}
