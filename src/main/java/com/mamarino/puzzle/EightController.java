package com.mamarino.puzzle;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JLabel;

public class EightController extends JLabel implements PropertyChangeListener, VetoableChangeListener {

  private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
  private List<List<Integer>> board;

  // structure to for mapping coordinates (00, ..., 22) to linear values (1, ..., 9)
  private final Map<String, Integer> coordinates = Stream.of(new Object[][] {
      { "00", 1 },
      { "01", 2 },
      { "02", 3 },
      { "10", 4 },
      { "11", 5 },
      { "12", 6 },
      { "20", 7 },
      { "21", 8 },
      { "22", 9 },
  }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

  private Pair<Integer> clickedTileValue = new Pair<>(0, 0);
  
  public EightController() {}

  /**
   * it checks whether the move made is legal or not, it's being triggered whenever a tile is pressed
   * then the controller has to check the move update the board accordingly
   * @param evt a {@code PropertyChangeEvent} object describing the
   *                event source and the property that has changed.
   * @throws PropertyVetoException launched when the movement is not allowed
   */
  @SuppressWarnings("unchecked")
  @Override
  public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
    // position, label
    Pair<Integer> tileChangedLabel = (Pair<Integer>) evt.getNewValue();
    Integer tileOldLabel = ((Pair<Integer>) evt.getOldValue()).getY();

    if (!Objects.equals(tileChangedLabel.getY(), Constants.HOLE)) {
      // just change property without throwing an error
      // as it is a consequence of an already approved move
      return;
    }

    // pair: (x, y)
    Pair<Integer> tileToHolePosition = getMatrixCoordinates(tileChangedLabel.getX());
    Pair<Integer> holePosition = getNearHolePosition(tileChangedLabel.getX());

    // if a hole is not found then it means we cannot move that tile
    if (holePosition == null) {
      setText("KO");

      // -1 means this tile is temporary on error state
      // a new event is sent to the corresponding tile
      setTileLabel(new Pair<>(tileChangedLabel.getX(), -1));

      throw new PropertyVetoException("Cannot move this tile", evt);
    }

    System.out.println("Veto passed! :)");
    setText("OK");

    // clicked tile becomes the new hole
    Pair<Integer> tileChangePair = new Pair<>(getBoardPosition(holePosition), tileOldLabel);
    setTileLabel(tileChangePair);

    // updating the board
    updateBoard(tileToHolePosition, holePosition);

    // check win
    if (checkVictory()) setText("YOU WON!");
  }

  /**
   * controller initialization phase where the board is filled with a permutation
   * @param permutation input permutation
   */
  private void initController(List<Integer> permutation) {
    board = generateBoard(permutation);
  }

  /**
   * it takes an integer permutation and transform it into a 3x3 matrix
   * @param permutation
   * @return 3x3 matrix representing the board state
   */
  private List<List<Integer>> generateBoard(List<Integer> permutation) {
    List<List<Integer>> newBoard = new ArrayList<>();
    List<Integer> row = new ArrayList<>();

    for (int i = 0; i < permutation.size(); i++) {
      row.add(permutation.get(i));

      if ((i + 1) % 3 == 0) {
        newBoard.add(row);
        row = new ArrayList<>();
      }
    }

    return newBoard;
  }

  /**
   * it updates the board by swapping the tile to be moved and the "hole" tile
   * @param currentTilePosition coordinates of the tile to be moved
   * @param holeTilePosition coordinates of the "hole" tile
   */
  private void updateBoard(Pair<Integer> currentTilePosition, Pair<Integer> holeTilePosition) {
    int tileRow = currentTilePosition.getX();
    int tileCol = currentTilePosition.getY();
    int holeRow = holeTilePosition.getX();
    int holeCol = holeTilePosition.getY();

    int temp = board.get(tileRow).get(tileCol);
    board.get(tileRow).set(tileCol, Constants.HOLE);
    board.get(holeRow).set(holeCol, temp);
  }

  /**
   * it returns the nearest hole tile by looking at top, left, right and bottom of the
   * position passed by parameter
   * @param position integer value representing the position (f.i, 4 -> (1,0))
   * @return coordinates of the nearest hole tile starting from 'position'; if no hole is found, it returns null
   */
  private Pair<Integer> getNearHolePosition(Integer position) {
    Pair<Integer> mCoordinates = getMatrixCoordinates(position);
    List<Pair<Integer>> possibleMoves = new ArrayList<>();
    Integer i = mCoordinates.getX();
    Integer j = mCoordinates.getY();

    possibleMoves.add(new Pair<>(i - 1, j));
    possibleMoves.add(new Pair<>(i + 1, j));
    possibleMoves.add(new Pair<>(i, j - 1));
    possibleMoves.add(new Pair<>(i, j + 1));

    Optional<Pair<Integer>> nearHole = possibleMoves
        .stream()
        .filter((move) -> isLegalCoordinate(move) &&
                Objects.equals(board.get(move.getX()).get(move.getY()), Constants.HOLE))
        .findFirst();

      return nearHole.orElse(null);

  }

