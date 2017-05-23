package player;

import java.util.*;
import world.World;

/**
 * Random guess player (task A).
 * Please implement this class.
 *
 * @author Youhan, Jeffrey
 */
public class RandomGuessPlayer implements Player{

    private World world;

    private int numRows;
    private int numCols;
    private int numShips;

    private List<World.Coordinate> hits; // list of hits recieved from opponent

    private List<Guess> availableGuesses; // list of guesses that haven't already been made

    @Override
    public void initialisePlayer(World world) 
    {
        this.world = world;

        this.numRows = world.numRow;
        this.numCols = world.numColumn;

        this.numShips = world.shipLocations.size();    

        this.hits = new ArrayList<World.Coordinate>();

        this.availableGuesses = new ArrayList<Guess>();
        
        // Initialise availableGuesses with every set of coordinates
        for (int i = 0; i < this.numRows; i++) 
        {
            for (int j = 0; j < this.numCols; j++)
            {
                Guess possibleGuess = new Guess();
                possibleGuess.row = i;
                possibleGuess.column = j;
                availableGuesses.add(possibleGuess);
            }
        }
    } 

    @Override
    public Answer getAnswer(Guess guess) 
    {
        // check if coordinates given in guess match coordinates of any of our ships
        // If so, record hit in hits List and check if corresponding ship is now dead

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

    @Override
    public Guess makeGuess() 
    {
        // Get a random number within size of availableGuesses list
        // select that coordinate set and remove from available guesses list

        Random random = new Random();

        int choices = this.availableGuesses.size();

        int choice = random.nextInt(choices);

        Guess guess = this.availableGuesses.get(choice);

        this.availableGuesses.remove(choice);

        return guess;

    } 


    @Override
    public void update(Guess guess, Answer answer) 
    {
       // NOT NEEDED FOR RANDOM PLAYER 
    } 


    @Override
    public boolean noRemainingShips() 
    {
        if (this.numShips == 0)
            return true;
        return false;
    } 
} 

