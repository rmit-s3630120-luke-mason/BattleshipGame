package player;

import java.util.*;
import java.util.concurrent.*;
import world.World;

/**
 * Greedy guess player (task B).
 * Please implement this class.
 *
 * @author Youhan, Jeffrey
 */
public class GreedyGuessPlayer  implements Player
{

    public enum STATE { HUNT, TARGET };
    public enum DIRECTION { NORTH, EAST, SOUTH, WEST };

    private STATE mode;
    private DIRECTION direction;

    private World world;
   
    private int numRows;
    private int numCols;
    private int numShips;
    
    private List<World.Coordinate> hits; // list of hits recieved from opponent
    private List<Guess> availableGuesses; // list of guesses that haven't already been made
    private int directions; // number of directions that have been attacked around a target (0-4)
    private World.Coordinate target; // current target for target mode
    private List<Guess> history; // guess history
    private BlockingQueue<World.Coordinate> targets; // Each hit is queued here until all its surrounding squares have been attacked

    @Override
    public void initialisePlayer(World world) 
    {
        this.world = world;
        this.mode = STATE.HUNT; 
        this.direction = DIRECTION.NORTH;
        this.numRows = world.numRow;
        this.numCols = world.numColumn;
        this.numShips = world.shipLocations.size();    
        this.hits = new ArrayList<World.Coordinate>();
        this.availableGuesses = new ArrayList<Guess>();
        this.directions = 4;
        this.target = world.new Coordinate();
        this.history = new ArrayList<Guess>();
        this.targets = new ArrayBlockingQueue<World.Coordinate>(this.numRows * this.numCols);

        // setup availableGuesses with every second square
        for (int i = 0; i < this.numRows; i++) 
        {
            for (int j = 0; j < this.numCols; j+=2)
            {
                Guess possibleGuess = new Guess();
                
                if (i%2 == 0 && j+1 <= this.numCols)
                    possibleGuess.column = j+1;
                else if (i%2 != 0)
                    possibleGuess.column = j;
                else continue; 

                possibleGuess.row = i;
                availableGuesses.add(possibleGuess);
            }
        }
    
    } 

    // check if coordinates given in guess match coordinates of any of our ships
    // If so, record hit in hits List and check if corresponding ship is now dead
    @Override
    public Answer getAnswer(Guess guess) 
    {
        Answer answer = new Answer();
        
        int x = guess.row;
        int y = guess.column;

        List<World.ShipLocation> shipLocations = world.shipLocations;

        for (int i = 0; i < shipLocations.size(); i++)
        {
            List<World.Coordinate> coordinates = shipLocations.get(i).coordinates;

            for (int j = 0; j < coordinates.size(); j++)
            {
                World.Coordinate current = coordinates.get(j);

                if (x == current.row && y == current.column)
                {
                    World.Coordinate hit = world.new Coordinate();
                    hit.row = x;
                    hit.column = y;
                    this.hits.add(hit);

                    answer.isHit = true;

                    if (dead(coordinates))
                    {
                        answer.shipSunk = shipLocations.get(i).ship;
                        this.numShips--; 
                    }
                }
            }
        }

        return answer;
    } 


    // decide whether to guess for target mode or hunting mode
    // return guess
    @Override
    public Guess makeGuess() 
    {
        Guess guess = new Guess();
       
        switch (this.mode)
        {
            case HUNT: guess = hunt();       
                break;
            case TARGET: guess = target();
                break;
        }

        this.history.add(guess);

        return guess;
    } 


    // obtain a guess for hunting mode
    // return a random guess from the availableGuesses array;
    public Guess hunt()
    {
        Random random = new Random();

        int choices = this.availableGuesses.size();

        int choice = random.nextInt(choices);

        Guess guess = this.availableGuesses.get(choice);

        this.availableGuesses.remove(choice);

        return guess;
    }

    // obtain a guess for target mode
    // return a guess attacking the square to the north, south, east or west of target
    public Guess target()
    {
        if (this.directions == 0) // if all direction have been attacked
        {
            this.directions = 4; // reset directions
            this.target = targets.poll(); // try next target

            if (this.target == null) // if there are no more targets
            {
                this.mode = STATE.HUNT; // return to hunt mode
                return hunt();
            }
        }

        Guess guess = new Guess();

        guess.row = target.row;
        guess.column = target.column;

        rotate(); // next direction

        this.directions--; // indicate another direction has been tried

        switch (this.direction) // adjust target based on direction
        {
            case NORTH: guess.column++;
                break;
            case SOUTH: guess.column--;
                break;
            case EAST: guess.row++;
                break;
            case WEST: guess.row--;
                break;
        }
        
        if (!check(guess)) // if guess is invalid, try next direction
            return target(); 
        
        remove(guess); // remove guess from availableGuesses
        
        return guess;
    }

    // Remove a guess from availableGuesses once it has been made
    public void remove(Guess guess)
    {
        for (int i = 0; i < this.availableGuesses.size(); i++)
        {
            Guess option = this.availableGuesses.get(i);

            if (option.row == guess.row && option.column == guess.column)
            {
                this.availableGuesses.remove(option);
                return;
            }
        }
    }

    // validate a guess
    // 1. Check guess is within bounds of map (only for target guesses)
    // 2. Check guess hasn't already been made 
    public boolean check(Guess guess)
    {
        if (guess.row >= this.numRows || guess.row < 0 || guess.column >= this.numCols || guess.column < 0)
            return false;
        
        for (int i = 0; i < this.history.size(); i++)
        {   
            Guess old = this.history.get(i);

            if (old.row == guess.row && old.column == guess.column)
                return false;
        }

        return true;
    }


    // update state of program
    // Enter target mode if a hit is made and
    // add location of hit to targets queue
    @Override
    public void update(Guess guess, Answer answer) 
    {
        boolean hit = answer.isHit;

        if (hit)
        {
            World.Coordinate location = world.new Coordinate();
            location.row = guess.row;
            location.column = guess.column;
            this.targets.add(location);

            if (this.mode == STATE.HUNT)
                this.target = this.targets.poll();

            this.mode = STATE.TARGET;
        }

    } // end of update()

    // rotate targeting direction
    public void rotate()
    {
        if (this.direction == DIRECTION.NORTH)
            this.direction = DIRECTION.EAST;
        else if (this.direction == DIRECTION.SOUTH)
            this.direction = DIRECTION.WEST;
        else if (this.direction == DIRECTION.WEST)
            this.direction = DIRECTION.NORTH;
        else if (this.direction == DIRECTION.EAST)
            this.direction = DIRECTION.SOUTH;
    }

    @Override
    public boolean noRemainingShips() 
    {
        if (this.numShips == 0)
            return true;
        return false;
    } 

    public boolean dead(List<World.Coordinate> coordinates)
    {
        // compare ship coordinates with list of hits recieved
        // If every one of the ships coordinates has a hit recorded, return true

        for (int i = 0; i < coordinates.size(); i++)
        {
            boolean match = false;
            
            World.Coordinate ship = coordinates.get(i);

            for (int j = 0; j < this.hits.size(); j++)
            {
                World.Coordinate hit = this.hits.get(j);

                if (hit.row == ship.row && hit.column == ship.column)
                {
                    match = true;
                    break;
                }
            }

            if (!match)
                return false;
        }
 
        return true;
    }

} // end of class GreedyGuessPlayer
