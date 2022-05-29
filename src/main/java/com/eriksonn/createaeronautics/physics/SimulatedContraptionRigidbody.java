package com.eriksonn.createaeronautics.physics;


import com.eriksonn.createaeronautics.blocks.CustomFloatingBlockProvider;
import com.eriksonn.createaeronautics.contraptions.AirshipContraption;
import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingTileEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.dimension.AirshipDimensionManager;
import com.eriksonn.createaeronautics.index.CAConfig;
import com.eriksonn.createaeronautics.mixins.ControlledContraptionEntityMixin;
import com.eriksonn.createaeronautics.physics.api.ContraptionEntityPhysicsAdapter;
import com.eriksonn.createaeronautics.physics.api.IFloatingBlockProvider;
import com.eriksonn.createaeronautics.physics.api.IThrustProvider;
import com.eriksonn.createaeronautics.physics.api.PhysicsAdapter;
import com.eriksonn.createaeronautics.physics.collision.detection.Contact;
import com.eriksonn.createaeronautics.physics.collision.detection.ICollisionDetector;
import com.eriksonn.createaeronautics.physics.collision.detection.impl.GJKCollisionDetector;
import com.eriksonn.createaeronautics.physics.collision.detection.impl.SphereAABBCollisionDetector;
import com.eriksonn.createaeronautics.physics.collision.detection.impl.SphereCollisionDetector;
import com.eriksonn.createaeronautics.physics.collision.resolution.IIterativeManifoldSolver;
import com.eriksonn.createaeronautics.physics.collision.resolution.SequentialManifoldSolver;
import com.eriksonn.createaeronautics.physics.collision.shape.MeshCollisionShapeGenerator;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.components.fan.EncasedFanTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.SailBlock;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.fluids.FluidAttributes;

import java.util.*;
import java.util.function.Supplier;

import static com.eriksonn.createaeronautics.physics.PhysicsUtils.deltaTime;
import static com.eriksonn.createaeronautics.physics.PhysicsUtils.getAirPressure;


public class SimulatedContraptionRigidbody extends AbstractContraptionRigidbody {
//    AirshipContraptionEntity entity;
public AirshipContraption contraption;
    public Quaternion orientation;
    public Vector3d momentum=Vector3d.ZERO;

    Vector3d centerOfMass=Vector3d.ZERO;
    double[][] inertiaTensor=new double[3][3];
    double mass;

    public Vector3d angularMomentum=Vector3d.ZERO;
    Vector3d angularVelocity=Vector3d.ZERO;
    public Quaternion principalRotation;
    public double[] principalInertia =new double[3];
    Vector3d localForce =Vector3d.ZERO;
    Vector3d globalForce =Vector3d.ZERO;
    Vector3d localTorque =Vector3d.ZERO;
    Vector3d globalTorque =Vector3d.ZERO;
    public Vector3d globalVelocity=Vector3d.ZERO;
    Vector3d localVelocity=Vector3d.ZERO;
    double totalAccumulatedBuoyancy =0.0;

    //BuoyancyController levititeBuoyancyController=new BuoyancyController(6.0);
    Map<BlockPos,IFloatingBlockProvider> floatingBlocks;
    Map<BlockPos,IFloatingBlockProvider> altitudeLockingFloatingBlocks;
    public static final Map<Block, Supplier<IFloatingBlockProvider>> simpleCustomFloatingBlock = new HashMap<>();
    boolean isInitialized=false;

    Vector3f CurrentAxis=new Vector3f(1,1,1);
    float CurrentAxisAngle = 0;
    public Map<UUID, SubcontraptionRigidbody> subcontraptionRigidbodyMap;

    Vector3d inertiaTensorI;
    Vector3d inertiaTensorJ;
    Vector3d inertiaTensorK;

    Vector3d inverseInertiaTensorI;
    Vector3d inverseInertiaTensorJ;
    Vector3d inverseInertiaTensorK;

    // manifold solver
    IIterativeManifoldSolver manifoldSolver = new SequentialManifoldSolver();

    public SimulatedContraptionRigidbody(AirshipContraption contraption, PhysicsAdapter adapter)
    {
        simpleCustomFloatingBlock.put(Blocks.END_STONE, () -> new CustomFloatingBlockProvider(1.5,0.5,true));

        orientation=Quaternion.ONE.copy();
        //orientation=new Quaternion(0,1,0,1);
        //orientation.normalize();

        this.contraption=contraption;
        this.adapter=adapter;
        momentum=Vector3d.ZERO;

        principalRotation =Quaternion.ONE.copy();

        //principialRotation=new Quaternion(1,2,3,4);
        //principialRotation.normalize();
        PhysicsUtils.generateLeviCivitaTensor();
        subcontraptionRigidbodyMap=new HashMap<>();
    }
    public void tryInit()
    {
        if(!isInitialized) {
            generateMassDependentParameters(contraption,Vector3d.ZERO);
            mergeMassFromSubContraptions();

            updateLevititeBuoyancyPositions();
            initCollision();
            updateRotation();
            isInitialized=true;
        }
    }


