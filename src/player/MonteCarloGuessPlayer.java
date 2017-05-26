package player;
import java.io.PrintStream;
import java.util.*;
import ship.Ship;
import world.World;
import world.World.ShipLocation;
import world.World.Coordinate;

/**
 * Monte Carlo guess player (task C).
 * Please implement this class.
 *
 * @author Youhan, Jeffrey
 */
public class MonteCarloGuessPlayer  implements Player{//Check if update neighbours works correctly, and fix the duplicate guessing, should not be able to guess same coord, check if recording of enemies ship status is correct, check if our ship status is correct
    //Get if a == 1 statements remade
    World world;
    //Holds MonteCarloPlayer's shipTypes on coordinates
    private int[][] shipBoard;//-1 = previous guesses, 0 = water, 1 = a, 2 = b, 3 = c, 4 = d, 5 = s (a-s represent ships)
    //Holds Current Guesses so far made by MonteCarlo
    private int[][] board;//0 = water, 1 = miss, 2 = hit, 3 = sunkenShip
    //Holds configurations for next guess. Highest number on board will be next guess
	private int[][] configurationsBoard;
    //holds x,y values so that the next guess will be at this coordinate since previous guess was a hit
    ArrayList<Coordinate> hitCoords;
   	//Represent the boards dimensions
    int numRow = -1;
	int numColumn = -1;
    //Represents the amount of ship parts sunk - is compared with amount of shipsParts currently hit
    int sunkCount = 0;
    //holds the direction of what the next shot should be in a straight line, x=1,0 or -1 and y=1,0 or -1 | {0,0} = x,y
    int[] direction = {0,0};
    //Mode activated for one guess, if previous guess was a hit
    boolean inTargetMode = false;
    //shows if direction has already changed, help stop a loop of constantly changing direction in a loop
    boolean reverseAdd = false;
    //Becomes true if the next shot in the current direction is off the board, so is predicted to be a miss, variable used to go to next update() IF statement
    boolean skipPrediction = false;
    //represents the index of hitCoords, is used to be referred back to original coordinate or previously hit coordinate
    int partCheck = 0;
    //Represents coords for next guess, updated by the update()
    int t1 = -1;
    int t2 = -1;
    //Is board hexagonal board?
	boolean isHex = false;
    //holds all ship objects
    ArrayList<Ship> allShips = new ArrayList<>();
    //lengths of each ship(max ship hit counter) [this players ships], this is the lives counter for each ship
    int aircraftCarrier = 5;
    int battleship = 4;
    int submarine = 3;
    int cruiser = 3;
    int destroyer = 2;
    //These represent the Status of each ship owned by opponent, 1 = alive, 0 = sunk
    int a = 1;//a = aircraftCarrier
    int b = 1;//b = battleship
    int c = 1;//c = cruiser
    int d = 1;//d = destroyer
    int s = 1;//s = submarine


	@Override
    public void initialisePlayer(World world) {
    	if(!world.isHex)
	    {
            //Assigning world and board sizes
            this.world = world;
		    board = new int[world.numColumn][world.numRow];
            configurationsBoard = new int[world.numColumn][world.numRow];
            shipBoard = new int[world.numColumn][world.numRow];
            //Assign board dimensions
            if(board.length > 0)
            {
                numRow = board[0].length;
                numColumn = board.length;	
            }
            //initialising the three boards
            for(int i =0; i< numColumn; i++)
            {
                for(int j = 0; j<numRow; j++)
                {
                    board[i][j] = 0;
                    configurationsBoard[i][j] = 0;
                    shipBoard[i][j] = 0;
                }
            }
            //Assigning shipType Numbers to coordinates specified by world.shiplocations
            ArrayList<ShipLocation> ships = world.shipLocations;
            for(ShipLocation ship: ships)
            {
               allShips.add(ship.ship);//adding ship object to arraylist
               int shipTypeNum = getShipNum(ship.ship.name());
               ArrayList<Coordinate> coords = ship.coordinates;
               for(Coordinate coord: coords)//for each ship, adding each coordinate to myShipBoard
               { 
                    shipBoard[coord.column][coord.row] = shipTypeNum;//assignes the shipType a number which is put on the board at said coordinate
               }
            }
            //Finds all the configurations for ALL ships in specified area.
            updateConfigurationsInArea(0,numColumn,0,numRow);
        }
        else
        {
            isHex = true;
        }
    } // end of initialisePlayer()

