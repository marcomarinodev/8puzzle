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

public class EightController extends JLabel implements PropertyChangeListener {

  private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

  private VetoableChangeListener listener;
  private List<List<Integer>> board;
  private List<EightTile> tiles;

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

  @SuppressWarnings("unchecked")
  public void setTiles(List<EightTile> tiles) {

    this.tiles = tiles;

    // controller is listening to tiles bounded properties changes
    tiles.forEach(tile -> tile.addPropertyChangeListener(this));

    // controller is listening to tiles constrained properties changes
    listener = new VetoableChangeListener() {
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

        // x, y
        Pair<Integer> tileToHolePosition = getMatrixCoordinates(tileChangedLabel.getX());
        Pair<Integer> holePosition = getNearHolePosition(tileChangedLabel.getX());

        if (holePosition == null) {
          setText("KO");

          setTileLabel(new Pair<>(tileChangedLabel.getX(), -1));

          throw new PropertyVetoException("Cannot move this tile", evt);
        }

        System.out.println("Veto passed! :)");
        setText("OK");

        // hole to clicked tile
        Pair<Integer> tileChangePair = new Pair<>(getBoardPosition(holePosition), tileOldLabel);
        setTileLabel(tileChangePair);

        // updating the board
        updateBoard(tileToHolePosition, holePosition);

        // check win
        if (checkVictory()) setText("YOU WON!");
      }
    };
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (changes != null)
      this.changes.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.changes.removePropertyChangeListener(listener);
  }

  private void initController(List<Integer> permutation) {
    board = generateBoard(permutation);

    for (EightTile tile : tiles) {
      // remove vetoable change listener if any
      tile.removeVetoableChangeListener(listener);
      // sets this controller as listener of proposed new tile position (veto)
      tile.addVetoableChangeListener(listener);

      // remove any property change listener related to the current tile
      removePropertyChangeListener(tile);
      // tile listens to 'changes' property change
      addPropertyChangeListener(tile);
    }
  }

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

  private void updateBoard(Pair<Integer> currentTilePosition, Pair<Integer> holeTilePosition) {
    int tileRow = currentTilePosition.getX();
    int tileCol = currentTilePosition.getY();
    int holeRow = holeTilePosition.getX();
    int holeCol = holeTilePosition.getY();

    int temp = board.get(tileRow).get(tileCol);
    board.get(tileRow).set(tileCol, Constants.HOLE);
    board.get(holeRow).set(holeCol, temp);
  }

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

  private boolean checkFlipMove() {
    // make sure that the tile in position 1 and 2 are not holes
    // and hole is in position 9
    return !Objects.equals(board.get(0).get(0), Constants.HOLE)
        && !Objects.equals(board.get(0).get(1), Constants.HOLE)
        && Objects.equals(board.get(2).get(2), Constants.HOLE);
  }

  private boolean isLegalCoordinate(Pair<Integer> point) {
    return point.getX() >= 0 && point.getX() < Constants.MAX_DIM &&
        point.getY() >= 0 && point.getY() < Constants.MAX_DIM;
  }

  private Pair<Integer> getMatrixCoordinates(Integer position) {
    return new Pair<>((int) Math.floor((double) (position - 1) / Constants.MAX_DIM), (position - 1) % 3);
  }

  private Integer getBoardPosition(Pair<Integer> coordinates) {
    String key = coordinates.toString();
    return this.coordinates.get(key);
  }

  public void setTileLabel(Pair<Integer> newClickedTileValue) {
    Pair<Integer> oldClickedTileValue = clickedTileValue;
    clickedTileValue = newClickedTileValue;

    changes.firePropertyChange(
        Constants.SET_LABEL_EVT,
        oldClickedTileValue,
        newClickedTileValue);
  }

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

      case Constants.COMPLETED_SET_LABEL_EVT:

        String oldValueStr = evt.getOldValue().toString();
        String newValueStr = evt.getNewValue().toString();

        System.out.println("completed set label from value " + oldValueStr + " to " + newValueStr);
      default:
        break;
    }
  }

  public boolean checkVictory() {
    List<List<Integer>> targetList = Arrays.asList(
        Arrays.asList(1, 2, 3),
        Arrays.asList(4, 5, 6),
        Arrays.asList(7, 8, 9));

    return board.equals(targetList);
  }

}