    public void tick()
    {

        if(doGravity)
         addGlobalForce(new Vector3d(0, -PhysicsUtils.gravity * getMass(), 0), Vector3d.ZERO);

        if(contraption==null)
            return;

//        centerOfMass=Vector3d.ZERO;
        generateMassDependentParameters(contraption,Vector3d.ZERO);
        mergeMassFromSubContraptions();

        tryInit();


        //orientation=new Quaternion(0,0,0.3827f,0.9239f);
        //orientation.normalize();





        //updateInertia();
        updateTileEntityInteractions();
        updateLevititeForces();
        //centerOfMass=Vector3d.ZERO;
      
        totalAccumulatedBuoyancy =0;

        //totalAccumulatedBuoyancy += levititeBuoyancyController.apply(orientation,adapter.position());
        updateWings();
        updateRotation();



        //globalForce=globalForce.add(0,-totalAccumulatedBuoyancy,0);

        //globalForce=globalForce.add(0,-PhysicsUtils.gravity*mass,0);

        momentum = momentum.add(MathUtils.rotateQuat(localForce.scale(deltaTime),orientation)).add(globalForce.scale(deltaTime));
        globalForce = Vector3d.ZERO;
        localForce = Vector3d.ZERO;
        localTorque =Vector3d.ZERO;
        globalTorque =Vector3d.ZERO;
//        if(entity.position().y<75)
//        {
//            entity.move(0,75-entity.position().y,0);
//            if(momentum.y<0)
//            {
//                momentum=momentum.multiply(1,-0.5,1);
//            }
//        }

        momentum=momentum.scale(0.995);
        globalVelocity=momentum.scale(1.0/mass);
        localVelocity = MathUtils.rotateQuatReverse(globalVelocity,orientation);

        float c = (float)Math.cos(CurrentAxisAngle);
        float s = (float)Math.sin(CurrentAxisAngle);
        CurrentAxis=new Vector3f(c,3,s);
        CurrentAxis=new Vector3f(0,1,0);
        CurrentAxis.normalize();


        CurrentAxisAngle+=0.01f;
        angularMomentum=angularMomentum.scale(0.995);

        if(doCollisions) {
            List<Contact> contacts = findContacts();

            manifoldSolver.preSolve(contacts);
            for (int i = 0; i < CAConfig.MAX_COLLISION_ITERATIONS.get(); i++) {
                manifoldSolver.solve(this, contacts, deltaTime);
            }
        }

        if(doFluidBuoyancy) {
            // fluid bouyancy
            fluidBuoyancy();
        }

        endPhysicsTick();

    }

    public boolean doFluidBuoyancy = true;
    public boolean doCollisions = true;
    public boolean doGravity = true;

