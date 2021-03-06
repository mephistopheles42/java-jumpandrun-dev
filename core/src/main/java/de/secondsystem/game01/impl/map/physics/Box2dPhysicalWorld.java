package de.secondsystem.game01.impl.map.physics;

import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.DistanceJoint;
import org.jbox2d.dynamics.joints.DistanceJointDef;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.map.IGameMap.WorldId;

public final class Box2dPhysicalWorld implements IPhysicsWorld {
	
	private static final int velocityIterations = 4;
	private static final int positionIterations = 2;

	World physicsWorld;
	
	@Override
	public void init(Vector2f gravity) {
		physicsWorld = new World(new Vec2(gravity.x, gravity.y));
		physicsWorld.setSleepingAllowed(true);
		physicsWorld.setContactListener(new Box2dContactListener());
		physicsWorld.setContinuousPhysics(false);
	}

	private long accFt;
	private static final long STEP_SIZE = 8;
	private static final int MIN_STEPS = 1;
	private static final int MAX_STEPS = 6;
	@Override
	public void update(long frameTime) {
		accFt+=frameTime;
		int steps = (int) (accFt/STEP_SIZE);
		if(steps<-1)
			return;
		
		int stepsClamped = Math.min(Math.max(steps, MIN_STEPS), MAX_STEPS);
		accFt-=stepsClamped*STEP_SIZE;
		
		for(;stepsClamped>0; --stepsClamped)
			physicsWorld.step(STEP_SIZE/1000.f, velocityIterations, positionIterations);
		
//		float dt = frameTime/1000.f;
//		float max = 1.f/60;
//		while (dt>=max) {
//			physicsWorld.step(max/2, velocityIterations, positionIterations);
//			dt -= max/2;
//		}
//		physicsWorld.step(dt, velocityIterations, positionIterations);
	}

	private static final class RaycastCbClosure {
		IPhysicsBody foundBody;
		float qdist = Float.MAX_VALUE;
		
		void update(Vector2f start, IPhysicsBody b) {
			float d = (start.x-b.getPosition().x)*(start.x-b.getPosition().x) + (start.y-b.getPosition().y)*(start.y-b.getPosition().y);
			if( d<=qdist ) {
				qdist = d;
				foundBody = b;
			}
		}
	}
	
	@Override
	public IPhysicsBody raycastSolid(Vector2f start, Vector2f target) {
		return raycast(start, target, new RaycastFilter() {
			@Override public boolean accept(IPhysicsBody body) {
				return body.getCollisionHandlerType()==CollisionHandlerType.SOLID;
			}
		});
	}
	@Override
	public IPhysicsBody raycast(final Vector2f start, Vector2f target, final RaycastFilter filter) {
		final RaycastCbClosure c = new RaycastCbClosure();
		final RayCastCallback callback = new RayCastCallback() {
			@Override public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal,
					float fraction) {
				if( fixture.isSensor() || !(fixture.m_body.getUserData() instanceof Box2dPhysicsBody) )
					return -1;
				
				IPhysicsBody b = (IPhysicsBody) fixture.m_body.getUserData();
				
				if( filter.accept(b) ) {
					c.update(start, b);
				}
				return -1;
			}
		};
		
		physicsWorld.raycast(callback, Box2dPhysicsBody.toBox2dCS(start.x, start.y), Box2dPhysicsBody.toBox2dCS(target.x, target.y));
		