    @Override
    public Answer getAnswer(Guess guess) {
        //initialising
        Answer answer = new Answer();
        Ship ship = null;
        answer.shipSunk = ship;
        int i = guess.column;
        int j = guess.row;
        //if coordinate = a ship type number (bigger than 0)
        if(shipBoard[i][j] > 0)
        {
            //asigning the type of ship hit via numType
            int shipType = shipBoard[i][j];
            boolean sunk = shipHit(shipType);
            if(sunk)
            {
                 answer.shipSunk = getShip(shipType);
            }
            //Assigning coord as an invalid guess coord for future guesses
            shipBoard[i][j] = -1;
            //Make answer a hit
            answer.isHit = true;
        }
        else
        {
            answer.isHit = false;
        }
        return answer;
    } // end of getAnswer()

    @Override
    public Guess makeGuess() 
    {
        //initialising variables
        Guess guess = null;
        if(inTargetMode)
        {
            guess = new Guess();
            guess.row = t2;
            guess.column = t1;
            return guess;
        }
        //initialising coords for cell and highest number of configs
        int highestNum = 0;
        int x = -1;
        int y = -1;
        //Contest for Getting highest number of configurations against all coordinates
        for(int i = 0; i<numColumn; i++)
        {
            for(int j = 0; j <numRow; j++)
            {
                int configs = configurationsBoard[i][j];//getting num configurations for contending cell
                if(configs == highestNum)//Randomly pick one cell to be next highestNumCell
                {
                    Random rand = new Random();
                    int n = rand.nextInt(2);
                    if(n == 0)//if 0 then config cell
                    {
                        x = i;
                        y = j;
                    }
                }
                else if(configs>highestNum)             
                {
                    //Assigning highest number and coordinates of that number
                    highestNum = configs;
                    x = i;
                    y = j;
                }
            }
        }
        guess = new Guess();
        guess.row = y;
        guess.column = x;
        return guess;
    } // end of makeGuess()


    @Override
    public void update(Guess guess, Answer answer) 
    {
        int i = guess.column;
        int j = guess.row;
        configurationsBoard[i][j] = 0;//Assignes previously guessed coordinate as 'guessed' (0)
        if(answer.shipSunk == null && answer.isHit && inTargetMode)//if ship is not sunk and guess was a hit and was already in targeting mode
        {
            board[i][j] = 2;//assignes board as a hitShip
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords.add(coord);//Adds hit ship's coords to an arrayList         
            int k = coord.column + direction[0];//assignes next coordinate by adding direction onto previous guess
            int l = coord.row + direction[1];
            boolean help = false;//initilising
            if(k<numColumn && k >=0 && l<numRow && l >=0)//checking if guess would end up out of the board's range
            {
                if(board[k][l] == 0)//checking if coordinate on board has not been guessed before
                {
                    t1 = k;
                    t2 = l;   
                }else{help = true;}
            }
            else{help = true;}
            if(help)                 
            {
                //basically skips a predicted guess of that it will be a miss and goes straight to the if statement below \/
                skipPrediction = true;
            }
        }
        if((!answer.isHit && inTargetMode)||skipPrediction)//if miss but still targeting detected ship
        {
            if(!skipPrediction)//if not from if statement above
            {
                board[i][j] = 1;
                updateNeighboursOfCell(i,j);
            }
            skipPrediction = false;//reset
            //reverse direction
            direction[0] = direction[0]*-1;
            direction[1] = direction[1]*-1;
            int k = hitCoords.get(partCheck).column;//assigning coords to the coords of specified hitShip part in hitCoords (checking back on old coordinate)
            int l = hitCoords.get(partCheck).row;
            int c = k + direction[0];//Adding the new reversed direction onto previously known shipPart coord, but now checking for beyond it in other direction
            int r = l + direction[1];
            boolean help = false;//initialise
            if(c < numColumn && r < numRow && c >= 0 && r >= 0)//checking if cell beyond shipPart is within board boundaries
            {             
                //checking if the reverse direction has already been reversed, stops constant reversing loop also checks if guess has not been made before
                if(board[c][r] == 0 && !reverseAdd)
                {
                    reverseAdd = true;
                    t1 = c;//assigning next guess coords
                    t2 = r;   
                }else{help = true;}                
            }else{help = true;}
            if(help)
            {
                reverseAdd = false;
                Coordinate coord = startSearchConfigs(k,l);
                t1 = coord.column;
                t2 = coord.row;
            }                        
        }
        else if(!inTargetMode && answer.isHit)//if first hit
        {
            inTargetMode = true;
            board[i][j] = 2;//assigning guessed shot coords to be a hitShip
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords = new ArrayList<>();//Making new hitList for the hit ship Part coordinates to be hunted down and recorded
            hitCoords.add(coord);//Adding first hit to hitCoords List          
            Coordinate coord2 = startSearchConfigs(i,j); //Start the search around that hit coordinate
            t1 = coord2.column;//Assigning next guess coords
            t2 = coord2.row;
        }
        else if(answer.shipSunk != null && answer.isHit && inTargetMode)//if hit AND it sunk a ship
        {
            int type = getShipNum(answer.shipSunk.name());//getting the ship Type that was hit
            sinkShip(type);//makes shipType status = 0; (sunk)
            board[i][j] = 2;//assignes hit Coords as hit ship Part
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords.add(coord);//Adds hit ship part to List
            sunkCount = sunkCount + answer.shipSunk.len();//Adds total sunk ship parts to sunk count            
            if(sunkCount == hitCoords.size())//Checking if sunk ship parts = hit ship parts
            {
                inTargetMode = false;
                for(int q = 0; q < hitCoords.size(); q++)//makes all coords sunk
                {
                    board[hitCoords.get(q).column][hitCoords.get(q).row] = 3;
                    updateNeighboursOfCell(hitCoords.get(q).column,hitCoords.get(q).row);
                    sunkCount--;
                }
                //updateConfigurationsInArea(0,numColumn,0,numRow);
                hitCoords.clear();
                partCheck = 0; 
            }  
            else //here means that the sunk ship's exact coords are unknown since there is some unsunk ship parts to be sunk still
            {
                Coordinate shipPiece = hitCoords.get(partCheck);//get original hit coords
                Coordinate coord2 = startSearchConfigs(shipPiece.column,shipPiece.row);//starts search again from original hit
                t1 = coord2.column;//assignes next guess coords
                t2 = coord2.row;
            }
            reverseAdd = false;
        }

        if(!inTargetMode && !answer.isHit)//if shot was a miss and there is no lead 
        {
            board[i][j] = 1;//assign guess as a miss on board
            updateNeighboursOfCell(i,j);//update the neighbouring cell configurations, since it was a miss
        }
    } // end of update()


