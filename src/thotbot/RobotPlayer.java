package thotbot;
import battlecode.common.*;

import javax.security.auth.login.AccountLockedException;
import java.util.*;


// ----------------------------- INFO ---------------------------

/*
BLOCKCHAIN CODES:
  117290 = Soup
  117291 = Refinery location
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
    static int minercount = 0;
    static int landscapercount = 0;
    static boolean beginDigging = false;
    static Random rand = new Random();
    static MapLocation target = null;
    static String purpose = "";
    static Direction startDirection = null;

    static MapLocation HQ = null;
    static List<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static List<MapLocation> expendedSoupLocs = new ArrayList<MapLocation>();
    static Map<MapLocation, Integer> soupCounts = new HashMap<MapLocation, Integer>();
    static List<MapLocation> refLocs = new ArrayList<MapLocation>();
    static List<MapLocation> designSchoolLocs = new ArrayList<MapLocation>();
    static List<int[]> outgoingMessages = new ArrayList<int[]>();


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

        while (minercount < 4)
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
                minercount += 1;
            }
            else
            {
                System.out.println("Cannot spawn any miners yet. Standing by.");
            }

            checkTransactions();
            Clock.yield();
        }
        if (rc.getRoundNum() % 100 == 0)
        {
            List<MapLocation> adj = getAdjacencies(rc.getLocation());
            List<RobotInfo> cornerSense = Arrays.asList(rc.senseRobotAtLocation(adj.get(0)), rc.senseRobotAtLocation(adj.get(2)), rc.senseRobotAtLocation(adj.get(4)), rc.senseRobotAtLocation(adj.get(6)));


            if (!cornerSense.contains(null))
            {
                boolean areLandscapers = true;
                for (RobotInfo corner : cornerSense)
                {
                    if (corner.getType() != RobotType.LANDSCAPER)
                        areLandscapers = false;
                }

                if (areLandscapers)
                {
                    int[] message = {117295, -1, -1, -1, -1, -1, -1};
                    if (rc.canSubmitTransaction(message, 15))
                    {
                        rc.submitTransaction(message, 15);
                        System.out.println("You're clear to start digging.");
                    }
                    else
                    {
                        outgoingMessages.add(message);
                    }
                }

            }

            int[] message = new int[]{117294, rc.getLocation().x,rc.getLocation().y, -1, -1, -1, -1};
            if (rc.canSubmitTransaction(message, 15))
            {
                rc.submitTransaction(message, 15);
                //System.out.println("Notified other landscapers of the HQ!");
            }
            else
            {
                outgoingMessages.add(message);
            }
            checkTransactions();
            Clock.yield();
        }

    }

    static void runMiner() throws GameActionException
    {

        // Get start direction if already null
        if (startDirection == null)
            startDirection = getStartDirection();
        //System.out.println("Miner start direction: " + startDirection.toString());


        maybeBuildRefinery();
        maybeBuildDesignSchool();

        //System.out.println("Known soup locations: " + soupLocs.toString());

        // If soup locations exist:
        if (!soupLocs.isEmpty()) {
            //System.out.println("Soup location found! Calculating best soup location...");

            // Get best soup location, set it to target
            target = getBestSoup();
            // Remove it from the list of locations
            //System.out.println("Best soup location found at: " + target.toString());


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
            //System.out.println("Soup has been located in direction: " + dirToMine + " relative to robot location");

            //Keep mining soup until at capacity
            //System.out.println("Beginning to mine soup.");

            while (rc.getSoupCarrying() < RobotType.MINER.soupLimit && rc.senseSoup(target) != 0)
            {
                if (rc.canMineSoup(dirToMine))
                {
                    //System.out.println("Still mining soup. Capacity at: " + Integer.toString(rc.getSoupCarrying()) + " / " + RobotType.MINER.soupLimit);
                    rc.mineSoup(dirToMine);
                }
                else
                {
                    //System.out.println("Can't mine here yet!");
                }
                checkTransactions();
                Clock.yield();
            }
            //System.out.println("At capcity! Finding refinery now.");

            if (rc.senseSoup(target) == 0)
            {
                soupLocs.remove(target);
                expendedSoupLocs.add(target);

                //System.out.println("Soup has been expended!");
                int[] message = {117292, target.x, target.y, -1, -1, -1, -1};
                if (rc.canSubmitTransaction(message, 15))
                {
                    rc.submitTransaction(message, 15);
                    //System.out.println("Notified other miners that soup is no longer available here!");
                }
                else
                {
                    outgoingMessages.add(message);
                }

            }
            else
            {
                // Get closest refinery
                MapLocation closestRefinery = getClosestRefinery();
                target = closestRefinery;
                //System.out.println("Closest refinery located at: " + target.toString());

                // Go to nearest refinery
                //System.out.println("Pathing to nearest refinery");
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
                //System.out.println("Refinery has been located in direction: " + dirToDeposit + " relative to robot location");

                // Deposit soup to refinery
                while (rc.getSoupCarrying() > 0)
                {
                    if (rc.canDepositSoup(dirToDeposit)) {
                        //System.out.println("Depositing soup to refinery!");
                        rc.depositSoup(dirToDeposit, rc.getSoupCarrying());
                    }
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

            // Pick random location, go towards that

            System.out.println("Moving somewhere else...");
            int randx = rand.nextInt(rc.getMapWidth());
            int randy = rand.nextInt(rc.getMapWidth());
            MapLocation randLoc = new MapLocation(randx, randy);
            soupPathfindTo(randLoc, 25);
        }
        checkTransactions();
        Clock.yield();
    }

    static void runRefinery() throws GameActionException
    {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
//        if (rc.getRoundNum() % 20 == 0)
//        {
//            System.out.println("Refinery here. Broadcasting my location!");
//            int[] message = {117291, target.x, target.y, -1, -1, -1, -1};
//            if (rc.canSubmitTransaction(message, 15))
//            {
//                rc.submitTransaction(message, 15);
//                System.out.println("Notified other miners of my location!");
//            }
//            else
//            {
//                outgoingMessages.add(message);
//            }
//        }
//        checkTransactions();
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException
    {
        while (landscapercount < 4)
        {
            // Get list of possible directions where miners can be spawned
            List<Direction> directionsToSpawn= getSpawnableDirections(RobotType.LANDSCAPER);

            // If you can spawn any miners at all
            if (directionsToSpawn.size() > 0)
            {
                // Pick a random direction and spawn it
                Direction directionChoice = directionsToSpawn.get(rand.nextInt(directionsToSpawn.size()));
                System.out.println("Spawning landscaper in direction: " + directionChoice.toString());
                rc.buildRobot(RobotType.LANDSCAPER, directionChoice);
                landscapercount += 1;
            }
            else
            {
                System.out.println("Cannot spawn any landscapers yet. Standing by.");
            }

            checkTransactions();
            Clock.yield();
        }
    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException
    {

        while (HQ == null)
        {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (RobotInfo nr : nearbyRobots)
            {
                if (nr.getType() == RobotType.HQ)
                {
                    HQ = nr.getLocation();
                    System.out.println("Found HQ at " + HQ.toString());

                    int[] message = {117294, HQ.x, HQ.y, -1, -1, -1, -1};
                    if (rc.canSubmitTransaction(message, 15))
                    {
                        rc.submitTransaction(message, 15);
                        System.out.println("Notified other landscapers of the HQ!");
                    }
                    else
                    {
                        outgoingMessages.add(message);
                    }

                }
            }
            checkTransactions();
            Clock.yield();
        }



        List<MapLocation> hqa = getAdjacencies(HQ);
        List<MapLocation> adjHQ = Arrays.asList(hqa.get(0), hqa.get(2), hqa.get(4), hqa.get(6));

        while (!adjHQ.contains(rc.getLocation()))
        {
            if (!adjHQ.contains(rc.getLocation()))
            {
                for (MapLocation m : adjHQ)
                {
                    System.out.println("Landscaper going to " + m.toString() + " for positioning.");
                    target = m;
                    pathfindTo(m);

                    Direction dirToMove = Direction.CENTER;
                    MapLocation dl = rc.getLocation();
                    for (Direction d : directions)
                    {
                        dl = new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy);
                        System.out.println("Checking location " + dl.toString() + " to see if it is the target of " + target.toString());
                        if (dl.equals(target))
                        {
                            //System.out.println("Found spot to move to for positioning!");
                            dirToMove = d;
                            break;
                        }
                    }
                    RobotInfo maybeOtherRobot = rc.senseRobotAtLocation(dl);
                    while (maybeOtherRobot != null && maybeOtherRobot.getType() != RobotType.LANDSCAPER)
                    {
                        maybeOtherRobot = rc.senseRobotAtLocation(dl);
                        checkTransactions();
                        Clock.yield();
                    }

                    waitForCooldown();
                    if (canReallyMove(dirToMove))
                    {
                        //System.out.println("Moving into position at location " + dl.toString());
                        rc.move(dirToMove);
                        break;
                    }
                }
                checkTransactions();
                Clock.yield();
            }
        }

        System.out.println("Landscaper in place!");
        while (!beginDigging)
        {
            checkTransactions();
            Clock.yield();
        }



        Direction forwardDir = getDirection(rc.getLocation(), HQ);
        Direction oppDir = directions.get((directions.indexOf(forwardDir) + 4)%8);

        List<Direction> dirsToDig = Arrays.asList(oppDir,
                                                        directions.get((directions.indexOf(oppDir) + 1)%8),
                                                        directions.get((directions.indexOf(oppDir) + 7)%8));
        List<Direction> dirsToDispense = Arrays.asList(Direction.CENTER,
                                                        directions.get((directions.indexOf(forwardDir) + 1)%8),
                                                        directions.get((directions.indexOf(forwardDir) + 7)%8));

        while (true)
        {
            dirsToDispense.sort(new Comparator<Direction>() {
                public int compare(Direction o1, Direction o2)  {
                    try {
                        return rc.senseElevation(new MapLocation(rc.getLocation().x+o1.dx, rc.getLocation().y+o1.dy)) - rc.senseElevation(new MapLocation(rc.getLocation().x+o2.dx, rc.getLocation().y+o2.dy));
                    } catch (GameActionException e) {
                        return -1;
                    }
                }
            });

            Collections.shuffle(dirsToDig);
            System.out.println("Places to dig: " + dirsToDig.toString());
            for (Direction digDir : dirsToDig)
            {
                if (rc.canDigDirt(digDir))
                {
                    System.out.println("Digging dirt in direction " + digDir.toString());
                    rc.digDirt(digDir);
                    Clock.yield();
                    break;
                }
            }

            if (rc.canDepositDirt(dirsToDispense.get(0)))
            {
                System.out.println("Depositing dirt on my location!");
                rc.depositDirt(dirsToDispense.get(0));
                Clock.yield();
            }
            Clock.yield();
        }

    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    // ---------------------------------------- HELPER FUNCTIONS --------------------------------

    private static Direction getDirection(MapLocation start, MapLocation end)
    {
        double angle = getAngle(start, end);

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

    private static double getAngle(MapLocation start, MapLocation end)
    {
        return (Math.atan2(end.y - start.y, end.x - start.x))/Math.PI + 1;
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

    private static int getRSquared(MapLocation start, MapLocation end)
    {
        return (int) (Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
    }

    private static MapLocation getBestSoup()
    {
        soupLocs.sort(new Comparator<MapLocation>() {
            double ks= 0.1;
            double kd = 1.0;
            public int compare(MapLocation o1, MapLocation o2) {
                double score = ((getRSquared(rc.getLocation(), o2) - getRSquared(rc.getLocation(), o1))*kd) - (int)((soupCounts.get(o2) - soupCounts.get(o1))*ks);
                return (int)score;
            }
        });
        return soupLocs.get(soupLocs.size()-1);
    }

    private static boolean canReallyMove(Direction d) throws GameActionException
    {
        MapLocation newLoc = new MapLocation(rc.getLocation().x + d.dx, rc.getLocation().y + d.dy);
        boolean stockCanMove = rc.canMove(d);
        boolean noFloodingExists = !rc.senseFlooding(newLoc);
        boolean spaceNotOccupied = (rc.senseRobotAtLocation(newLoc)==null);

//        if (stockCanMove)
//            System.out.println("Stock move says I can move in direction " + d.toString() + " to location " + newLoc.toString());
//        else
//            System.out.println("Stock move says I CANNOT move in direction " + d.toString() + " to location " + newLoc.toString());
//
//        if (noFloodingExists)
//            System.out.println("There is NO flooding at location " + newLoc.toString());
//        else
//            System.out.println("Oh no! There IS flooding at location " + newLoc.toString());
//
//        if (spaceNotOccupied)
//            System.out.println("There is NO other robot at location " + newLoc.toString());
//        else
//            System.out.println("Gotta wait! There IS another robot at location " + newLoc.toString());

        return stockCanMove && noFloodingExists && spaceNotOccupied;


    }

    private static Direction getStartDirection()
    {
        if (rc.getType() == RobotType.MINER)
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
        }
        return null;
    }

    private static MapLocation getClosestRefinery()
    {
        if (refLocs.size() > 0)
        {
            refLocs.sort(new Comparator<MapLocation>() {
                public int compare(MapLocation o1, MapLocation o2) {
                    return getRSquared(rc.getLocation(), o2) - getRSquared(rc.getLocation(), o1);
                }
            });
            return refLocs.get(refLocs.size()-1);
        }
        return HQ;
    }

    private static List<MapLocation> getAdjacencies(MapLocation ref)
    {
        List<MapLocation> retAdj = new ArrayList<MapLocation>();
        for (Direction d : directions)
        {
            retAdj.add(new MapLocation(ref.x + d.dx, ref.y + d.dy));
        }
        return retAdj;
    }

    private static void pathfindTo(MapLocation tg) throws GameActionException
    {
        System.out.println("Pathfinding to location: " + tg.toString());
        List<MapLocation> path = new ArrayList<MapLocation>();
        boolean adjReached = false;

        // While target is not reached
        while (true)
        {
            List<MapLocation> adj = getAdjacencies(rc.getLocation());
            //System.out.println("Adjacent squares: " + adj.toString());
            if (adj.contains(tg))
            {
                adjReached = true;
                break;
            }

            if (rc.getType() == RobotType.MINER && (expendedSoupLocs.contains(tg) || (tg.equals(HQ) && !refLocs.contains(HQ))))
                break;


            // Array storing order of offset directions
            int[] offset = {0, 1, -1, 2, -2, 3, -3, 4};
            List<MapLocation> adjHQ = getAdjacencies(HQ);


            // Get direction to target
            Direction dirToMove = getDirection(rc.getLocation(), tg);
            //System.out.println("Initial direction: " + dirToMove);

            // Flag to check if we are waiting for a robot to get out of the way
            boolean waitForRobot = false;
            for (int of = 0; of< offset.length; of++)
            {
                Direction newDir = directions.get((directions.indexOf(dirToMove) + of)%8);
                MapLocation newLoc = new MapLocation(rc.getLocation().x + newDir.dx, rc.getLocation().y + newDir.dy);

                //System.out.println("Trying direction " + newDir.toString() + " at location " + newLoc.toString());
                if (Collections.frequency(path, newLoc) < 2) // If we have been to this new location in the last 15 turns less than twice
                {
                    // If there is a robot in the direction we want to go
                    if (rc.senseRobotAtLocation(newLoc) != null)
                    {
                        double chance = rand.nextDouble();
                        if (chance < 0.7)
                        {
                            // If by weighted chance, we will decide to wait for the robot in the way to move
                            waitForRobot = true;
                            dirToMove = newDir;
                            break;
                        }
                    }
                    else if (canReallyMove(newDir))  // Otherwise, we check if we can move there
                    {
                        dirToMove = newDir;
                        break;
                    }
                }
            }

            // If the waitForRobot flag is false, make the move

            if (!waitForRobot && canReallyMove(dirToMove))
            {
                // Wait for any cooldown
                waitForCooldown();

                // Move in the direction decided
                rc.move(dirToMove);
                // Add location to path
                path.add(rc.getLocation());
                // Trim down path to only the last 15 locations
                if (path.size() > 15)
                    path = path.subList(1, path.size());

            }
            // Yield clock, check transactions
            maybeBuildRefinery();
            maybeBuildDesignSchool();
            checkTransactions();
            Clock.yield();
        }
        System.out.println("Location " + tg.toString() + " found!");
    }

    private static void soupPathfindTo(MapLocation tg, int limit) throws GameActionException
    {
        //System.out.println("Pathfinding to location: " + tg.toString());
        List<MapLocation> path = new ArrayList<MapLocation>();
        int stepcount = 0;
        boolean adjReached = false;

        // While target is not reached and step count is less than the limit
        while (true)
        {
            List<MapLocation> adj = getAdjacencies(rc.getLocation());
            if (adj.contains(tg))
            {
                adjReached = true;
                break;
            }
            else if (stepcount >= limit || soupLocs.size() > 0)
            {
                break;
            }

            // Array storing order of offset directions
            int[] offset = {0, 1, -1, 2, -2, 3, -3, 4};

            // Get direction to target
            Direction dirToMove = getDirection(rc.getLocation(), tg);

            // Flag to check if we are waiting for a robot to get out of the way
            boolean waitForRobot = false;
            for (int of = 0; of< offset.length; of++)
            {
                Direction newDir = directions.get((directions.indexOf(dirToMove) + of)%8);
                MapLocation newLoc = new MapLocation(rc.getLocation().x + newDir.dx, rc.getLocation().y + newDir.dy);

                //System.out.println("Trying direction " + newDir.toString() + " at location " + newLoc.toString());
                if (Collections.frequency(path, newLoc) < 2) // If we have been to this new location in the last 15 turns less than twice
                {
                    // If there is a robot in the direction we want to go
                    if (rc.senseRobotAtLocation(newLoc) != null)
                    {
                        double chance = rand.nextDouble();
                        if (chance < 0.7)
                        {
                            // If by weighted chance, we will decide to wait for the robot in the way to move
                            waitForRobot = true;
                            dirToMove = newDir;
                            break;
                        }
                    }
                    else if (canReallyMove(newDir))  // Otherwise, we check if we can move there
                    {
                        dirToMove = newDir;
                        break;
                    }
                }
            }
            // Scan for soup
            checkForSoup();
            Clock.yield();

            // If the waitForRobot flag is false, we make the move
            if (!waitForRobot && canReallyMove(dirToMove))
            {
                // Wait for any cooldown
                waitForCooldown();

                // Move in the direction decided
                rc.move(dirToMove);
                // Add location to path
                path.add(rc.getLocation());
                // Trim down path to only the last 15 locations
                if (path.size() > 15)
                    path = path.subList(1, path.size());
            }


            // Yield clock, check transactions, maybe build a refinery
            maybeBuildRefinery();
            maybeBuildDesignSchool();
            checkTransactions();
            Clock.yield();
        }

        if (adjReached)
            System.out.println("Location " + tg.toString() + " found!");
        else
            System.out.println("Couldn't find location " + tg.toString() + " within " + Integer.toString(limit) + " steps :(");
    }

    private static void checkTransactions () throws GameActionException
    {
        //System.out.println("Looking for current data...");
        if (rc.getRoundNum() < 2)
            return;

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
            if (message[0] == 117291)
            {
                MapLocation mark = new MapLocation(message[1], message[2]);
                if (!refLocs.contains(mark))
                {
                    System.out.println("Refinery location received! Adding to currently known locations...");
                    refLocs.add(mark);
                }
            }
            if (message[0] == 117292)
            {
                MapLocation mark = new MapLocation(message[1], message[2]);
                if (!expendedSoupLocs.contains(mark))
                {
                    System.out.println("Expended soup location received! Adding to currently known locations...");
                    refLocs.remove(mark);
                    expendedSoupLocs.add(mark);
                }
            }
            if (message[0] == 117293)
            {
                MapLocation mark = new MapLocation(message[1], message[2]);
                if (!designSchoolLocs.contains(mark))
                {
                    System.out.println("Design school location received! Adding to currently known locations...");
                    designSchoolLocs.add(mark);
                }
            }
            if (message[0] == 117294)
            {
                MapLocation mark = new MapLocation(message[1], message[2]);
                if (HQ==null)
                {
                    System.out.println("HQ location updated.");
                    HQ = mark;
                }
            }
            if (message[0] == 117295)
            {
                beginDigging = true;
                if (refLocs.contains(HQ))
                {
                    refLocs.remove(HQ);
                }
                System.out.println("Build the wall!");
            }
        }

        List<int[]> deletedMessages = new ArrayList<int[]>();
        for (int[] msg : outgoingMessages)
        {
            if (rc.canSubmitTransaction(msg, 15))
            {
                rc.submitTransaction(msg, 15);
                deletedMessages.add(msg);
            }
        }
        for (int[] dm : deletedMessages)
            outgoingMessages.remove(dm);


    }

    private static void waitForCooldown() throws GameActionException
    {
        while (rc.getCooldownTurns() != 0)
        {
            //System.out.println("Waiting for cooldown!");
            checkTransactions();
            Clock.yield();
        }
    }

    private static boolean obstaclesAroundMe() throws GameActionException
    {
        List<MapLocation> adjlist = getAdjacencies(rc.getLocation());
        for (MapLocation a : adjlist)
        {
            if (!canReallyMove(getDirection(rc.getLocation(), a)))
            return true;
        }
        return false;
    }

    private static void checkForSoup() throws GameActionException
    {
        MapLocation[] soupSensed = rc.senseNearbySoup();

        for (MapLocation sl : soupSensed)
        {
            System.out.println("Looking for soup at location: " + sl.toString());
            int soupcount;
            if (rc.canSenseLocation(sl))
                soupcount = rc.senseSoup(sl);
            else
                soupcount = 200;

            //System.out.println(Integer.toString(soupcount) + " soup found!");
            int[] message = {117290, sl.x, sl.y, soupcount, -1, -1, -1};
            if (rc.canSubmitTransaction(message, 15))
            {
                rc.submitTransaction(message, 15);
                //System.out.println("Notified other miners of soup!");
            }
            else
            {
                outgoingMessages.add(message);
            }


            if (!soupLocs.contains(sl))
            {
                System.out.println("Adding to currently known locations...");
                soupLocs.add(sl);
                soupCounts.put(sl, soupcount);
            }

        }
    }

    private static void maybeBuildRefinery() throws GameActionException
    {
        MapLocation newRefinery = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
        List<MapLocation> adjHQ = getAdjacencies(HQ);

        boolean isCloseToSoup = false;
        for (MapLocation sl : soupLocs)
        {
            if (getRSquared(sl, newRefinery) < 100)
            {
                isCloseToSoup = true;
                break;
            }

        }

        if (refLocs.size() < 2 && !adjHQ.contains(newRefinery)) //Check if refineries haven't been built yet, and if far away from HQ
        {
            if (rc.canBuildRobot(RobotType.REFINERY, Direction.NORTH))
            {

                rc.buildRobot(RobotType.REFINERY, Direction.NORTH);
                refLocs.add(newRefinery);

                System.out.println("Refinery built!");
                int[] message = {117291, newRefinery.x, newRefinery.y, -1, -1, -1, -1};
                if (rc.canSubmitTransaction(message, 15))
                {
                    rc.submitTransaction(message, 15);
                    System.out.println("Notified other miners of the new refinery!");
                }
                else
                {
                    outgoingMessages.add(message);
                }


                if (!refLocs.contains(newRefinery))
                {
                    System.out.println("Adding to currently known locations...");
                    refLocs.add(newRefinery);
                }

            }
        }
    }

    private static void maybeBuildDesignSchool() throws GameActionException
    {
        MapLocation newDesignSchool = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
        List<MapLocation> adjHQ = getAdjacencies(HQ);

        if (designSchoolLocs.size() < 1 && !adjHQ.contains(newDesignSchool)) //Check if one exists, and is not within HQ proximity
        {
            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, Direction.NORTH))
            {

                rc.buildRobot(RobotType.DESIGN_SCHOOL, Direction.NORTH);
                designSchoolLocs.add(newDesignSchool);

                System.out.println("Design school built!");
                int[] message = {117293, newDesignSchool.x, newDesignSchool.y, -1, -1, -1, -1};
                if (rc.canSubmitTransaction(message, 15))
                {
                    rc.submitTransaction(message, 15);
                    System.out.println("Notified other robots of the new design school!");
                }
                else
                {
                    outgoingMessages.add(message);
                }


                if (!designSchoolLocs.contains(newDesignSchool))
                {
                    System.out.println("Adding to currently known locations...");
                    designSchoolLocs.add(newDesignSchool);
                }

            }
        }
    }





}