    public void fluidBuoyancy() {
        // for every block
        for (Map.Entry<BlockPos, Template.BlockInfo> entry : contraption.getBlocks().entrySet()) {
            BlockPos airshipPos = entry.getKey();
            Template.BlockInfo info = entry.getValue();
            BlockState state = info.state;

            Vector3d globalPos = toGlobal(new Vector3d(airshipPos.getX(), airshipPos.getY(), airshipPos.getZ()).add(0.5, 0.5, 0.5));

            // for all blocks within 1 block of this
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = new BlockPos(globalPos.x + x, globalPos.y + y, globalPos.z + z);
                        FluidState fluidState = adapter.getFluidState(pos);

                        if(fluidState != null && !fluidState.isEmpty()) {
                            // test if we are overlapping this block
                            AxisAlignedBB aabb1 = new AxisAlignedBB(globalPos.x - 0.5, globalPos.y - 0.5, globalPos.z - 0.5, globalPos.x + 0.5, globalPos.y + 0.5, globalPos.z + 0.5);
                            AxisAlignedBB aabb2 = new AxisAlignedBB(pos);

                            if (aabb1.intersects(aabb2)) {
                                AxisAlignedBB intersect = aabb1.intersect(aabb2);
                                Vector3d point = toLocal(intersect.getCenter());
                                double volume = intersect.getSize();

                                FluidAttributes attributes = fluidState.getType().getAttributes();
                                double viscosity = attributes.getViscosity() / 1000.0;
                                double density = attributes.getDensity() / 1000.0;

                                double buoyancy = volume * density;

                                addGlobalForce(new Vector3d(0, 1.25, 0).scale(buoyancy), point);

                                // water resistance & inertia
                                Vector3d scale = getVelocityAtPoint(point).scale(-viscosity * 0.3);
                                addGlobalForce(scale, point);

                            }
                        }

                    }
                }
            }
        }
    }

    public void endPhysicsTick() {
        Vector3d v = angularVelocity.scale(deltaTime*0.5f);
        Quaternion q = new Quaternion((float)v.x,(float)v.y,(float)v.z, 1.0f);
        q.mul(orientation);
        orientation=q;
        orientation.normalize();




//        entity.quat=orientation.copy();
//        entity.velocity=globalVelocity.scale(deltaTime);
//        entity.setDeltaMovement(globalVelocity.scale(deltaTime));
//        entity.move(globalVelocity.x* deltaTime,globalVelocity.y* deltaTime,globalVelocity.z* deltaTime);
    }

    public void readAdditional(CompoundNBT compound, boolean spawnPacket) {

        orientation = readQuaternion(compound.getCompound("Orientation"));
        momentum = readVector(compound.getCompound("Momentum"));
        angularMomentum = readVector(compound.getCompound("AngularMomentum"));
    }

    public void writeAdditional(CompoundNBT compound, boolean spawnPacket) {
        compound.put("Orientation",writeQuaternion(orientation));
        compound.put("Momentum",writeVector(momentum));
        compound.put("AngularMomentum",writeVector(angularMomentum));
    }
    CompoundNBT writeQuaternion(Quaternion Q)
    {
        CompoundNBT compound=new CompoundNBT();
        compound.putFloat("R",Q.r());
        compound.putFloat("I",Q.i());
        compound.putFloat("J",Q.j());
        compound.putFloat("K",Q.k());
        return compound;
    }
    Quaternion readQuaternion(CompoundNBT compound)
    {
        float r = compound.getFloat("R");
        float i = compound.getFloat("I");
        float j = compound.getFloat("J");
        float k = compound.getFloat("K");
        return new Quaternion(i,j,k,r);
    }
    CompoundNBT writeVector(Vector3d V)
    {
        CompoundNBT compound=new CompoundNBT();
        compound.putDouble("X",V.x);
        compound.putDouble("Y",V.y);
        compound.putDouble("Z",V.z);
        return compound;
    }
    Vector3d readVector(CompoundNBT compound)
    {
        double x = compound.getDouble("X");
        double y = compound.getDouble("Y");
        double z = compound.getDouble("Z");
        return new Vector3d(x,y,z);
    }
    public void addSubContraption(UUID uuid,AbstractContraptionEntity newEntity)
    {
        SubcontraptionRigidbody rigidbody = new SubcontraptionRigidbody(newEntity,this);
        subcontraptionRigidbodyMap.put(uuid, rigidbody);
        rigidbody.generateMassDependentParameters(newEntity.getContraption(),Vector3d.ZERO);
        generateCollisionShapes(rigidbody);
    }
    public void removeSubContraption(UUID uuid)
    {
        subcontraptionRigidbodyMap.remove(uuid);
    }
    public Quaternion getPartialOrientation(float partialTick)
    {
        Vector3d v = angularVelocity.scale(partialTick* deltaTime*0.5f);
        Quaternion q = new Quaternion((float)v.x,(float)v.y,(float)v.z, 1.0f);
        q.mul(orientation);
        q.normalize();
        return q;
    }

    void updateLevititeBuoyancyPositions()
    {
        floatingBlocks = new HashMap<>();
        altitudeLockingFloatingBlocks = new HashMap<>();
        for (Map.Entry<BlockPos, Template.BlockInfo> entry : contraption.getBlocks().entrySet())
        {
            Block block = entry.getValue().state.getBlock();

            if(simpleCustomFloatingBlock.containsKey(block))
                addFloatingProvider(simpleCustomFloatingBlock.get(block).get(),entry.getKey());

            if(block instanceof IFloatingBlockProvider)
                addFloatingProvider((IFloatingBlockProvider)block,entry.getKey());
        }
        for (Map.Entry<BlockPos, TileEntity> entry : contraption.presentTileEntities.entrySet()) {
            if(entry.getValue() instanceof IFloatingBlockProvider)
                addFloatingProvider((IFloatingBlockProvider)entry.getValue(),entry.getKey());
        }
    }
    void addFloatingProvider(IFloatingBlockProvider provider, BlockPos pos)
    {
        if(provider.isAltitudeLocking())
        {
            altitudeLockingFloatingBlocks.put(pos,provider);
        }else
            floatingBlocks.put(pos,provider);
    }
    void updateLevititeForces()
    {
        if(altitudeLockingFloatingBlocks.size()==0)
            return;

        //all variables in here are in global reference frame
        Tuple<Vector3d,Vector3d> T = getForceAndPositionFromFloatingBlocks(altitudeLockingFloatingBlocks);
        Vector3d maxForce = T.getA();
        Vector3d relativePosition = T.getB();
        Vector3d addedTourqe = maxForce.cross(relativePosition);
        Vector3d addedAngularAcceleration = rotate(multiplyInertiaInverse(rotateInverse(addedTourqe)));

        Vector3d totalAcceleration = relativePosition.cross(addedAngularAcceleration);
        totalAcceleration=totalAcceleration.add(maxForce.scale(1/mass));

        Vector3d expectedAcceleration = new Vector3d(0,1,0).scale(PhysicsUtils.gravity);

        double scaleFactor = expectedAcceleration.lengthSqr()/expectedAcceleration.dot(totalAcceleration);

        double smoothing = 0.1;//lower value is tighter
        if(scaleFactor>1)
            scaleFactor = Math.log((scaleFactor-1)/smoothing+1)*smoothing+1;

        addFloatingBlockForce(altitudeLockingFloatingBlocks,maxForce,relativePosition,scaleFactor);

        //T = getForceAndPositionFromFloatingBlocks(floatingBlocks);
        //maxForce = T.getA();
        //relativePosition = T.getB();
        //addFloatingBlockForce(floatingBlocks,maxForce,relativePosition,1.0);

    }
    void addFloatingBlockForce(Map<BlockPos, IFloatingBlockProvider> floatingBlocks,Vector3d maxForce,Vector3d relativePosition ,double liftScale)
    {
        addGlobalForce(maxForce.scale(liftScale),relativePosition);
        for (Map.Entry<BlockPos, IFloatingBlockProvider> entry : floatingBlocks.entrySet()) {

            IFloatingBlockProvider floatingProvider = entry.getValue();
            Vector3d pos = getLocalCoordinate(entry.getKey());
            double airPressure = PhysicsUtils.getAirPressure(toGlobal(pos));
            Vector3d localVelocity = getVelocityAtPoint(pos);

            double magnitude = -floatingProvider.getVerticalFriction()*localVelocity.y*airPressure;
            Vector3d reactionForce = new Vector3d(0,magnitude,0);
            addGlobalForce(reactionForce,rotate(pos));

            double relativeGravityReaction = liftScale*airPressure*floatingProvider.getRelativeStrength();
            if(adapter instanceof ContraptionEntityPhysicsAdapter)
            {
               AirshipContraptionEntity entity = ((ContraptionEntityPhysicsAdapter)adapter).contraption;
               if(entity.level.isClientSide && entity.fakeClientWorld != null) {
                   floatingProvider.tickOnForceApplication(
                        entity.fakeClientWorld,
                        entry.getKey().offset(0,AirshipManager.getPlotPosFromId(entity.plotId).getY(),0),
                        rotateInverse(new Vector3d(0, relativeGravityReaction, 0)),
                        rotateInverse(reactionForce));
                }
            }
        }
    }
    Tuple<Vector3d,Vector3d> getForceAndPositionFromFloatingBlocks(Map<BlockPos,IFloatingBlockProvider> floatingBlocks)
    {
        Vector3d totalPos = Vector3d.ZERO;
        double totalForce = 0;
        for (Map.Entry<BlockPos, IFloatingBlockProvider> entry : floatingBlocks.entrySet()) {
            Vector3d pos = getLocalCoordinate(entry.getKey());
            double force = entry.getValue().getRelativeStrength()*getAirPressure(toGlobal(pos))*PhysicsUtils.gravity;
            totalPos=totalPos.add(rotate(pos).scale(force));
            totalForce+=force;
        }
        totalPos = totalPos.scale(1/totalForce);
        return new Tuple<>(new Vector3d(0,totalForce,0),totalPos);
    }

    Vector3d previousCenterOfMass = null;

    void mergeMassFromSubContraptions()
    {
        mass=localMass;
        centerOfMass=localCenterOfMass.scale(mass);
        for(Map.Entry<UUID,SubcontraptionRigidbody> entry : subcontraptionRigidbodyMap.entrySet()) {
            SubcontraptionRigidbody rigidbody = entry.getValue();
            Vector3d entityOffsetPosition = rigidbody.entity.position().subtract(getPlotOffset());
            Vector3d pos = rigidbody.rotateLocal(rigidbody.localCenterOfMass).add(entityOffsetPosition);
            mass+=rigidbody.localMass;
            centerOfMass = centerOfMass.add(pos.scale(rigidbody.localMass));
        }
        centerOfMass = centerOfMass.scale(1/mass);

        inertiaTensor = localInertiaTensor.clone();
        Vector3d localShift = centerOfMass.subtract(localCenterOfMass);
        double[] posArray=new double[]{localShift.x,localShift.y,localShift.z};
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                inertiaTensor[i][j] -= localMass*posArray[i]* posArray[j];
        for (int i = 0; i < 3; i++) inertiaTensor[i][i] +=localMass * localShift.lengthSqr();


        for(Map.Entry<UUID,SubcontraptionRigidbody> entry : subcontraptionRigidbodyMap.entrySet())
        {
            SubcontraptionRigidbody rigidbody = entry.getValue();
            Vector3d entityOffsetPosition = rigidbody.entity.position().subtract(getPlotOffset());
            Vector3d pos = rigidbody.rotateLocal(rigidbody.localCenterOfMass).add(entityOffsetPosition);

            pos = pos.subtract(centerOfMass);

            posArray=new double[]{pos.x,pos.y,pos.z};
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    inertiaTensor[i][j]+= rigidbody.localInertiaTensor[i][j] - rigidbody.localMass*posArray[i]* posArray[j];
            for (int i = 0; i < 3; i++) inertiaTensor[i][i] +=rigidbody.localMass * pos.lengthSqr();
        }

        if(previousCenterOfMass == null) previousCenterOfMass = centerOfMass;

        if(!previousCenterOfMass.equals(centerOfMass)) {
            Vector3d movement = centerOfMass.subtract(previousCenterOfMass);
            contraption.entity.move(movement.x, movement.y, movement.z);
        }

        previousCenterOfMass = centerOfMass;
        /*Map<BlockPos, Template.BlockInfo> blocks = contraption.getBlocks();
        double maxDistance = 0;
        double maxDistanceSquared = 0;
        for (Map.Entry<BlockPos, Template.BlockInfo> entry : blocks.entrySet()) {
            Vector3d vec = new Vector3d(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());

            if (vec.lengthSqr() > maxDistanceSquared) {
                maxDistanceSquared = vec.lengthSqr();
                maxDistance = vec.length();

            }
        }
        maxDistance += 2 + centerOfMass.length();
        Vector3d start = new Vector3d(maxDistance, maxDistance, maxDistance).add(adapter.position());
        Vector3d end = new Vector3d(-maxDistance, -maxDistance, -maxDistance).add(adapter.position());
        contraption.entity.setBoundingBox(new AxisAlignedBB(end, start));*/
    }
    void updateRotation()
    {
        //Find the eigenvector decomposition of the inertia tensor
        //in terms of principal components and a quaternion rotation


        //column vectors of the original inertia tensor
        inertiaTensorI=new Vector3d(inertiaTensor[0][0],inertiaTensor[0][1],inertiaTensor[0][2]);
        inertiaTensorJ=new Vector3d(inertiaTensor[1][0],inertiaTensor[1][1],inertiaTensor[1][2]);
        inertiaTensorK=new Vector3d(inertiaTensor[2][0],inertiaTensor[2][1],inertiaTensor[2][2]);

        //decomposition into principal components
        Vector3d principalVectorI = getPrincipalComponent(0);
        Vector3d principalVectorJ = getPrincipalComponent(1);
        Vector3d principalVectorK = getPrincipalComponent(2);
        principalInertia[0]=principalVectorI.length();
        principalInertia[1]=principalVectorJ.length();
        principalInertia[2]=principalVectorK.length();

        double determinant = principalVectorI.dot(principalVectorJ.cross(principalVectorK));

        inverseInertiaTensorI = inertiaTensorJ.cross(inertiaTensorK).scale(1/determinant);
        inverseInertiaTensorJ = inertiaTensorK.cross(inertiaTensorI).scale(1/determinant);
        inverseInertiaTensorK = inertiaTensorI.cross(inertiaTensorJ).scale(1/determinant);

        updateSpectralDecomposition();

        //global torque to local reference frame
        localTorque = localTorque.add(MathUtils.rotateQuatReverse(globalTorque,orientation));

        //torque gives a change of angular momentum over time
        angularMomentum=angularMomentum.add(localTorque.scale(deltaTime));

        //rotate the angular momentum into the principal reference frame and scale by the inverse of the inertia
        //tensor to get angular velocity in the principal frame
        Vector3d principalVelocity = MathUtils.rotateQuat(multiplyInertiaInverse(angularMomentum), principalRotation);

        //euler's rotation equations
        Vector3d principalTorque = new Vector3d(
                (principalInertia[2] - principalInertia[1]) * principalVelocity.y * principalVelocity.z,
                (principalInertia[0] - principalInertia[2]) * principalVelocity.z * principalVelocity.x,
                (principalInertia[1] - principalInertia[0]) * principalVelocity.x * principalVelocity.y
        );

        //rotate the torque back to the contraption grid
        Vector3d extraTorque = MathUtils.rotateQuatReverse(principalTorque, principalRotation);

        double momentumMag = angularMomentum.length();

        angularMomentum = angularMomentum.add(extraTorque.scale(deltaTime));

        if (angularMomentum.lengthSqr() > 0)//reset the length to maintain conservation of momentum
        {
            angularMomentum=angularMomentum.normalize();
            angularMomentum=angularMomentum.scale(momentumMag);
        }

        angularVelocity = multiplyInertiaInverse(angularMomentum);
    }

    List<Contact> findContacts() {
        List<Contact> contacts = new ArrayList<>();

        // to handle collisions, we need to test every collision shape against the blocks they could collide with
        // first let's call an overload on each subcontraption and this rigidbody to prevent duplicate code
        findContacts(this, contacts);

        for (SubcontraptionRigidbody subcontraption : subcontraptionRigidbodyMap.values()) {
            // repeat for number of iterations
            //findContacts(subcontraption, contacts);
        }

        return contacts;
    }

    ICollisionDetector collisionDetector;


    void findContacts(AbstractContraptionRigidbody rb, List<Contact> contacts) {
        collisionDetector.solve(rb, contacts);
    }

    void initCollision() {

        // Set collision detector based off of quality settings
        switch (CAConfig.COLLISION_QUALITY.get()) {
            case GJKEPA:
                collisionDetector = new GJKCollisionDetector();
                break;
            case SPHERE_AABB:
                collisionDetector = new SphereAABBCollisionDetector();
                break;
            case SPHERE:
                collisionDetector = new SphereCollisionDetector();
                break;
        }

        // generate collision shapes for all the rigid bodies
        generateCollisionShapes(this);
        for(AbstractContraptionRigidbody rb : subcontraptionRigidbodyMap.values()) {
            generateCollisionShapes(rb);
        }
    }

    void generateCollisionShapes(AbstractContraptionRigidbody rb) {
        MeshCollisionShapeGenerator generator = new MeshCollisionShapeGenerator(rb);

        rb.setCollisionShapes(generator.generateShapes(contraption));
    }


    void updateSpectralDecomposition()
    {
        // attempts to perform spectral decomposition on the local inertia tensor = A
        // this is done by finding a rotation Q such that D = (Q^-1)AQ is a diagonal matrix
        // the way this is done is by starting with some attempted matrix M = (Q^-1)AQ using the current rotation Q
        // and then trying to find a small pertubation rotation dQ that causes the resulting matrix (dQ^-1)MdQ
        // to become more diagonal than what M currently is
        // dQ is defined using a small cross product dQ(v) = v + cross(v,k)

        //scaleDown parameter is used to scale down the matrix to have approximatly unit length column vectors
        //somehow this makes the algorithm far more stable
        double scaleDown = 0;
        for (int i =0;i<3;i++)
        {
            for (int j =0;j<3;j++)
            {
                scaleDown+=localInertiaTensor[i][j]*localInertiaTensor[i][j];
            }
        }
        scaleDown=Math.sqrt(scaleDown);

        // attemptedDecomposition is the attempted result matrix using the current rotation
        // attemptedDecomposition = rotationInverse * localInertiaTensor * rotation
        // the goal is to get this to be a diagonal matrix, as then the principialRotation (if expressed as a matrix)
        // will have the principial axies as column vectors,
        // and the resulting diagonal matrix will contain the moments of inertia
        double[][] attemptedDecomposition = new double[3][3];
        for (int i =0;i<3;i++)
        {

            Vector3d v=getPrincipalComponent(i);

            attemptedDecomposition[i][0]=v.x/scaleDown;
            attemptedDecomposition[i][1]=v.y/scaleDown;
            attemptedDecomposition[i][2]=v.z/scaleDown;
        }
        // numerically that goal corresponds to this cost value being as low as possible
        // as this cost is the sum of the squares of the non-diagonal elements
        // so a diagonal matrix will have zero cost
        double cost = 0;
        for (int i =0;i<3;i++)
        {
            for (int j =0;j<3;j++)
            {
                if(i!=j)
                    cost+=attemptedDecomposition[i][j]*attemptedDecomposition[i][j];
            }
        }
        Vector3d gradientVector=new Vector3d(0,0,0);
        for (int i =0;i<3;i++) {
            for (int j = 0; j < 3; j++) {
                if(i==j) continue;
                Vector3d v=new Vector3d(0,0,0);
                for (int k = 0; k < 3; k++)
                {
                    double scalar=0;
                    for(int r=0;r<3;r++)
                    {
                        if(k==r) continue;
                        scalar+=PhysicsUtils.LeviCivitaTensor[k][i][r]*attemptedDecomposition[r][j];
                        scalar-=PhysicsUtils.LeviCivitaTensor[k][r][j]*attemptedDecomposition[i][r];
                    }
                    v=setVectorFromIndex(k,scalar,v);
                }
                gradientVector=gradientVector.add(v.scale(2.0*attemptedDecomposition[i][j]));
            }
        }

        double minCost=0.000000001;
        double stepScale=0.2;
        if(cost<minCost||gradientVector.lengthSqr()==0)
            return;
        //newtons method
        Vector3d change = gradientVector.scale(stepScale*(cost-minCost)/gradientVector.lengthSqr());

        //System.out.println("Cost: "+cost);
        Quaternion q=new Quaternion((float)change.x,(float)change.y,(float)change.z,1.0f);
        principalRotation.mul(q);
        principalRotation.normalize();
    }
    Vector3d getPrincipalComponent(int column)
    {
        Vector3d v=setVectorFromIndex(column,1);
        v= MathUtils.rotateQuat(v, principalRotation);
        v=multiplyInertia(v);
        v= MathUtils.rotateQuatReverse(v, principalRotation);
        return v;
    }
    /*void blockAddedEvent(BlockPos blockPos,Template.BlockInfo info)
    {
        LocalCenterOfMass = (LocalCenterOfMass * LocalMass + (Vector3)(Pos) * B.mass) / (LocalMass + B.mass);
        LocalMass += B.mass;
        double blockMass=getBlockMass(info);
        Vector3d pos=getLocalCoordinate(blockPos);
        double[] posArray=new double[]{pos.x,pos.y,pos.z};

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                localInertiaTensor[i][j]-=blockMass*posArray[i]* posArray[j];
        for (int i = 0; i < 3; i++) localInertiaTensor[i][i] += blockMass * pos.lengthSqr();

    }
    void blockRemovedEvent(BlockPos blockPos,Template.BlockInfo info)
    {
        LocalCenterOfMass = (LocalCenterOfMass * LocalMass - (Vector3)(Pos) * B.mass) / (LocalMass - B.mass);
        LocalMass -= B.mass;
        double blockMass=getBlockMass(info);
        Vector3d pos=getLocalCoordinate(blockPos);
        double[] posArray=new double[]{pos.x,pos.y,pos.z};

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                localInertiaTensor[i][j]-=blockMass*posArray[i]* posArray[j];
        for (int i = 0; i < 3; i++) localInertiaTensor[i][i] += blockMass * pos.lengthSqr();
    }*/
    Vector3d setVectorFromIndex(int i,double value)
    {
        double x=0,y=0,z=0;
        switch (i)
        {
            case(0):x=value;break;
            case(1):y=value;break;
            case(2):z=value;break;
        }
        return new Vector3d(x,y,z);
    }
    Vector3d setVectorFromIndex(int i,double value,Vector3d original)
    {
        double x=original.x,y=original.y,z=original.z;
        switch (i)
        {
            case(0):x+=value;break;
            case(1):y+=value;break;
            case(2):z+=value;break;
        }
        return new Vector3d(x,y,z);
    }

    double getIndexedVectorValue(int i,Vector3d V)
    {
        switch (i)
        {
            case(0):return V.x;
            case(1):return V.y;
            case(2):return V.z;
        }
        return 0;
    }
    double sq(double x)
    {
        return x*x;
    }
    Vector3d multiplyMatrixArray(double[][] M,Vector3d v)
    {
        Vector3d out = new Vector3d(0,0,0);
        for (int i =0;i<3;i++) {
            out=out.add(
            M[i][0]*getIndexedVectorValue(i,v),
            M[i][1]*getIndexedVectorValue(i,v),
            M[i][2]*getIndexedVectorValue(i,v)
            );
        }
        return out;
    }
    void updateWings()
    {
        findSails(this.contraption);

        for (Map.Entry<BlockPos, BlockState> entry : sails.entrySet())
        {
            Vector3d pos = getLocalCoordinate(entry.getKey());
            Vector3d vel = getLocalVelocityAtPosition(pos);
            Vector3d normal = getFacingVector(entry.getValue());
            Vector3d force = normal.scale(-8.0f*normal.dot(vel));
            addForce(force,pos);
        }

        for (Map.Entry<UUID, SubcontraptionRigidbody> entry : subcontraptionRigidbodyMap.entrySet())
        {
            AbstractContraptionEntity entity = entry.getValue().entity;
            Contraption subContraption = entity.getContraption();

            entry.getValue().findSails(subContraption);

            if(entity instanceof ControlledContraptionEntity) {
                ControlledContraptionEntity controlledEntity = (ControlledContraptionEntity)entity;
                TileEntity te = entity.level.getBlockEntity(((ControlledContraptionEntityMixin)controlledEntity).getControllerPos());
                if (te instanceof PropellerBearingTileEntity) {
                    continue;
                }
            }

            for (Map.Entry<BlockPos, Template.BlockInfo> blockStateEntry : subContraption.getBlocks().entrySet())
            {
                if(blockStateEntry.getValue().state.getBlock() instanceof SailBlock) {

                    Vector3d pos = VecHelper.getCenterOf(blockStateEntry.getKey());
                    pos=entry.getValue().toParent(pos);

                    Vector3d vel = getLocalVelocityAtPosition(pos);
                    Vector3d normal = getFacingVector(blockStateEntry.getValue().state);
                    normal = entity.applyRotation(normal,1);
                    Vector3d force = normal.scale(-8.0f*normal.dot(vel));
                    addForce(force,pos);

                }
            }
        }

    }
    public Vector3d getPlotOffset()
    {
        return adapter.getPlotOffset();
//        BlockPos plotPos = AirshipManager.getPlotPosFromId(entity.plotId);
//        if(entity.level.isClientSide) {
//            return new Vector3d(0, plotPos.getY(), 0);
//        } else {
//            return new Vector3d(plotPos.getX(), plotPos.getY(), plotPos.getZ());
//        }
    }
    void updateTileEntityInteractions()
    {
        Vector3d TotalForce=Vector3d.ZERO;
        for (Map.Entry<BlockPos, TileEntity> entry : contraption.presentTileEntities.entrySet())
        {
            TileEntity te = entry.getValue();
            BlockPos pos = entry.getKey();
            Vector3d posV = getLocalCoordinate(entry.getKey());
            //if(CATileEntities.PROPELLER_BEARING.is(te))
                //addForce(getForcePropellerBearing(pos,(PropellerBearingTileEntity)te),posV);
            if(te instanceof IThrustProvider)
            {
                double airPressure = PhysicsUtils.getAirPressure(toGlobal(getLocalCoordinate(pos)));
                Vector3d relativeVelocity = rotateInverse(getVelocityAtPoint(posV));
                addForce(((IThrustProvider) te).getForce(pos,airPressure,relativeVelocity,this),posV);
            }

            if(AllTileEntities.ENCASED_FAN.is(te))
                addForce(getForceEncasedFan(pos,(EncasedFanTileEntity)te),posV);

        }
        momentum=momentum.add(TotalForce);
    }
    public double getMass()
    {
        return mass;
    }

    public Vector3d getCenterOfMass() {
        return centerOfMass;
    }
    //angular momentum to angular velocity
    public Vector3d multiplyInertia(Vector3d v) {
        return inertiaTensorI.scale(v.x).add(inertiaTensorJ.scale(v.y)).add(inertiaTensorK.scale(v.z));
    }
    //angular velocity to angular momentum
    public Vector3d multiplyInertiaInverse(Vector3d v) {
        return inverseInertiaTensorI.scale(v.x).add(inverseInertiaTensorJ.scale(v.y)).add(inverseInertiaTensorK.scale(v.z));
    }

    public Vector3d rotate(Vector3d point) {
        return MathUtils.rotateQuat(point,orientation);
    }

    public Vector3d rotateInverse(Vector3d point) {
        return MathUtils.rotateQuatReverse(point,orientation);
    }

    public Vector3d rotateLocal(Vector3d point) {
        return MathUtils.rotateQuat(point,orientation);
    }

    public Vector3d rotateLocalInverse(Vector3d point) {
        return MathUtils.rotateQuatReverse(point,orientation);
    }

    public Vector3d toLocal(Vector3d globalPoint) {
        Vector3d rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        return MathUtils.rotateQuatReverse(globalPoint.subtract(adapter.position()).subtract(rotationOffset),orientation);
    }

    public Vector3d toGlobal(Vector3d localPoint) {
        Vector3d rotationalOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        return MathUtils.rotateQuat(localPoint.subtract(rotationalOffset).subtract(centerOfMass),orientation).add(rotationalOffset).add(adapter.position());
    }

    public Vector3d getVelocity() {
        return globalVelocity;
    }

    public Vector3d getVelocityAtPoint(Vector3d pos) {

        return globalVelocity.add(rotate(pos.cross(angularVelocity)));

    }
    public Vector3d getAngularVelocity() {
        return angularVelocity;
    }

    public void addForce(Vector3d force, Vector3d pos)
    {
        localForce = localForce.add(force);
        localTorque = localTorque.add(force.cross(pos));
    }
    public void addGlobalForce(Vector3d force, Vector3d pos)
    {
        globalForce = globalForce.add(force);
        globalTorque = globalTorque.add(force.cross(pos));
    }


    public void applyImpulse(Vector3d pos, Vector3d impulse) {
        momentum = momentum.add(impulse);
        globalVelocity = momentum.scale(1.0 / getMass());
        localVelocity = MathUtils.rotateQuatReverse(globalVelocity,orientation);

        Vector3d additionalAngularMomentum = rotateInverse(impulse).cross(pos);
        angularMomentum = angularMomentum.add(additionalAngularMomentum);
        updateRotation();
    }


    public void applyGlobalImpulse(Vector3d pos, Vector3d impulse) {
        applyImpulse(toLocal(pos), impulse);
    }

    Vector3d getLocalVelocityAtPosition(Vector3d pos)
    {
        return localVelocity.add(pos.cross(angularVelocity));
    }

    //Vector3d getForcePropellerBearing(BlockPos pos,PropellerBearingTileEntity te)

    Vector3d getForceEncasedFan(BlockPos pos,EncasedFanTileEntity te)
    {

        Vector3d facingVector = getFacingVector(te.getBlockState());
        Vector3d direction= facingVector;
        // abs dir
        direction = new Vector3d(Math.abs(direction.x),Math.abs(direction.y),Math.abs(direction.z));
        float magnitude = 0.5f*te.getSpeed();

        Vector3d vector3d = adapter.toGlobalVector(new Vector3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5,0.5,0.5), 1.0f);
        Vector3d pPos = vector3d;
        Vector3d veloVector = adapter.applyRotation(facingVector,1.0f);

        float particleSpeed = te.getSpeed() / 256;
        veloVector = veloVector.scale(Math.abs(particleSpeed));
        if(Math.abs(particleSpeed) > 0) {
//            entity.addParticle(new PropellerAirParticleData(new Vector3i(vector3d.x, vector3d.y, vector3d.z)), pPos.x, pPos.y, pPos.z, veloVector.x, veloVector.y, veloVector.z);
        }

        return direction.scale(-magnitude);
    }

    Vector3d getFacingVector(BlockState state)
    {
        Direction direction = state.getValue(BlockStateProperties.FACING);
        return new Vector3d(direction.getNormal().getX(), direction.getNormal().getY(), direction.getNormal().getZ());
    }
    Vector3d getLocalCoordinate(BlockPos pos)
    {
        Vector3d p=new Vector3d(pos.getX(),pos.getY(),pos.getZ());
        return p.subtract(centerOfMass);
    }
    Vector3d getLocalCoordinate(Vector3d pos)
    {
        return pos.subtract(centerOfMass);
    }
    class BuoyancyController
    {
        Vector3d averagePos;
        int totalCount;
        Vector3d upVector;
        Vector3d projectedAveragePos;
        double averageSquaredMagnitudes;
        double strengthScale=0.0;
        public List<Vector3d> points;
        public BuoyancyController(double strengthScale)
        {
            this.strengthScale=strengthScale;
            upVector=new Vector3d(0,1,0);
        }
        public void set(List<Vector3d> points)
        {

            this.points=points;
            averagePos=Vector3d.ZERO;
            projectedAveragePos=Vector3d.ZERO;
            averageSquaredMagnitudes=0.0;
            totalCount=points.size();
            if(totalCount==0)
                return;
            for (Vector3d p:points) {
                averagePos=averagePos.add(p);
                projectedAveragePos=projectedAveragePos.add(p.scale(p.dot(upVector)));
                averageSquaredMagnitudes+=p.dot(p);
            }
            averagePos=averagePos.scale(1.0/totalCount);
            projectedAveragePos=projectedAveragePos.scale(1.0/totalCount);
            averageSquaredMagnitudes*=(1.0/totalCount);
        }

        public Tuple<Vector3d, Vector3d> getForceAndPosition(Quaternion rotation, Vector3d referencePos)
        {
            if(totalCount==0)
                return new Tuple<>(Vector3d.ZERO, Vector3d.ZERO);
            /*
            calculates the total buoyancy force and point of average force application
            average force application is the weighted average of the positions,
            with the weights being the buoyancy forces at the worldpositions of each point
            weight = gravity*getAirPressure(worldPos)*strengthScale
            total buoyancy is then the sum of all those weights
            */

            double referenceBuoyancy = PhysicsUtils.gravity*PhysicsUtils.getAirPressure(referencePos)*strengthScale;
            double referenceBuoyancyDerivative = PhysicsUtils.gravity* PhysicsUtils.getAirPressureDerivative(referencePos)*strengthScale;
            Vector3d rotatedAverage = MathUtils.rotateQuat(averagePos,rotation);
            double averageBuoyancy = referenceBuoyancy + rotatedAverage.dot(upVector) * referenceBuoyancyDerivative;
            double totalBuoyancy = averageBuoyancy*totalCount;

            Vector3d circleCenter = upVector.scale(averageSquaredMagnitudes*0.5);
            Vector3d circularOffset =projectedAveragePos.subtract(circleCenter);
            circularOffset = circleCenter.add(MathUtils.rotateQuat(MathUtils.rotateQuat(circularOffset,rotation),rotation));

            Vector3d averageBuoyancyPosition =
                    (rotatedAverage.scale(referenceBuoyancy)
                            .add(circularOffset.scale(referenceBuoyancyDerivative)))
                            .scale(1.0/averageBuoyancy);
            return new Tuple<>(upVector.scale(totalBuoyancy),averageBuoyancyPosition);
        }
        public double apply(Quaternion rotation, Vector3d referencePos)
        {

            Tuple<Vector3d, Vector3d> T = getForceAndPosition(rotation,referencePos);

            addGlobalForce(T.getA(),T.getB());

            return T.getA().length();

        }
    }

    /**
     * Converts a vector to an array
     */
    public double[] vecToArray(Vector3d v)
    {
        return new double[]{v.x,v.y,v.z};
    }

    /**
     * Converts an array to a vector
     */
    public Vector3d arrayToVec(double[] array)
    {
        return new Vector3d(array[0],array[1],array[2]);
    }

    /**
     * Converts a quaternion to an array
     */
    public double[] quatToArray(Quaternion q)
    {
        return new double[]{q.i(),q.j(),q.k(),q.r()};
    }

    /**
     * Converts an array to a quaternion
     */
    public Quaternion arrayToQuat(double[] array)
    {
        return new Quaternion((float)array[0], (float)array[1], (float)array[2], (float)array[3]);
    }

}
