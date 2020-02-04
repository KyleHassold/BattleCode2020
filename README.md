# Battlecode 2020
This is the GitHub Repository for the work done by the team BetaZero in Battlecode 2020
Battlecode 2020 Match 141 Game 1 BetaZero (Blue) vs BrutalPigeons64 (Red): https://2020.battlecode.org/visualizer.html?https://2020.battlecode.org/replays/97f9e4b659ef32e10dddc4bcd0c50a.bc20

### Final Code

- `src/droneRush`
    Final version of the code. Submitted to the Qualifying tournament for Battlecode 2020
    
#### How it Works

Within each package, a 'RobotPlayer.java' file exists and is called at the beginning of a unit's life.
The RobotPlayer file determines the type of robot it is and creates and instance of the corrisponding type to then call the .run() method on.
Every robot shares the Robot super class and every moveable robot shares the Unit super class and the Building super class for the non-moveable robots.
The Robot super class forces the implementation of the .run() method which is called to run each robot with the code corrisponding to their type.

#### Strategy

The strategy is to have 6 miners built by the HQ to begin mining for soup.

Once all the miners are built, the first miner (designated as the builder) places a design school and fulfillment center near the HQ but towards the center in a hard coded translation.
If this can not be done, the builder finds a spot away from the HQ to build another Design School which will build a landscaper to terraform the enviroment around the HQ.

The builder, after building the Design School and Fulfillment center will search out soup deposits to put a refinery next to, since the HQ will no longer be accessable to work as a refinery
The miners then switch to using the refinery as their base.

The Design School then builds 8 Landscapers that move into position surrounding the HQ and then putting dirt under them and adjacent to them on the wall to build up an even wall around the HQ.

Once every landscaper is in place, the Fulfillment center builds drones as much as it can.
Each drone searches the map for the enemy HQ and attacks and units it can to disrupt the enemy

With the enemy's resources disrupted and our landscapers building a wall around the HQ, we hope to outlast the enemy