    @Override
    public boolean noRemainingShips() {
        if(aircraftCarrier < 1 && battleship < 1 && cruiser < 1 && destroyer < 1 && submarine < 1)//checking if all lives of all ships = 0
        {
            return true;
        }
        else
        {
            return false;
        }
    } // end of noRemainingShips
    
    public void printBoard(int[][] board)//Used for testing purposes, prints out given board
    {
        int length = board.length;
        int height = board[0].length;
        for(int j = height-1; j >= 0; j--)
        {
            for(int i = 0; i < length; i++)
            {
                System.out.printf("%2d|",board[i][j]);
            }
            System.out.println("");
        }
        System.out.println("\n");
    }

    //Getting highest configuration in neighbouring 4 tiles around coord
    public Coordinate startSearchConfigs(int x, int y)
    {
        Coordinate coord = world.new Coordinate();
        //initialising
        int above = -1;
        int below = -1;
        int left = -1;
        int right = -1; 
        if(x+1 < numColumn && y+1 < numRow && x-1 >= 0 && y-1 >=0)//I think this might not be necarserry
        {
            updateConfigurationsForCell(x,y+1);
            updateConfigurationsForCell(x,y-1);
            updateConfigurationsForCell(x+1,y);
            updateConfigurationsForCell(x-1,y);
        }
        
        if(y+1 < numColumn)//checking if cell above is within bounds
        {
            updateConfigurationsForCell(x,y+1);
            above = configurationsBoard[x][y+1];//gettting configurations for above cell
        }
        if(y-1>=0)//checking if cell below is within bounds
        {
            updateConfigurationsForCell(x,y-1);
            below = configurationsBoard[x][y-1];//getting configurations for below cell
        }
        if(x+1<numColumn)//checking if cell to the left is within bounds
        {
            updateConfigurationsForCell(x+1,y);
            left = configurationsBoard[x+1][y];//gettting configurations for left cell
        }
        if(x-1>=0)//checking if cell to the right is with in bounds
        {
            updateConfigurationsForCell(x-1,y);
            right = configurationsBoard[x-1][y];//gettting configurations for right cell
        }
        int max1 = Math.max(above,below);//getting maximum semi-finals
        int max2 = Math.max(left,right);//getting maximum semi-finals
        int max = Math.max(max1,max2);//gettting maximum grand-finals
        if(above == max)
        {
            direction[0] = 0;//setting direction for North
            direction[1] = 1;
            coord.column = x;//setting coord for above cell
            coord.row = y+1;
        }
        else if(below == max)
        {
            direction[0] = 0;//setting direction for South
            direction[1] = -1;
            coord.column = x;//setting coord for below cell
            coord.row = y-1;
        }
        else if(left == max)
        {
            direction[0] = 1;//setting direction for West
            direction[1] = 0;
            coord.column = x+1;//setting coord for left cell
            coord.row = y;
        }
        else if(right == max)
        {
            direction[0] = -1;//setting direction for East
            direction[1] = 0;
            coord.column = x-1;//setting coord for right cell
            coord.row = y;
        }
        if(max < 1)//checking if the max configurations between all 4 cells is 0
        {
            //if it gets to this stage then all 4 squares around are equal
            ++partCheck;//Because original hit has no more possible leads, the index is increased by 1 to see if the next hit has any leads on potential shipParts
            Coordinate coord2 = hitCoords.get(partCheck); 
            coord = startSearchConfigs(coord2.column,coord2.row);//selfcalls to get coordinate of next previously hit ship part in list
        }
        return coord;
    }
    
