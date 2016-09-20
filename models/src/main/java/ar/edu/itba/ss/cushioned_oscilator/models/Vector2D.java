package ar.edu.itba.ss.cushioned_oscilator.models;

public class Vector2D {
  private double x;
  private double y;

  public Vector2D(final double x, final double y) {
    this.x = x;
    this.y = y;
  }

  public Vector2D add(final Vector2D vector) {
    this.x += vector.x();
    this.y += vector.y();
    return this;
  }
  public Vector2D sub(final Vector2D vector) {
    this.x -= vector.x();
    this.y -= vector.y();
    return this;
  }

  public Vector2D times(final double scalar) {
    this.x *= scalar;
    this.y *= scalar;
    return this;
  }

  public Vector2D div(final double scalar) {
    this.x /= scalar;
    this.y /= scalar;

    return this;
  }

  public double x() {
    return x;
  }

  public void setX(final int x) {
    this.x = x;
  }

  public double y() {
    return y;
  }

  public void setY(final int y) {
    this.y = y;
  }

  @Override
  public String toString() {
    return "Vector2D{" +
            "x=" + x +
            ", y=" + y +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Vector2D vector2D = (Vector2D) o;

    if (Double.compare(vector2D.x, x) != 0) return false;
    return Double.compare(vector2D.y, y) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
