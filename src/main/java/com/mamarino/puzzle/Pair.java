package com.mamarino.puzzle;

public class Pair<T> {
    private T x;
    private T y;

    public Pair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public void setX(T x) {
        this.x = x;
    }

    public T getY() {
        return y;
    }

    public void setY(T y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "" + x + y;
    }

    public boolean equalsTo(T x, T y) {
        return this.x == x && this.y == y;
    }

}
