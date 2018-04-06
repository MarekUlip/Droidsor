package com.example.marekulip.droidsor.sensorlogmanager;

/**
 * Auto generated wrapper class for the data with 3 dimensions.
 * */
public class Point3D {
  public double x, y, z;

  public Point3D(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Returns object of this class with all point set to 0.0
   * @return
   */
  public static Point3D getDefaultPoint3D(){
    return new Point3D(0.0,0.0,0.0);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(z);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Point3D other = (Point3D) obj;
    return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x) && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y) && Double.doubleToLongBits(z) == Double.doubleToLongBits(other.z);
  }
}
