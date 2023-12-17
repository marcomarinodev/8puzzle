package com.mamarino.puzzle;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.List;

import javax.swing.JButton;

public class EightTile extends JButton implements PropertyChangeListener {

  private final VetoableChangeSupport vChangeSupport = new VetoableChangeSupport(this);
  private Integer position;
  private Integer label;

  public EightTile() {
  }

  public EightTile(Integer position, Integer label) {
    this.label = label;
    this.position = position;
    updateAppearence();
  }

  public void onClick() {
    if (vChangeSupport == null)
      return;

    System.out.println("tile on click event vChangeSupport is not null");

    // * send an event to the controller
    // * so that it can make the move check for us
    Pair<Integer> newValue = new Pair<Integer>(position, label);
    try {
      vChangeSupport.fireVetoableChange(
          "tilePositionProperty",
          this.getValue(),
          newValue);
    } catch (PropertyVetoException pve) {
      System.out.println("VetoExc: " + pve.getMessage());
    }
  }

  public void setLabel(Integer label) {
    if (label > 0) {
      this.label = label;
      updateAppearence();
      return;
    }

    showError();
  }


  public void setPosition(Integer position) {
    this.position = position;
  }

  private void updateAppearence() {

    if (label == Constants.HOLE) {
      System.out.println("setting " + position + " as HOLE");
      setBackground(Color.GRAY);
      setText("");
      return;
    }

    setText(String.valueOf(label));

    if (label == position) {
      setBackground(Color.GREEN);
      return;
    }

    setBackground(Color.YELLOW);
  }

  public void showError() {
    System.out.println("Showing error");
    setBackground(Color.RED);
    new java.util.Timer().schedule(
        new java.util.TimerTask() {
          @Override
          public void run() {
            updateAppearence();
          }
        },
        Constants.ERR_DURATION);
  }

  public Integer getPosition() {
    return position;
  }

  public Integer getInternalLabel() {
    return label;
  }

  public Pair<Integer> getValue() {
    return new Pair<Integer>(position, label);
  }

  @Override
  public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
    vChangeSupport.addVetoableChangeListener(listener);
  }

  @Override
  public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
    vChangeSupport.removeVetoableChangeListener(listener);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    switch (evt.getPropertyName()) {
      case Constants.RESTART_EVT:
        List<Integer> permutation = (List<Integer>) evt.getNewValue();
        Integer newLabel = permutation.get(position - 1);
        setLabel(newLabel);

        break;

      case Constants.CLICK_TILE_EVT:
        Pair<Integer> clickedTileValue = (Pair<Integer>) evt.getNewValue();

        // (x,y) x = position; y = label
        int eventTilePosition = clickedTileValue.getX();
        int eventTileLabel = clickedTileValue.getY();

        if (position == eventTilePosition)
          setLabel(eventTileLabel);

        break;
      default:
        break;
    }
  }

}