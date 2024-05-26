package net.cystic.vendingmachines.block.custom;

import com.mojang.datafixers.types.templates.Check;
import net.cystic.vendingmachines.block.entity.ModBlockEntities;
import net.cystic.vendingmachines.block.entity.VendingMachineBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class VendingMachineBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public VendingMachineBlock(Settings settings) {
        super(settings);
        this.setDefaultState((this.stateManager.getDefaultState()).with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
    }

    private static final VoxelShape SHAPE_N_LOWER = Stream.of(
            Block.createCuboidShape(0, 1, 0, 4, 32, 16),
            Block.createCuboidShape(15, 1, 0, 16, 32, 16),
            Block.createCuboidShape(4, 1, 15, 15, 32, 16),
            Block.createCuboidShape(4, 7, 0, 15, 8, 15),
            Block.createCuboidShape(4, 4, 2, 15, 7, 3),
            Block.createCuboidShape(4, 8, 1, 15, 29, 2),
            Block.createCuboidShape(4, 29, 0, 15, 32, 15),
            Block.createCuboidShape(1, 20, -0.25, 3, 23, 0),
            Block.createCuboidShape(1, 18, -0.25, 3, 19, 0),
            Block.createCuboidShape(4, 12, 11, 15, 13, 15),
            Block.createCuboidShape(4, 16, 11, 15, 17, 15),
            Block.createCuboidShape(4, 20, 11, 15, 21, 15),
            Block.createCuboidShape(4, 24, 11, 15, 25, 15),
            Block.createCuboidShape(0, 0, 0, 2, 1, 2),
            Block.createCuboidShape(14, 0, 0, 16, 1, 2),
            Block.createCuboidShape(14, 0, 14, 16, 1, 16),
            Block.createCuboidShape(0, 0, 14, 2, 1, 16),
            Block.createCuboidShape(4, 1, 0, 15, 4, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_N_UPPER = Stream.of(
            Block.createCuboidShape(0, -15, 0, 4, 16, 16),
            Block.createCuboidShape(15, -15, 0, 16, 16, 16),
            Block.createCuboidShape(4, -15, 15, 15, 16, 16),
            Block.createCuboidShape(4, -9, 0, 15, -8, 15),
            Block.createCuboidShape(4, -12, 2, 15, -9, 3),
            Block.createCuboidShape(4, -8, 1, 15, 13, 2),
            Block.createCuboidShape(4, 13, 0, 15, 16, 15),
            Block.createCuboidShape(1, 4, -0.25, 3, 7, 0),
            Block.createCuboidShape(1, 2, -0.25, 3, 3, 0),
            Block.createCuboidShape(4, -4, 11, 15, -3, 15),
            Block.createCuboidShape(4, 0, 11, 15, 1, 15),
            Block.createCuboidShape(4, 4, 11, 15, 5, 15),
            Block.createCuboidShape(4, 8, 11, 15, 9, 15),
            Block.createCuboidShape(0, -16, 0, 2, -15, 2),
            Block.createCuboidShape(14, -16, 0, 16, -15, 2),
            Block.createCuboidShape(14, -16, 14, 16, -15, 16),
            Block.createCuboidShape(0, -16, 14, 2, -15, 16),
            Block.createCuboidShape(4, -15, 0, 15, -12, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_E_LOWER = Stream.of(
            Block.createCuboidShape(0, 1, 0, 16, 32, 4),
            Block.createCuboidShape(0, 1, 15, 16, 32, 16),
            Block.createCuboidShape(0, 1, 4, 1, 32, 15),
            Block.createCuboidShape(1, 7, 4, 16, 8, 15),
            Block.createCuboidShape(13, 4, 4, 14, 7, 15),
            Block.createCuboidShape(14, 8, 4, 15, 29, 15),
            Block.createCuboidShape(1, 29, 4, 16, 32, 15),
            Block.createCuboidShape(16, 20, 1, 16.25, 23, 3),
            Block.createCuboidShape(16, 18, 1, 16.25, 19, 3),
            Block.createCuboidShape(1, 12, 4, 5, 13, 15),
            Block.createCuboidShape(1, 16, 4, 5, 17, 15),
            Block.createCuboidShape(1, 20, 4, 5, 21, 15),
            Block.createCuboidShape(1, 24, 4, 5, 25, 15),
            Block.createCuboidShape(14, 0, 0, 16, 1, 2),
            Block.createCuboidShape(14, 0, 14, 16, 1, 16),
            Block.createCuboidShape(0, 0, 14, 2, 1, 16),
            Block.createCuboidShape(0, 0, 0, 2, 1, 2),
            Block.createCuboidShape(1, 1, 4, 16, 4, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_E_UPPER = Stream.of(
            Block.createCuboidShape(0, -15, 0, 16, 16, 4),
            Block.createCuboidShape(0, -15, 15, 16, 16, 16),
            Block.createCuboidShape(0, -15, 4, 1, 16, 15),
            Block.createCuboidShape(1, -9, 4, 16, -8, 15),
            Block.createCuboidShape(13, -12, 4, 14, -9, 15),
            Block.createCuboidShape(14, -8, 4, 15, 13, 15),
            Block.createCuboidShape(1, 13, 4, 16, 16, 15),
            Block.createCuboidShape(16, 4, 1, 16.25, 7, 3),
            Block.createCuboidShape(16, 2, 1, 16.25, 3, 3),
            Block.createCuboidShape(1, -4, 4, 5, -3, 15),
            Block.createCuboidShape(1, 0, 4, 5, 1, 15),
            Block.createCuboidShape(1, 4, 4, 5, 5, 15),
            Block.createCuboidShape(1, 8, 4, 5, 9, 15),
            Block.createCuboidShape(14, -16, 0, 16, -15, 2),
            Block.createCuboidShape(14, -16, 14, 16, -15, 16),
            Block.createCuboidShape(0, -16, 14, 2, -15, 16),
            Block.createCuboidShape(0, -16, 0, 2, -15, 2),
            Block.createCuboidShape(1, -15, 4, 16, -12, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_S_LOWER = Stream.of(
            Block.createCuboidShape(12, 1, 0, 16, 32, 16),
            Block.createCuboidShape(0, 1, 0, 1, 32, 16),
            Block.createCuboidShape(1, 1, 0, 12, 32, 1),
            Block.createCuboidShape(1, 7, 1, 12, 8, 16),
            Block.createCuboidShape(1, 4, 13, 12, 7, 14),
            Block.createCuboidShape(1, 8, 14, 12, 29, 15),
            Block.createCuboidShape(1, 29, 1, 12, 32, 16),
            Block.createCuboidShape(13, 20, 16, 15, 23, 16.25),
            Block.createCuboidShape(13, 18, 16, 15, 19, 16.25),
            Block.createCuboidShape(1, 12, 1, 12, 13, 5),
            Block.createCuboidShape(1, 16, 1, 12, 17, 5),
            Block.createCuboidShape(1, 20, 1, 12, 21, 5),
            Block.createCuboidShape(1, 24, 1, 12, 25, 5),
            Block.createCuboidShape(14, 0, 14, 16, 1, 16),
            Block.createCuboidShape(0, 0, 14, 2, 1, 16),
            Block.createCuboidShape(0, 0, 0, 2, 1, 2),
            Block.createCuboidShape(14, 0, 0, 16, 1, 2),
            Block.createCuboidShape(1, 1, 1, 12, 4, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_S_UPPER = Stream.of(
            Block.createCuboidShape(12, -15, 0, 16, 16, 16),
            Block.createCuboidShape(0, -15, 0, 1, 16, 16),
            Block.createCuboidShape(1, -15, 0, 12, 16, 1),
            Block.createCuboidShape(1, -9, 1, 12, -8, 16),
            Block.createCuboidShape(1, -12, 13, 12, -9, 14),
            Block.createCuboidShape(1, -8, 14, 12, 13, 15),
            Block.createCuboidShape(1, 13, 1, 12, 16, 16),
            Block.createCuboidShape(13, 4, 16, 15, 7, 16.25),
            Block.createCuboidShape(13, 2, 16, 15, 3, 16.25),
            Block.createCuboidShape(1, -4, 1, 12, -3, 5),
            Block.createCuboidShape(1, 0, 1, 12, 1, 5),
            Block.createCuboidShape(1, 4, 1, 12, 5, 5),
            Block.createCuboidShape(1, 8, 1, 12, 9, 5),
            Block.createCuboidShape(14, -16, 14, 16, -15, 16),
            Block.createCuboidShape(0, -16, 14, 2, -15, 16),
            Block.createCuboidShape(0, -16, 0, 2, -15, 2),
            Block.createCuboidShape(14, -16, 0, 16, -15, 2),
            Block.createCuboidShape(1, -15, 1, 12, -12, 16)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_W_LOWER = Stream.of(
            Block.createCuboidShape(0, 1, 12, 16, 32, 16),
            Block.createCuboidShape(0, 1, 0, 16, 32, 1),
            Block.createCuboidShape(15, 1, 1, 16, 32, 12),
            Block.createCuboidShape(0, 7, 1, 15, 8, 12),
            Block.createCuboidShape(2, 4, 1, 3, 7, 12),
            Block.createCuboidShape(1, 8, 1, 2, 29, 12),
            Block.createCuboidShape(0, 29, 1, 15, 32, 12),
            Block.createCuboidShape(-0.25, 20, 13, 0, 23, 15),
            Block.createCuboidShape(-0.25, 18, 13, 0, 19, 15),
            Block.createCuboidShape(11, 12, 1, 15, 13, 12),
            Block.createCuboidShape(11, 16, 1, 15, 17, 12),
            Block.createCuboidShape(11, 20, 1, 15, 21, 12),
            Block.createCuboidShape(11, 24, 1, 15, 25, 12),
            Block.createCuboidShape(0, 0, 14, 2, 1, 16),
            Block.createCuboidShape(0, 0, 0, 2, 1, 2),
            Block.createCuboidShape(14, 0, 0, 16, 1, 2),
            Block.createCuboidShape(14, 0, 14, 16, 1, 16),
            Block.createCuboidShape(0, 1, 1, 15, 4, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_W_UPPER = Stream.of(
            Block.createCuboidShape(0, -15, 12, 16, 16, 16),
            Block.createCuboidShape(0, -15, 0, 16, 16, 1),
            Block.createCuboidShape(15, -15, 1, 16, 16, 12),
            Block.createCuboidShape(0, -9, 1, 15, -8, 12),
            Block.createCuboidShape(2, -12, 1, 3, -9, 12),
            Block.createCuboidShape(1, -8, 1, 2, 13, 12),
            Block.createCuboidShape(0, 13, 1, 15, 16, 12),
            Block.createCuboidShape(-0.25, 4, 13, 0, 7, 15),
            Block.createCuboidShape(-0.25, 2, 13, 0, 3, 15),
            Block.createCuboidShape(11, -4, 1, 15, -3, 12),
            Block.createCuboidShape(11, 0, 1, 15, 1, 12),
            Block.createCuboidShape(11, 4, 1, 15, 5, 12),
            Block.createCuboidShape(11, 8, 1, 15, 9, 12),
            Block.createCuboidShape(0, -16, 14, 2, -15, 16),
            Block.createCuboidShape(0, -16, 0, 2, -15, 2),
            Block.createCuboidShape(14, -16, 0, 16, -15, 2),
            Block.createCuboidShape(14, -16, 14, 16, -15, 16),
            Block.createCuboidShape(0, -15, 1, 15, -12, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {

        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            switch (state.get(FACING)) {
                case NORTH:
                    return SHAPE_N_LOWER;
                case SOUTH:
                    return SHAPE_S_LOWER;
                case WEST:
                    return SHAPE_W_LOWER;
                case EAST:
                    return SHAPE_E_LOWER;
            }
        } else {
            switch (state.get(FACING)) {
                case NORTH:
                    return SHAPE_N_UPPER;
                case SOUTH:
                    return SHAPE_S_UPPER;
                case WEST:
                    return SHAPE_W_UPPER;
                case EAST:
                    return SHAPE_E_UPPER;
            }
        }
        return null;
    }
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf doubleBlockHalf = (DoubleBlockHalf)state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y && doubleBlockHalf == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighborState.isOf(this) && neighborState.get(HALF) != doubleBlockHalf ? (BlockState)((BlockState)((BlockState)((BlockState)state.with(FACING, (Direction)neighborState.get(FACING))))) : Blocks.AIR.getDefaultState();
        } else {
            return doubleBlockHalf == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(HALF, DoubleBlockHalf.LOWER);
        //BlockPos blockPos = ctx.getBlockPos();
        //World world = ctx.getWorld();
        //if (blockPos.getY() < world.getTopY() - 1 && world.getBlockState(blockPos.up()).canReplace(ctx)) {
        //    boolean bl = world.isReceivingRedstonePower(blockPos) || world.isReceivingRedstonePower(blockPos.up());
        //    return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing()))))).with(HALF, DoubleBlockHalf.LOWER);
        //} else {
        //    return null;
        //}
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        //builder.add(FACING);
        builder.add(HALF, FACING);
    }

    public long getRenderingSeed(BlockState state, BlockPos pos) {
        return MathHelper.hashCode(pos.getX(), pos.down(state.get(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    /* BLOCK ENTITY */

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof VendingMachineBlockEntity) {
                ItemScatterer.spawn(world, pos, (VendingMachineBlockEntity)blockEntity);
                world.updateComparators(pos,this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), (BlockState)state.with(HALF, DoubleBlockHalf.UPPER), 3);
    }

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return state.get(HALF) == DoubleBlockHalf.LOWER ? blockState.isSideSolidFullSquare(world, blockPos, Direction.UP) : blockState.isOf(this);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VendingMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.VENDING_MACHINE, VendingMachineBlockEntity::tick);
    }
}
