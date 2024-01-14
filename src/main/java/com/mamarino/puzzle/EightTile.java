package com.mamarino.puzzle;

import java.awt.Color;
import java.beans.*;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;

public class EightTile extends JButton implements PropertyChangeListener {

  private Integer position;
  private Integer label;

  private final VetoableChangeSupport vChangeSupport = new VetoableChangeSupport(this);

  public EightTile() {
  }

  public EightTile(Integer position, Integer label) {
    this.label = label;
    this.position = position;
    updateAppearance();
  }

  /**
   * it requests to controller to be the new "hole"
   */
  public void onClick() {
    if (vChangeSupport == null)
      return;

    setLabel(Constants.HOLE);
  }

  /**
   * it changes its new value based on EightController's approval
   * @param newLabel the label the tile wants to represent
   */
  public void setLabel(Integer newLabel) {
    Pair<Integer> oldValue = this.getValue();

    if (newLabel <= 0) {
      showError();
      return;
    }

    try {
      Pair<Integer> newValue = new Pair<>(this.position, newLabel);
      vChangeSupport.fireVetoableChange(
              "tileLabelProperty",
              oldValue,
              newValue
      );

      this.label = newLabel;
      updateAppearance();
    } catch (PropertyVetoException e) {
      showError();
      System.out.println("VetoExc: " + e.getMessage());
    }
  }


  public void setPosition(Integer position) {
    this.position = position;
  }

  private void updateAppearance() {
    if (Objects.equals(label, Constants.HOLE)) {
      System.out.println("setting " + position + " as HOLE");
      setBackground(Color.GRAY);
      setText("");
      return;
    }

    setText(String.valueOf(label));

    if (Objects.equals(label, position)) {
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
            updateAppearance();
          }
        },
        Constants.ERR_DURATION);
  }

  public Pair<Integer> getValue() {
    return new Pair<>(position, label);
  }

  @Override
  public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
    vChangeSupport.addVetoableChangeListener(listener);
  }

  @Override
  public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
    vChangeSupport.removeVetoableChangeListener(listener);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    switch (evt.getPropertyName()) {
      case Constants.RESTART_EVT:
        List<Integer> permutation = (List<Integer>) evt.getNewValue();
          this.label = permutation.get(position - 1);
        updateAppearance();

        break;

      case Constants.SET_LABEL_EVT:
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