   public void sinkShip(int shipNum)//sinks given ship type (makes status = 0)
    {
        switch(shipNum)
        {
            case 1: a = 0;break;
            case 2: b = 0;break;
            case 3: c = 0;break;
            case 4: d = 0;break;
            case 5: s = 0;break;
        }
    }

    //minuses 1 from the ship and checks if it has sunk
    public boolean shipHit(int shipType)
    {
        boolean sunk = false;
        if(shipType == 1){
           aircraftCarrier--;//minus 1 part from aircraftCarrier
           if(aircraftCarrier < 1)//check if it has sunk (0 parts left)
           {
                sunk = true;
           }
        }
        else if(shipType == 2){
            battleship--;//minus 1 part from battleship
            if(battleship < 1)//check if it has sunk (0 parts left)
            {
                sunk = true;
            }
        }
        else if(shipType == 3){
           cruiser--;//minus 1 part from cruiser
           if(cruiser < 1)//check if it has sunk (0 parts left)
           {
               sunk = true;
           }
        }
        else if(shipType == 4){
            destroyer--;//minus 1 part from destroyer
            if(destroyer < 1)//check if it has sunk (0 parts left)
            {
                sunk = true;
            } 
        }
        else if(shipType == 5){
            submarine--;//minus 1 part from submarine
            if(submarine < 1)//check if it has sunk (0 parts left)
            {
                sunk = true;
            }
        }
        return sunk;
    }

    //Returns ship object matching shipType number
    public Ship getShip(int shipType)
    {
        Ship ship = null;
        for(Ship ship2: allShips)
        {
            int shipNum = getShipNum(ship2.name());
            if(shipNum == shipType)
            {
                ship = ship2;
            }
        }
       return ship;
    }


    //returns number matching shipType name
    public int getShipNum(String shipName)
    {
        int shipNum = 0;
        switch(shipName)
        {
            case "AircraftCarrier":   shipNum = 1;break;
            case "Battleship":   shipNum = 2;break;
            case "Cruiser":   shipNum = 3;break;
            case "Destroyer":   shipNum = 4;break;
            case "Submarine":   shipNum = 5;break;
            //default: shipNum = 0;break;
        }
        return shipNum;
    }

    public void updateConfigurationsForCell(int i, int j)//updates configs for given cell for all ALIVE ships only, not sunk ships
    {
        int total = 0;
        if(a == 1)
        {
            total = total + configurations(5,i,j,board);
        }
        if(b == 1)
        {
            total = total + configurations(4,i,j,board);
        }
        if(c == 1)
        {
            total = total +  configurations(3,i,j,board);
        }
        if(d == 1)
        {
            total = total + configurations(2,i,j,board);
        }
        if(s == 1)
        {
            total = total + configurations(3,i,j,board);
        }        
        configurationsBoard[i][j] = total;//returns total possible ships (configurations - configs)
    }