		return c.foundBody;
	}
	
	@Override
	public void destroyBody(IPhysicsBody body) {
		body.onDestroy();
	}

	Body createBody(BodyDef def) {
		return physicsWorld.createBody(def);
	}

	public RevoluteJoint createRevoluteJoint(Body body1, Body body2, Vec2 anchor) {
		return createRevoluteJoint(body1, body2, anchor, null);
	}
	public RevoluteJoint createRevoluteJoint(Body body1, Body body2, Vec2 anchor, Float maxMotorTorque) {
		RevoluteJointDef jointDef = new RevoluteJointDef();
		jointDef.initialize(body1, body2, anchor);
		
		if( maxMotorTorque!=null ) {
			jointDef.maxMotorTorque = maxMotorTorque;
			jointDef.enableMotor = true;
			jointDef.motorSpeed = 1f;
		}
		
		return (RevoluteJoint) physicsWorld.createJoint(jointDef);
	}

	public Joint createDistanceJoint(Body body1, Body body2, float distance) {
		DistanceJointDef jointDef = new DistanceJointDef();
		jointDef.initialize(body1, body2, body1.getPosition(), body2.getPosition());
		jointDef.length = distance;
		jointDef.frequencyHz = 4;
		jointDef.dampingRatio = 0.1f;
		jointDef.collideConnected = true;
		
		return (DistanceJoint) physicsWorld.createJoint(jointDef);
	}

	public void destroyJoint(Joint joint) {
		physicsWorld.destroyJoint(joint);
	}

	@Override
	public PhysicsBodyFactory factory() {
		return new Box2dPhysicsBodyFactory();
	}


	private class Box2dPhysicsBodyFactory implements PhysicsBodyFactory {
		int worldMask;
		float x;
		float y;
		float rotation;
		float width;
		float height;
		float density = 1.f;
		Float fixedWeight;
		float friction = 0.5f;
		float restitution = 0.f;
		boolean interactive = false;
		boolean liftable = false;
		boolean kinematic = false;
		boolean flying = false;
		boolean sensor = false;
		CollisionHandlerType type = CollisionHandlerType.SOLID;
		PhysicsBodyShape shape = null;
		
		@Override public PhysicsBodyFactory inWorld(WorldId worldId) {
			worldMask |= worldId.id;
			return this;
		}

		@Override
		public PhysicsBodyFactory worldMask(int worldMask) {
			this.worldMask = worldMask;
			return this;
		}
	
		@Override public PhysicsBodyFactory position(float x, float y) {
			this.x = x;
			this.y = y;
			return this;
		}
	
		@Override public PhysicsBodyFactory rotation(float rotation) {
			this.rotation = rotation;
			return this;
		}
	
		@Override public PhysicsBodyFactory dimension(float width, float height) {
			this.width = width;
			this.height = height;
			return this;
		}

		@Override public PhysicsBodyFactory weight(float weight) {
			fixedWeight = weight;
			return this;
		}

		@Override public PhysicsBodyFactory density(float weightPerPx) {
			density = weightPerPx;
			return this;
		}

		@Override public PhysicsBodyFactory friction(float friction) {
			this.friction = friction;
			return this;
		}

		@Override public PhysicsBodyFactory restitution(float restitution) {
			this.restitution = restitution;
			return this;
		}
	
		@Override public PhysicsBodyFactory type(CollisionHandlerType type) {
			this.type = type;
			return this;
		}

		@Override public PhysicsBodyFactory interactive(boolean interactive) {
			this.interactive = interactive;
			return this;
		}

		@Override public PhysicsBodyFactory liftable(boolean liftable) {
			this.liftable = liftable;
			return this;
		}

		@Override
		public PhysicsBodyFactory flying(boolean flying) {
			this.flying = flying;
			return this;
		}
		
		@Override
		public PhysicsBodyFactory sensor(boolean sensor) {
			this.sensor = sensor;
			return this;
		}
	
		@Override public StaticPhysicsBodyFactory staticBody(PhysicsBodyShape shape) {
			this.shape = shape;
			return new Box2dStaticPhysicsBodyFactory();
		}
	
		@Override public DynamicPhysicsBodyFactory dynamicBody(PhysicsBodyShape shape) {
			this.shape = shape;
			return new Box2dDynamicPhysicsBodyFactory();
		}
		@Override
		public DynamicPhysicsBodyFactory kinematicBody(PhysicsBodyShape shape) {
			this.kinematic = true;
			return dynamicBody(shape);
		}
	
		@Override public HumanoidPhysicsBodyFactory humanoidBody() {
			return new Box2dHumanoidPhysicsBodyFactory();
		}

		class Box2dStaticPhysicsBodyFactory implements StaticPhysicsBodyFactory {
			@Override public IPhysicsBody create() {
				Box2dPhysicsBody b = new Box2dPhysicsBody(Box2dPhysicalWorld.this, worldMask, width, height, interactive, liftable, type, kinematic, flying);
				b.initBody(x, y, rotation, shape, sensor, friction, restitution, density, fixedWeight);
				return b;
			}
		}
		
		class Box2dDynamicPhysicsBodyFactory extends Box2dStaticPhysicsBodyFactory implements DynamicPhysicsBodyFactory {
			boolean stableCheck = false;
			boolean worldSwitchAllowed = false;
			float maxXSpeed = Float.MAX_VALUE;
			float maxYSpeed = Float.MAX_VALUE;
			Float initialRelXSpeed;
			Float initialRelYSpeed;
			
			@Override public IDynamicPhysicsBody create() {
				Box2dDynamicPhysicsBody b = new Box2dDynamicPhysicsBody(Box2dPhysicalWorld.this, worldMask, width, height, interactive, liftable, 
						type, kinematic, flying, stableCheck, worldSwitchAllowed, maxXSpeed, maxYSpeed);
				b.initBody(x, y, rotation, shape, sensor, friction, restitution, density, fixedWeight);
				
				if( initialRelXSpeed!=null && initialRelYSpeed!=null )
					b.move(initialRelXSpeed, initialRelYSpeed);
				
				return b;
			}
	
			@Override
			public DynamicPhysicsBodyFactory initialRelativeSpeed(
					float x, float y) {
				  float sr = (float) Math.sin(Math.toRadians(rotation));
				  float cr = (float) Math.cos(Math.toRadians(rotation));
				  
				  initialRelXSpeed = x * cr - y * sr;
				  initialRelYSpeed = x * sr - y * cr;
				  
				return this;
			}
			
			@Override public DynamicPhysicsBodyFactory stableCheck(boolean enable) {
				stableCheck = enable;
				return this;
			}
	
			@Override public DynamicPhysicsBodyFactory worldSwitch(boolean allowed) {
				worldSwitchAllowed = allowed;
				return this;
			}
	
			@Override public DynamicPhysicsBodyFactory maxXSpeed(float speed) {
				maxXSpeed = speed;
				return this;
			}
	
			@Override public DynamicPhysicsBodyFactory maxYSpeed(float speed) {
				maxYSpeed = speed;
				return this;
			}

		}
		
		class Box2dHumanoidPhysicsBodyFactory extends Box2dDynamicPhysicsBodyFactory implements HumanoidPhysicsBodyFactory {
			float maxSlope = 45;
			float maxReach = 10;
			float maxThrowSpeed = 100;
			float maxLiftWeight = Float.MAX_VALUE;
			float maxLiftForce = 2;
			
			@Override public HumanoidPhysicsBodyFactory maxXSpeed(float speed) {
				return (HumanoidPhysicsBodyFactory) super.maxXSpeed(speed);
			}
	
			@Override public HumanoidPhysicsBodyFactory maxYSpeed(float speed) {
				return (HumanoidPhysicsBodyFactory) super.maxYSpeed(speed);
			}
	
			@Override public HumanoidPhysicsBodyFactory maxSlope(float degree) {
				maxSlope = degree;
				return this;
			}
	
			@Override public HumanoidPhysicsBodyFactory maxReach(float px) {
				maxReach = px;
				return this;
			}

			@Override public HumanoidPhysicsBodyFactory maxThrowSpeed(float speed) {
				maxThrowSpeed = speed;
				return this;
			}

			@Override public HumanoidPhysicsBodyFactory maxLiftWeight(float weight) {
				maxLiftWeight = weight;
				return this;
			}
			@Override public HumanoidPhysicsBodyFactory maxLiftForce(float force) {
				maxLiftForce = force;
				return this;
			}
	
			@Override public IHumanoidPhysicsBody create() {
				Box2dHumanoidPhysicsBody b = new Box2dHumanoidPhysicsBody(Box2dPhysicalWorld.this, worldMask, width, height, interactive, liftable, type, flying, maxXSpeed, maxYSpeed, 
						maxThrowSpeed, maxLiftWeight, maxLiftForce, maxSlope, maxReach);
				b.initBody(x, y, rotation, null, sensor, friction, restitution, density, fixedWeight);
				return b;
			}
		}

	}
}
