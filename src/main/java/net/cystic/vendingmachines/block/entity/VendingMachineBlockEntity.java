package net.cystic.vendingmachines.block.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.cystic.vendingmachines.VendingMachines;
import net.cystic.vendingmachines.item.inventory.ImplementedInventory;
import net.cystic.vendingmachines.screen.slot.VendingMachineScreenHandler;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.GolemLastSeenSensor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.*;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static net.minecraft.entity.InventoryOwner.INVENTORY_KEY;

public class VendingMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory, Merchant{

    private final DefaultedList<ItemStack> inventory =
            DefaultedList.ofSize(3, ItemStack.EMPTY);

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Vending Machine");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new VendingMachineScreenHandler(syncId, playerInventory);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        writeCustomDataToNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        readCustomDataFromNbt(nbt);
    }

    public static void tick(World world, BlockPos pos, BlockState state, VendingMachineBlockEntity entity) {
        if(hasRecipe(entity) && hasNotReachedStackLimit(entity)) {
            craftItem(entity);
        }
    }

    private static void craftItem(VendingMachineBlockEntity entity) {
        entity.removeStack(0, 1);
        entity.removeStack(1, 1);

        entity.setStack(2, new ItemStack(Items.GOLDEN_APPLE,
                entity.getStack(2).getCount() + 1));
    }

    private static boolean hasRecipe(VendingMachineBlockEntity entity) {
        boolean hasItemInFirstSlot = entity.getStack(0).getItem() == Items.EMERALD;
        boolean hasItemInSecondSlot = entity.getStack(1).getItem() == Items.BOOK;

        return hasItemInFirstSlot && hasItemInSecondSlot;
    }

    private static boolean hasNotReachedStackLimit(VendingMachineBlockEntity entity) {
        return entity.getStack(2).getCount() < entity.getStack(2).getMaxCount();
    }

    //Villager stuff

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TrackedData<VillagerData> VILLAGER_DATA;
    public static final int field_30602 = 12;
    public static final Map<Item, Integer> ITEM_FOOD_VALUES;
    private static final int field_30604 = 2;
    private static final Set<Item> GATHERABLE_ITEMS;
    private static final int field_30605 = 10;
    private static final int field_30606 = 1200;
    private static final int field_30607 = 24000;
    private static final int field_30608 = 25;
    private static final int field_30609 = 10;
    private static final int field_30610 = 5;
    private static final long field_30611 = 24000L;
    @VisibleForTesting
    public static final float field_30603 = 0.5F;
    private int levelUpTimer;
    private boolean levelingUp;
    @Nullable
    private PlayerEntity lastCustomer;
    private boolean field_30612;
    private int foodLevel;
    private long gossipStartTime;
    private long lastGossipDecayTime;
    private int experience;
    private long lastRestockTime;
    private int restocksToday;
    private long lastRestockCheckTime;
    private boolean natural;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES;
    private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS;
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>> POINTS_OF_INTEREST;



    private static final TrackedData<Integer> HEAD_ROLLING_TIME_LEFT = DataTracker.registerData(MerchantEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final int field_30599 = 300;
    private static final int INVENTORY_SIZE = 8;
    @Nullable
    private PlayerEntity customer;
    @Nullable
    protected TradeOfferList offers;
    private final SimpleInventory inventory2 = new SimpleInventory(8);



    public static DefaultAttributeContainer.Builder createVillagerAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    public boolean isNatural() {
        return this.natural;
    }

    private void beginTradeWith(PlayerEntity customer) {
        this.prepareOffersFor(customer);
        this.setCustomer(customer);
        this.sendOffers(customer, this.getDisplayName(), this.getVillagerData().getLevel());
    }

    public void setCustomer(@Nullable PlayerEntity customer) {
        boolean bl = this.getCustomer() != null && customer == null;
        this.customer = customer;
        if (bl) {
            this.resetCustomer();
        }

    }

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return null;
    }

    @Override
    public TradeOfferList getOffers() {
        return offers;
    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {

    }

    @Override
    public void trade(TradeOffer offer) {

    }

    @Override
    public void onSellingItem(ItemStack stack) {

    }

    protected void resetCustomer() {
        this.setCustomer(null);
        this.clearSpecialPrices();
    }

    private void clearSpecialPrices() {
        Iterator var1 = this.getOffers().iterator();

        while(var1.hasNext()) {
            TradeOffer tradeOffer = (TradeOffer)var1.next();
            tradeOffer.clearSpecialPrice();
        }

    }

    public boolean canRefreshTrades() {
        return true;
    }

    public boolean isClient() {
        return this.getWorld().isClient;
    }

    public void restock() {
        this.updateDemandBonus();
        Iterator var1 = this.getOffers().iterator();

        while(var1.hasNext()) {
            TradeOffer tradeOffer = (TradeOffer)var1.next();
            tradeOffer.resetUses();
        }

        this.sendOffersToCustomer();
        this.lastRestockTime = this.getWorld().getTime();
        ++this.restocksToday;
    }

    private void sendOffersToCustomer() {
        TradeOfferList tradeOfferList = this.getOffers();
        PlayerEntity playerEntity = this.getCustomer();
        if (playerEntity != null && !tradeOfferList.isEmpty()) {
            playerEntity.sendTradeOffers(playerEntity.currentScreenHandler.syncId, tradeOfferList, this.getVillagerData().getLevel(), this.getExperience(), this.isLeveledMerchant(), this.canRefreshTrades());
        }

    }

    private boolean needsRestock() {
        Iterator var1 = this.getOffers().iterator();

        TradeOffer tradeOffer;
        do {
            if (!var1.hasNext()) {
                return false;
            }

            tradeOffer = (TradeOffer)var1.next();
        } while(!tradeOffer.hasBeenUsed());

        return true;
    }

    private boolean canRestock() {
        return this.restocksToday == 0 || this.restocksToday < 2 && this.getWorld().getTime() > this.lastRestockTime + 2400L;
    }

    public boolean shouldRestock() {
        long l = this.lastRestockTime + 12000L;
        long m = this.getWorld().getTime();
        boolean bl = m > l;
        long n = this.getWorld().getTimeOfDay();
        if (this.lastRestockCheckTime > 0L) {
            long o = this.lastRestockCheckTime / 24000L;
            long p = n / 24000L;
            bl |= p > o;
        }

        this.lastRestockCheckTime = n;
        if (bl) {
            this.lastRestockTime = m;
            this.clearDailyRestockCount();
        }

        return this.canRestock() && this.needsRestock();
    }

    private void restockAndUpdateDemandBonus() {
        int i = 2 - this.restocksToday;
        if (i > 0) {
            Iterator var2 = this.getOffers().iterator();

            while(var2.hasNext()) {
                TradeOffer tradeOffer = (TradeOffer)var2.next();
                tradeOffer.resetUses();
            }
        }

        for(int j = 0; j < i; ++j) {
            this.updateDemandBonus();
        }

        this.sendOffersToCustomer();
    }

    private void updateDemandBonus() {
        Iterator var1 = this.getOffers().iterator();

        while(var1.hasNext()) {
            TradeOffer tradeOffer = (TradeOffer)var1.next();
            tradeOffer.updateDemandBonus();
        }

    }

    private void prepareOffersFor(PlayerEntity player) {
        int i = 1;
        if (i != 0) {
            Iterator var3 = this.getOffers().iterator();

            while(var3.hasNext()) {
                TradeOffer tradeOffer = (TradeOffer)var3.next();
                tradeOffer.increaseSpecialPrice(-MathHelper.floor((float)i * tradeOffer.getPriceMultiplier()));
            }
        }

        if (player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            StatusEffectInstance statusEffectInstance = player.getStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
            int j = statusEffectInstance.getAmplifier();
            Iterator var5 = this.getOffers().iterator();

            while(var5.hasNext()) {
                TradeOffer tradeOffer2 = (TradeOffer)var5.next();
                double d = 0.3 + 0.0625 * (double)j;
                int k = (int)Math.floor(d * (double)tradeOffer2.getOriginalFirstBuyItem().getCount());
                tradeOffer2.increaseSpecialPrice(-Math.max(k, 1));
            }
        }

    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        TradeOfferList tradeOfferList = this.getOffers();
        if (!tradeOfferList.isEmpty()) {
            nbt.put("Offers", tradeOfferList.toNbt());
        }
        nbt.put(INVENTORY_KEY, this.inventory2.toNbtList());

        DataResult var10000 = VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData());
        Logger var10001 = LOGGER;
        Objects.requireNonNull(var10001);
        //var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
        //    nbt.put("VillagerData", nbtElement);
        //});
        nbt.putByte("FoodLevel", (byte)this.foodLevel);
        nbt.putInt("Xp", this.experience);
        nbt.putLong("LastRestock", this.lastRestockTime);
        nbt.putLong("LastGossipDecay", this.lastGossipDecayTime);
        nbt.putInt("RestocksToday", this.restocksToday);
        if (this.natural) {
            nbt.putBoolean("AssignProfessionWhenSpawned", true);
        }

    }

    public void readCustomDataFromNbt(NbtCompound nbt) {

        if (nbt.contains("Offers", NbtElement.COMPOUND_TYPE)) {
            this.offers = new TradeOfferList(nbt.getCompound("Offers"));
        }
        if (nbt.contains(INVENTORY_KEY, NbtElement.LIST_TYPE)) {
            this.inventory2.readNbtList(nbt.getList(INVENTORY_KEY, NbtElement.COMPOUND_TYPE));
        }
        if (nbt.contains("VillagerData", 10)) {
            DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic(NbtOps.INSTANCE, nbt.get("VillagerData")));
            Logger var10001 = LOGGER;
            Objects.requireNonNull(var10001);
            dataResult.resultOrPartial(var10001::error).ifPresent(this::setVillagerData);
        }

        if (nbt.contains("Offers", 10)) {
            this.offers = new TradeOfferList(nbt.getCompound("Offers"));
        }

        if (nbt.contains("FoodLevel", 1)) {
            this.foodLevel = nbt.getByte("FoodLevel");
        }

        NbtList nbtList = nbt.getList("Gossips", 10);
        if (nbt.contains("Xp", 3)) {
            this.experience = nbt.getInt("Xp");
        }

        this.lastRestockTime = nbt.getLong("LastRestock");
        this.lastGossipDecayTime = nbt.getLong("LastGossipDecay");

        this.restocksToday = nbt.getInt("RestocksToday");
        if (nbt.contains("AssignProfessionWhenSpawned")) {
            this.natural = nbt.getBoolean("AssignProfessionWhenSpawned");
        }

    }

    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    public void setVillagerData(VillagerData villagerData) {
        VillagerData villagerData2 = this.getVillagerData();
        if (villagerData2.getProfession() != villagerData.getProfession()) {
            this.offers = null;
        }

        //this.dataTracker.set(VILLAGER_DATA, villagerData);
    }

    public VillagerData getVillagerData() {
        //return (VillagerData)this.dataTracker.get(VILLAGER_DATA);
        return null;
    }

    protected void afterUsing(TradeOffer offer) {
        int i = 3 + Random.create().nextInt(4);
        this.experience += offer.getMerchantExperience();
        this.lastCustomer = this.getCustomer();
        if (this.canLevelUp()) {
            this.levelUpTimer = 40;
            this.levelingUp = true;
            i += 5;
        }
    }

    public void method_35201(boolean bl) {
        this.field_30612 = bl;
    }

    public boolean method_35200() {
        return this.field_30612;
    }

    private boolean lacksFood() {
        return this.foodLevel < 12;
    }

    private void depleteFood(int amount) {
        this.foodLevel -= amount;
    }

    public void setOffers(TradeOfferList offers) {
        this.offers = offers;
    }

    private boolean canLevelUp() {
        int i = this.getVillagerData().getLevel();
        return VillagerData.canLevelUp(i) && this.experience >= VillagerData.getUpperLevelExperience(i);
    }

    private void levelUp() {
        this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().getLevel() + 1));
        this.fillRecipes();
    }

    protected Text getDefaultName() {
        return Text.translatable("no idea" + "." + Registries.VILLAGER_PROFESSION.getId(this.getVillagerData().getProfession()).getPath());
    }



    protected void fillRecipes() {
        VillagerData villagerData = this.getVillagerData();
        Int2ObjectMap<TradeOffers.Factory[]> int2ObjectMap = (Int2ObjectMap)TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(villagerData.getProfession());
        if (int2ObjectMap != null && !int2ObjectMap.isEmpty()) {
            TradeOffers.Factory[] factorys = (TradeOffers.Factory[])int2ObjectMap.get(villagerData.getLevel());
            if (factorys != null) {
                TradeOfferList tradeOfferList = this.getOffers();
                this.fillRecipesFromPool(tradeOfferList, factorys, 2);
            }
        }
    }

    protected void fillRecipesFromPool(TradeOfferList recipeList, TradeOffers.Factory[] pool, int count) {
        HashSet<Integer> set = Sets.newHashSet();
        if (pool.length > count) {
            while (set.size() < count) {
                set.add(Random.create().nextInt(pool.length));
            }
        } else {
            for (int i = 0; i < pool.length; ++i) {
                set.add(i);
            }
        }
        for (Integer integer : set) {
            TradeOffers.Factory factory = pool[integer];
            TradeOffer tradeOffer = factory.create(this.getCustomer(), Random.create());
            if (tradeOffer == null) continue;
            recipeList.add(tradeOffer);
        }
    }

    public int getExperience() {
        return this.experience;
    }

    @Override
    public void setExperienceFromServer(int experience) {

    }

    @Override
    public boolean isLeveledMerchant() {
        return false;
    }

    @Override
    public SoundEvent getYesSound() {
        return null;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    private void clearDailyRestockCount() {
        this.restockAndUpdateDemandBonus();
        this.restocksToday = 0;
    }

    static {
        VILLAGER_DATA = DataTracker.registerData(VillagerEntity.class, TrackedDataHandlerRegistry.VILLAGER_DATA);
        ITEM_FOOD_VALUES = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
        GATHERABLE_ITEMS = ImmutableSet.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, new Item[]{Items.BEETROOT_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD});
        MEMORY_MODULES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, new MemoryModuleType[]{MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE, MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN, MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY});
        SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);
        POINTS_OF_INTEREST = ImmutableMap.of(MemoryModuleType.HOME, (villager, registryEntry) -> {
            return registryEntry.matchesKey(PointOfInterestTypes.HOME);
        }, MemoryModuleType.JOB_SITE, (villager, registryEntry) -> {
            return villager.getVillagerData().getProfession().heldWorkstation().test(registryEntry);
        }, MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> {
            return VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(registryEntry);
        }, MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> {
            return registryEntry.matchesKey(PointOfInterestTypes.MEETING);
        });
    }

}