    public void updateNeighboursOfCell(int x, int y)//updates configurations of cells, around given cell coords, with radius of largest alive ship
    {
        if(a == 1)
        {
            updateLineOfNeighboursOfCell(5,x,y);
        }
        else if(b == 1)
        {
            updateLineOfNeighboursOfCell(4,x,y);
        }
        else if(c == 1)
        {
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(s == 1)
        {
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(d == 1)
        {
            updateLineOfNeighboursOfCell(2,x,y);
        }
    }
    
    public void updateLineOfNeighboursOfCell(int spanningLength,int x, int y)//updates a line of cells' configuration, spanning a given length from coordinate
    {
        for(int i = 1; i< spanningLength; i++)
        {
            if(x+i < numColumn)
            {
                updateConfigurationsForCell(x+i,y);
            }
            if(x-i >= 0)
            {                
                updateConfigurationsForCell(x-i,y);
            }
        }
        for(int i = 1; i< spanningLength; i++)
        {
            if(y+i < numRow)
            {
                updateConfigurationsForCell(x,y+i);
            }
            if(y-i >= 0)
            {
                updateConfigurationsForCell(x,y-i);
            }
        }
    }

    //Updates the configurations in left, right, top and bottom board boundaries.
    public void updateConfigurationsInArea(int x1,int x2, int y1, int y2)
    {
        for(int i = x1; i < x2; i++)
        {
            for(int j = y1; j < y2; j++)
            {
               updateConfigurationsForCell(i,j);
            }
        }
    }


	// Gets number of configurations for the length of ship in specified coordinate Over the given board	
	public int configurations(int shipLength, int x, int y,int[][] board)
	{
        int lXBoundary = x-shipLength+1;
        int lYBoundary = y-shipLength+1;
        int hXBoundary = x+shipLength-1;
        int hYBoundary = y+shipLength-1;
		//Returns 0 configurations since coord has already been shot at before
		if(board[x][y] != 0)
		{
			return 0;
		}
		int numConfigs = 0;
        int numXConfigs = 0;
        int numYConfigs = 0;
		//configuring distance from coordinate to all board edges
		if(!isHex)
		{	
            if(lXBoundary < 0)
            {
                lXBoundary = 0;
            }
            if(lYBoundary < 0)
            {
                lYBoundary =0;
            }
            if(hXBoundary > numColumn-1)
            {
                hXBoundary = numColumn-1;
            }
            if(hYBoundary > numRow-1)
            {
                hYBoundary = numRow-1;
            }
			for(int i = lXBoundary; i <= hXBoundary; i++)
			{

                //x-1 is so that the first part of ship length fits into the current square, not joining on the end.
                int endCoord = lXBoundary+x-1+shipLength - i;
                int beginningCoord = lXBoundary + x-i;
                if(endCoord <= hXBoundary && beginningCoord >= lXBoundary)
                {
                    boolean clean = true;
                    //checking for any missedShots or sunken ship parts along the desired horizontal ship configuration
                    for(int k = 0; k<=(endCoord - beginningCoord); k++)
                    {
                        if(board[endCoord-k][y]==1 || board[endCoord-k][y]==3)//checking if there is any misses or sunk ship parts in possible configuration
                        {
                            clean = false;
                            break;
                        }
                    }
                    if(clean)
                    {
                        ++numXConfigs;//adds one possible configuration of ship
                    }
                } 
            }//endFor
            for(int j = lYBoundary; j <= hYBoundary; j++)
            {
                int topCoord = lYBoundary+y-1+shipLength - j;
                int bottomCoord = lYBoundary+y - j;
                if(topCoord <= hYBoundary && bottomCoord >=lYBoundary)
                {
                    boolean clean = true;
                    //Checking for any missedShots or sunken ship parts along the desired vertical ship configuration
                    for(int k = 0; k<=(topCoord - bottomCoord); k++)
                    {
                        if(board[x][topCoord-k]==1 || board[x][topCoord-k]==3)
                        {	
                            clean = false;	
                            k = topCoord-bottomCoord;//break
                        }	
                    }
                    if(clean)
                    {
                        ++numYConfigs;
                    }
                }           
            }//endFor
		}//endIf
		else
		{
			//HEX BOARD NOT IMPLEMENTED
		}
        numConfigs = numXConfigs + numYConfigs;
		return numConfigs;
	} //endConfigurations()
} // end of class MonteCarloGuessPlayer
