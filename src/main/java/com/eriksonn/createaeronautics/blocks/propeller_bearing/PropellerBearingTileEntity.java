package com.eriksonn.createaeronautics.blocks.propeller_bearing;

import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.particle.PropellerAirParticle;
import com.eriksonn.createaeronautics.particle.PropellerAirParticleData;
import com.eriksonn.createaeronautics.physics.AbstractContraptionRigidbody;
import com.eriksonn.createaeronautics.physics.PhysicsUtils;
import com.eriksonn.createaeronautics.physics.api.IThrustProvider;
import com.eriksonn.createaeronautics.physics.collision.shape.MeshCollisionShape;
import com.eriksonn.createaeronautics.physics.collision.shape.MeshCollisionShapeGenerator;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.components.fan.AirCurrent;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.state.properties.BlockStateProperties.FACING;

public class PropellerBearingTileEntity extends MechanicalBearingTileEntity implements MecanicalBearingTileEntityExtension, IThrustProvider {
    public ScrollOptionBehaviour<ThrustDirection> movementDirection;
    protected float lastGeneratedSpeed;
    public List<BlockPos> sailPositions;
    public float rotationSpeed = 0;
    public float disassemblyTimer;
    public boolean disassemblySlowdown = false;
    public Vector3d thrustDirection = Vector3d.ZERO;
    public float disassemblyTimerTotal;
    public float disassemblyTimerScale = 3.5f;
    public static final float particleSpeedScale = 0.04f;
    double radius = 0;
    public double lastThrustOutput;
    private boolean insideMainTick=false;
    public float prevAngle;
    double currentAirPressure=1;


    public PropellerBearingTileEntity(TileEntityType<? extends MechanicalBearingTileEntity> type) {
        super(type);
        sailPositions = new ArrayList<>();
    }

    @Override
    public float calculateStressApplied() {
        if (!running)
            return 0;
        int sails = 0;
        if (movedContraption != null) {
            sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks();
        }
        sails = Math.max(sails, 2);
        return sails * 2f;
    }


    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode.setValue(2);
        behaviours.remove(movementMode);
        movementDirection = new ScrollOptionBehaviour<>(ThrustDirection.class,
                new StringTextComponent("Thrust Direction"), this, getMovementModeSlot());

