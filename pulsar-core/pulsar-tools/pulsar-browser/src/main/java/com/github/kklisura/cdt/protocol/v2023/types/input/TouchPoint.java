package com.github.kklisura.cdt.protocol.v2023.types.input;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

public class TouchPoint {

  private Double x;

  private Double y;

  @Optional
  private Double radiusX;

  @Optional private Double radiusY;

  @Optional private Double rotationAngle;

  @Optional private Double force;

  @Experimental
  @Optional private Double tangentialPressure;

  @Experimental @Optional private Integer tiltX;

  @Experimental @Optional private Integer tiltY;

  @Experimental @Optional private Integer twist;

  @Optional private Double id;

  /** X coordinate of the event relative to the main frame's viewport in CSS pixels. */
  public Double getX() {
    return x;
  }

  /** X coordinate of the event relative to the main frame's viewport in CSS pixels. */
  public void setX(Double x) {
    this.x = x;
  }

  /**
   * Y coordinate of the event relative to the main frame's viewport in CSS pixels. 0 refers to the
   * top of the viewport and Y increases as it proceeds towards the bottom of the viewport.
   */
  public Double getY() {
    return y;
  }

  /**
   * Y coordinate of the event relative to the main frame's viewport in CSS pixels. 0 refers to the
   * top of the viewport and Y increases as it proceeds towards the bottom of the viewport.
   */
  public void setY(Double y) {
    this.y = y;
  }

  /** X radius of the touch area (default: 1.0). */
  public Double getRadiusX() {
    return radiusX;
  }

  /** X radius of the touch area (default: 1.0). */
  public void setRadiusX(Double radiusX) {
    this.radiusX = radiusX;
  }

  /** Y radius of the touch area (default: 1.0). */
  public Double getRadiusY() {
    return radiusY;
  }

  /** Y radius of the touch area (default: 1.0). */
  public void setRadiusY(Double radiusY) {
    this.radiusY = radiusY;
  }

  /** Rotation angle (default: 0.0). */
  public Double getRotationAngle() {
    return rotationAngle;
  }

  /** Rotation angle (default: 0.0). */
  public void setRotationAngle(Double rotationAngle) {
    this.rotationAngle = rotationAngle;
  }

  /** Force (default: 1.0). */
  public Double getForce() {
    return force;
  }

  /** Force (default: 1.0). */
  public void setForce(Double force) {
    this.force = force;
  }

  /** The normalized tangential pressure, which has a range of [-1,1] (default: 0). */
  public Double getTangentialPressure() {
    return tangentialPressure;
  }

  /** The normalized tangential pressure, which has a range of [-1,1] (default: 0). */
  public void setTangentialPressure(Double tangentialPressure) {
    this.tangentialPressure = tangentialPressure;
  }

  /**
   * The plane angle between the Y-Z plane and the plane containing both the stylus axis and the Y
   * axis, in degrees of the range [-90,90], a positive tiltX is to the right (default: 0)
   */
  public Integer getTiltX() {
    return tiltX;
  }

  /**
   * The plane angle between the Y-Z plane and the plane containing both the stylus axis and the Y
   * axis, in degrees of the range [-90,90], a positive tiltX is to the right (default: 0)
   */
  public void setTiltX(Integer tiltX) {
    this.tiltX = tiltX;
  }

  /**
   * The plane angle between the X-Z plane and the plane containing both the stylus axis and the X
   * axis, in degrees of the range [-90,90], a positive tiltY is towards the user (default: 0).
   */
  public Integer getTiltY() {
    return tiltY;
  }

  /**
   * The plane angle between the X-Z plane and the plane containing both the stylus axis and the X
   * axis, in degrees of the range [-90,90], a positive tiltY is towards the user (default: 0).
   */
  public void setTiltY(Integer tiltY) {
    this.tiltY = tiltY;
  }

  /**
   * The clockwise rotation of a pen stylus around its own major axis, in degrees in the range
   * [0,359] (default: 0).
   */
  public Integer getTwist() {
    return twist;
  }

  /**
   * The clockwise rotation of a pen stylus around its own major axis, in degrees in the range
   * [0,359] (default: 0).
   */
  public void setTwist(Integer twist) {
    this.twist = twist;
  }

  /** Identifier used to track touch sources between events, must be unique within an event. */
  public Double getId() {
    return id;
  }

  /** Identifier used to track touch sources between events, must be unique within an event. */
  public void setId(Double id) {
    this.id = id;
  }
}
