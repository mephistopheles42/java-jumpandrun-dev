package de.secondsystem.game01.impl.graphic;

import org.jsfml.graphics.Color;
import org.jsfml.graphics.PrimitiveType;
import org.jsfml.graphics.Vertex;
import org.jsfml.graphics.VertexArray;
import org.jsfml.system.Vector2f;

public class Light {

	private static final int SUBDIVISIONS = 32;
	
	private Vector2f center;
	
	private Color color;
	
	private float radius, degree, centerDegree;
	
	private VertexArray vertices;
	
	private boolean dirty;
	
	public Light(Vector2f center, Color color, float radius, float degree, float centerDegree) {
		this.center = center;
		this.color = color;
		this.radius = radius;
		this.degree = (float) Math.toRadians(degree);
		this.centerDegree = (float) Math.toRadians(centerDegree);
		this.dirty = true;
	}

	VertexArray getDrawable() {
		return dirty ? vertices=createDrawable(center, color, radius, degree, centerDegree) : vertices;
	}


	private static VertexArray createDrawable(Vector2f center, Color color, float radius,
			float degree, float centerDegree) {
		VertexArray v = new VertexArray(PrimitiveType.TRIANGLE_FAN);
		v.ensureCapacity(SUBDIVISIONS+1);
		
		v.add(new Vertex(center, color));
		
		final float startDegree = (float) (centerDegree-degree/2 -Math.PI/2);
		final float stepSize = (float) ((Math.PI*2)/SUBDIVISIONS);
		final Color borderColor = new Color(0, 0, 0, 0);
		
		for (float angle=0; angle<=Math.min(Math.PI*2, degree)+stepSize; angle+=stepSize ) {
			v.add(new Vertex(
					new Vector2f(
							radius*(float)Math.cos(angle+startDegree) + center.x,
                    		radius*(float)Math.sin(angle+startDegree) + center.y),
                    		borderColor ));
		}
		
		return v;
	}

	
	public void setCenter(Vector2f center) {
		this.center = center;
		this.dirty = true;
	}
	public void setColor(Color color) {
		this.color = color;
		this.dirty = true;
	}
	public void setRadius(float radius) {
		this.radius = radius;
		this.dirty = true;
	}
	public void setDegree(float degree) {
		this.degree = (float) Math.toRadians(degree);
		this.dirty = true;
	}
	public void setCenterDegree(float centerDegree) {
		this.centerDegree = (float) Math.toRadians(centerDegree);
		this.dirty = true;
	}

	public Vector2f getCenter() {
		return center;
	}
	public Color getColor() {
		return color;
	}
	public float getRadius() {
		return radius;
	}
	public float getDegree() {
		return degree;
	}
	public float getCenterDegree() {
		return centerDegree;
	}
}