        movementDirection.requiresWrench();
        movementDirection.withCallback($ -> onDirectionChanged());
        behaviours.add(movementDirection);
    }

    private void onDirectionChanged() {

        if (!running)
            return;
        if (!level.isClientSide)
            updateGeneratedRotation();
    }

    @Override
    public void write(CompoundNBT compound, boolean clientPacket) {
        compound.putFloat("LastGenerated", lastGeneratedSpeed);
        compound.putFloat("RotationSpeed", rotationSpeed);
        super.write(compound, clientPacket);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        if (!wasMoved)
            lastGeneratedSpeed = compound.getFloat("LastGenerated");
        rotationSpeed = compound.getFloat("RotationSpeed");
        super.fromTag(state, compound, clientPacket);
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    public float getAngularSpeed() {
        float speed = rotationSpeed;

        if(insideMainTick&&disassemblySlowdown)
            speed=getPartialVelocity(1);

        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return MathHelper.lerp(partialTicks + .5f, prevAngle, angle);
        if (movedContraption == null || movedContraption.isStalled() || !running)
            partialTicks = 0;
        if(disassemblySlowdown)
            return angle + getPartialVelocity(partialTicks);
        else
            return MathHelper.lerp(partialTicks, angle, angle + getAngularSpeed());
    }

    @Override
    public void tick() {
        prevAngle=angle;
        if (disassemblySlowdown)
            updateSlowdownSpeed();
        else
            updateRotationSpeed();

        insideMainTick=true;
        super.tick();
        insideMainTick=false;



        if (movedContraption != null && !movedContraption.isAlive())
            movedContraption = null;

        if (movedContraption != null) {
            pushEntities();
        }
        if (level.isClientSide) {
            //tickRotation();
            spawnParticles();
            return;
        }
        if (speed != 0)
            lastGeneratedSpeed = speed;
    }

    void pushEntities() {

        AirshipManager.AirshipOrientedInfo orientedInfo = AirshipManager.INSTANCE.getInfo(level, worldPosition);
        currentAirPressure = PhysicsUtils.getAirPressure(orientedInfo.position);

        double distance = getParticleRange() * 1.25;
        Vector3d center = VecHelper.getCenterOf(worldPosition).add(thrustDirection);
        Vector3d directedNormal = thrustDirection.scale(getDirectionScale());

        Vector3d A = directedNormal.scale(distance);
        Vector3d B = new Vector3d(directedNormal.y, directedNormal.z, directedNormal.x).scale(radius);
        Vector3d C = new Vector3d(directedNormal.z, directedNormal.x, directedNormal.y).scale(radius);

        Vector3d min = center.subtract(B).subtract(C);
        Vector3d max = center.add(A).add(B).add(C);

        AxisAlignedBB boundingBox = new AxisAlignedBB(min, max);
        List<Entity> Entities;
        if (orientedInfo.onAirship) {
            Vector3d[] vertecies = MeshCollisionShapeGenerator.vertices(boundingBox);
            for (int i = 0; i < vertecies.length; i++) {
                vertecies[i] = orientedInfo.airship.toGlobalVector(
                        vertecies[i].subtract(orientedInfo.airship.simulatedRigidbody.getPlotOffset()), 1);

            }
            boundingBox = new MeshCollisionShape(vertecies).getBounds();
            Entities = orientedInfo.level.getEntities(null, boundingBox);
        } else
            Entities = level.getEntities(null, boundingBox);

        //if(level.isClientSide)
            //CreateClient.OUTLINER.showAABB("propeller bearing" + worldPosition, boundingBox);

        directedNormal = MathUtils.rotateQuat(directedNormal, orientedInfo.orientation);


        for (Entity entity : Entities) {
            if (entity instanceof AbstractContraptionEntity || AirCurrent.isPlayerCreativeFlying(entity))
                continue;

            Vector3d entityPosition = entity.getBoundingBox().getCenter();
            Vector3d relativePosition = entityPosition.subtract(orientedInfo.position);
            relativePosition=relativePosition.subtract(MathUtils.rotateQuat(thrustDirection,orientedInfo.orientation));

            if (MathUtils.isInCylinder(directedNormal, relativePosition, distance, radius)) {

                Vector3d startVector = entityPosition.subtract(directedNormal.scale(directedNormal.dot(relativePosition)));



                //RaycastHelper.PredicateTraceResult result =  RaycastHelper
                //        .rayTraceUntil(startVector,entityPosition,(x) -> !level.getBlockState(x).isAir());

                BlockRayTraceResult result =  level.clip(new RayTraceContext(
                        startVector,
                        entityPosition,
                        RayTraceContext.BlockMode.COLLIDER,
                        RayTraceContext.FluidMode.NONE,
                        null));
                //CreateClient.OUTLINER.showLine("propeller bearing" + worldPosition,startVector,entityPosition);
                if(result.getType() != RayTraceResult.Type.MISS)
                {
                    continue;
                }
                Vector3d previousMotion = entity.getDeltaMovement();
                float sneakModifier = entity.isShiftKeyDown() ? 4096f : 512f;
                float speed = (float)Math.abs(getAirFlow())*4f;
                double entityDistance = directedNormal.dot(relativePosition);
                float acceleration = (float) (speed / (sneakModifier * Math.pow((entityDistance + 0.25) / distance,0.5))) * 3f;
                acceleration *= Math.pow(1 - entityDistance / distance, 0.15)*currentAirPressure;
                float maxAcceleration = 5;

                double xIn =
                        MathHelper.clamp(directedNormal.x * acceleration - previousMotion.x, -maxAcceleration, maxAcceleration);
                double yIn =
                        MathHelper.clamp(directedNormal.y * acceleration - previousMotion.y, -maxAcceleration, maxAcceleration);
                double zIn =
                        MathHelper.clamp(directedNormal.z * acceleration - previousMotion.z, -maxAcceleration, maxAcceleration);

                entity.setDeltaMovement(previousMotion.add(new Vector3d(xIn, yIn, zIn).scale(1 / 8f)));
                entity.fallDistance = 0;
            }
        }
    }

    void updateRotationSpeed() {
        float nextSpeed = convertToAngular(getSpeed());
        if (getSpeed() == 0)
            nextSpeed = 0;
        if (sailPositions.size() > 0) {

            //Larger propellers accelerate slower
            float lerpAmount = 0.4f / (float)Math.sqrt(sailPositions.size());
            rotationSpeed = MathHelper.lerp(lerpAmount, rotationSpeed, nextSpeed);
        } else {
            rotationSpeed = nextSpeed;
        }
    }

    public void updateSlowdownSpeed() {
        disassemblyTimer--;
        if (disassemblyTimer <= 0.5) {
            if (!level.isClientSide)
                disassemble();
            disassemblySlowdown = false;

            running = false;
            return;
        }

        // the angle it will end up at if slowing down at a constant rate
        float currentStoppingPoint = (angle + rotationSpeed * disassemblyTimer * 0.5f);

        // the closest grid-aligned angle to currentStoppingPoint
        float optimalStoppingPoint = 90f * Math.round(currentStoppingPoint / 90f);

        float Q = (optimalStoppingPoint - currentStoppingPoint) / disassemblyTimer;

        rotationSpeed = (rotationSpeed + 6f * Q / disassemblyTimer) * (1f - 1f / disassemblyTimer);
    }
    public float getPartialVelocity(float partialTick)
    {
        // the angle it will end up at if slowing down at a constant rate
        float currentStoppingPoint = (angle + rotationSpeed * disassemblyTimer * 0.5f);

        // the closest grid-aligned angle to currentStoppingPoint
        float optimalStoppingPoint = 90f * Math.round(currentStoppingPoint / 90f);

        float Q = (optimalStoppingPoint - currentStoppingPoint) / disassemblyTimer;

        float scaledTime = partialTick/disassemblyTimer;

        return partialTick * (rotationSpeed + scaledTime*(
                3f*Q - rotationSpeed*0.5f - 2f*Q*scaledTime
                ));
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        super.attach(contraption);
        contraptionInitialize();
    }

    @Override
    public void assemble() {
        rotationSpeed = 0;
        super.assemble();
        contraptionInitialize();
    }

    @Override
    public void disassemble() {
        if (!running && movedContraption == null)
            return;
        angle = 0;
        applyRotation();
        super.disassemble();
    }

    public void setAssembleNextTick(boolean value) {
        assembleNextTick = value;
    }

    public void startDisassemblySlowdown() {
        if (!disassemblySlowdown) {
            disassemblySlowdown = true;
            disassemblyTimerTotal = 1 + disassemblyTimerScale * (float)Math.sqrt(sailPositions.size());
            disassemblyTimer = disassemblyTimerTotal;
        }
    }

    public void contraptionInitialize() {

        Direction direction = getBlockState().getValue(FACING);
        thrustDirection = new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        findSails();
    }

    public void findSails() {
        sailPositions = new ArrayList<>();
        radius = 0;
        if (movedContraption != null) {
            Map<BlockPos, Template.BlockInfo> Blocks = ((BearingContraption) movedContraption.getContraption()).getBlocks();
            for (Map.Entry<BlockPos, Template.BlockInfo> entry : Blocks.entrySet()) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(entry.getValue().state)) {
                    BlockPos currentPos = entry.getKey();
                    sailPositions.add(currentPos);

                    Vector3d relativePosition = new Vector3d(currentPos.getX(), currentPos.getY(), currentPos.getZ());
                    double distance = thrustDirection.dot(relativePosition);
                    relativePosition = relativePosition.subtract(thrustDirection.scale(distance));
                    radius = Math.max(radius, relativePosition.lengthSqr());
                }
            }
        }
        radius = Math.sqrt(radius) + 0.5;
    }

    public void spawnParticles() {
        if (Math.abs(rotationSpeed) > 0.01 && movedContraption != null && isRunning()) {
            World world = getLevel();
            Direction direction = getBlockState().getValue(BlockStateProperties.FACING);
            Vector3f speed = new Vector3f(thrustDirection);

            float directionScale = getDirectionScale();

            float offset = 1.0f + directionScale * 0.25f;

            float speedScale=(float)getAirFlow()/20;
            float particleCount = 0.02f * sailPositions.size() * Math.abs(rotationSpeed) * (float)currentAirPressure;

            particleCount += Create.RANDOM.nextFloat() - 1.0f;
            for (int i = 0; i < particleCount; i++) {
                BlockPos sailPos = sailPositions.get(Create.RANDOM.nextInt(sailPositions.size()));
                Vector3d floatPos = new Vector3d(sailPos.getX(), sailPos.getY(), sailPos.getZ());
                floatPos = movedContraption.applyRotation(floatPos, 0);

                Vector3d randomOffset = VecHelper.offsetRandomly(Vector3d.ZERO, Create.RANDOM, .5f);
                randomOffset = randomOffset.subtract(thrustDirection.scale(0.4*randomOffset.dot(thrustDirection)/thrustDirection.dot(thrustDirection)));

                Vector3d pos = VecHelper.getCenterOf(this.worldPosition)
                        .add(Vector3d.atLowerCornerOf(direction.getNormal())
                                .scale(offset))
                        .add(floatPos)
                        .add(randomOffset);



                world.addParticle(new PropellerAirParticleData(this.worldPosition), pos.x, pos.y, pos.z, speed.x() * speedScale, speed.y() * speedScale, speed.z() * speedScale);
            }
        }
    }

    public float getDirectionScale() {
        float speed = rotationSpeed;

        if (speed == 0)
            return 1;
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        speed = convertToDirection(speed, facing);
        if(movementDirection.value== 1)
            speed*=-1;
        return speed>0?1:-1;
    }
    public double getDirectedRotationRate()
    {
        float speed = rotationSpeed;
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        speed = convertToDirection(speed, facing);
        if(movementDirection.value== 1)
            speed*=-1;
        return speed;
    }

    public double getParticleRange() {
        double startVelocity = Math.abs(getAirFlow())/20;
        double a = startVelocity * PropellerAirParticle.frictionScale * PropellerAirParticle.lifeTime;
        return Math.log(a + 1) / PropellerAirParticle.frictionScale;
    }

    /**
     * The thrust of the propeller represents how hard it can push, or how much it can lift against gravity.
     * The actual thrust for any given situation also depends on the airspeed and airpressure
     * Dividing this thrust by the gravity constant
     * results in the amount of block-mass that can be lifted, if airpressure is ignored
     * Larger propellers are more stress-efficient at producing thrust than smaller ones,
     * due to the larger volume of air that is pushed
     * @return The thrust in units of (block-mass)*meters/second^2
     */
    public double getThrust()
    {
        int sails = sailPositions.size();
        return 0.1f * (float) Math.pow(sails, 1.5f) * getDirectedRotationRate()*(10/3.0);
    }

    /**
     * The airflow speed of the propeller represents the maximum speed that the propeller can push a contraption at
     * It also controls the speed and range of the particle effects,
     * and by extension how entities are pushed by the propeller
     * Smaller propellers are more stress-efficient at making higher airflows than larger ones, due to the higher rpm
     *
     * @return The current air flow in meters/second
     */
    public double getAirFlow()
    {
        return 0.1*Math.sqrt(sailPositions.size())*getDirectedRotationRate()*(10/3.0);
    }

    public Vector3d getForce(BlockPos localPos, double airPressure, Vector3d velocity, AbstractContraptionRigidbody rigidbody) {
        if (!isRunning())
            return Vector3d.ZERO;

        double magnitude = -getThrust();
        magnitude *= airPressure;
        double currentAirSpeed = velocity.dot(thrustDirection);
        double airFlow = -getAirFlow();
        //Reduce the magnitude of the thrust if the contraption is already moving,
        //as then the change in airflow due to the propeller will be smaller
        if(Math.abs(airFlow)>0.001)
            magnitude *= (airFlow-currentAirSpeed)/ airFlow;

        return thrustDirection.scale(magnitude);
    }

    public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
        boolean previous = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if(movedContraption!=null)
        {
            tooltip.add(componentSpacing.plainCopy()
                    .append(""));
            tooltip.add(componentSpacing.plainCopy()
                    .append("Propeller Bearing:"));

            tooltip.add(componentSpacing.plainCopy().plainCopy()
                    .append(new StringTextComponent(" Thrust: ").withStyle(TextFormatting.GRAY))
                    .append(new StringTextComponent(IHaveGoggleInformation.format(currentAirPressure*Math.abs(getThrust())/ PhysicsUtils.gravity)+" blocks").withStyle(TextFormatting.AQUA)));

            tooltip.add(componentSpacing.plainCopy().plainCopy()
                    .append(new StringTextComponent(" Airflow: ").withStyle(TextFormatting.GRAY))
                    .append(new StringTextComponent(IHaveGoggleInformation.format(Math.abs(getAirFlow()))+"m/s").withStyle(TextFormatting.AQUA)));
            return true;
        }
        return previous;
    }

    @Override
    public boolean isPropeller() {
        return true;
    }

    enum ThrustDirection implements INamedIconOptions {

        RIGHT_HANDED(AllIcons.I_REFRESH,"pull_when_clockwise"), LEFT_HANDED(AllIcons.I_ROTATE_CCW,"push_when_clockwise");

        private final String translationKey;
        private final AllIcons icon;

        ThrustDirection(AllIcons icon, String name) {
            this.icon = icon;
            translationKey = "generic." + name;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {

            return translationKey;
        }

    }
}