  /**
   * it checks if the flip move is possible
   * @return boolean: true -> flip move is possible; not otherwise
   */
  private boolean checkFlipMove() {
    // make sure that the tile in position 1 and 2 are not holes
    // and hole is in position 9
    return !Objects.equals(board.get(0).get(0), Constants.HOLE)
        && !Objects.equals(board.get(0).get(1), Constants.HOLE)
        && Objects.equals(board.get(2).get(2), Constants.HOLE);
  }

  /**
   * ti checks that the passing coordinate 'point' is a coordinate within the board bounds
   * @param point coordinate to be checked
   * @return boolean: true -> the point is within the board; not otherwise
   */
  private boolean isLegalCoordinate(Pair<Integer> point) {
    return point.getX() >= 0 && point.getX() < Constants.MAX_DIM &&
        point.getY() >= 0 && point.getY() < Constants.MAX_DIM;
  }

  /**
   * retrieve coordinates from position (f.i, position=6 -> returns (1,2))
   * @param position integer position (1,...,9)
   * @return coordinates as (Integer, Integer)
   */
  private Pair<Integer> getMatrixCoordinates(Integer position) {
    // we are assuming as a pre-condition that position is a number from 1 to 9
    // therefore the findFirst() returns always an Optional value that is always present
    String foundKey = coordinates.entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getValue(), position))
            .map(Map.Entry::getKey)
            .findFirst().get();

    return getCoordinatesFromString(foundKey);
  }

  /**
   * it sets the tile label by firing a SET_LABEL_EVT. The tile catching the event
   * must check whether it is the tile to be changed or not by checking the tile value
   * passed by the sending event
   * @param newClickedTileValue pair (position, value) (f.i, (3, 8) means that tile in position 3 has label '8')
   */
  public void setTileLabel(Pair<Integer> newClickedTileValue) {
    Pair<Integer> oldClickedTileValue = clickedTileValue;
    clickedTileValue = newClickedTileValue;

    changes.firePropertyChange(
        Constants.SET_LABEL_EVT,
        oldClickedTileValue,
        newClickedTileValue);
  }

  /**
   * controller is registered to events fired by the EightBoard class: RESTART_EVT and FLIP_EVT
   * are being caught to handle restart and flip movements respectively
   * @param evt A PropertyChangeEvent object describing the event source
   *          and the property that has changed.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    switch (evt.getPropertyName()) {
      case Constants.RESTART_EVT:
        List<Integer> permutation = (List<Integer>) evt.getNewValue();

        initController(permutation);

        if (!checkVictory()) setText("START");
        else setText("YOU WON!");
        break;

      case Constants.FLIP_EVT:
        if (checkFlipMove()) {
          // switch both position 1 and 2
          Integer labelIn1 = board.get(0).get(0);
          Integer labelIn2 = board.get(0).get(1);

          Pair<Integer> tile1 = new Pair<>(1, labelIn2);
          Pair<Integer> tile2 = new Pair<>(2, labelIn1);

          board.get(0).set(0, labelIn2);
          board.get(0).set(1, labelIn1);

          changes.firePropertyChange(
              Constants.SET_LABEL_EVT,
              new Pair<>(-1, -1), // dummy old value
              tile1);

          changes.firePropertyChange(
                  Constants.SET_LABEL_EVT,
              new Pair<>(-1, -1), // dummy old value
              tile2);
        }
        break;
      default:
        break;
    }
  }

  public boolean checkVictory() {
    List<List<Integer>> targetBoard = generateBoard(List.of(1,2,3,4,5,6,7,8,9));
    return board.equals(targetBoard);
  }

  private Pair<Integer> getCoordinatesFromString(String coordinatesStr) {
    if (coordinatesStr.length() != 2) {
      throw new IllegalArgumentException("Input string must have exactly two characters.");
    }

    int row = Character.getNumericValue(coordinatesStr.charAt(0));
    int col = Character.getNumericValue(coordinatesStr.charAt(1));

    return new Pair<>(row, col);
  }

  private Integer getBoardPosition(Pair<Integer> coordinates) {
    String key = coordinates.toString();
    return this.coordinates.get(key);
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (changes != null)
      this.changes.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.changes.removePropertyChangeListener(listener);
  }

}