package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MicroscopeStationSavedData extends SavedData {
    public static final int LENS_SLOT = 0;
    public static final int ITEM_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int IDENTIFY_TIME_TOTAL = 50;

    private static final String DATA_NAME = "kny_microscope_stations";

    private final Map<BlockPos, StationState> stations = new HashMap<>();

    public static MicroscopeStationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            MicroscopeStationSavedData::load,
            MicroscopeStationSavedData::new,
            DATA_NAME
        );
    }

    public static Container containerFor(ServerLevel level, BlockPos pos) {
        return get(level).container(pos.immutable());
    }

    public static void dropAndClear(ServerLevel level, BlockPos pos) {
        MicroscopeStationSavedData data = get(level);
        StationState state = data.stations.remove(pos.immutable());
        if (state != null) {
            if (!state.isEmpty()) {
                Containers.dropContents(level, pos, new StaticStationContainer(state.items));
            }
            data.setDirty();
        }
    }

    private static MicroscopeStationSavedData load(CompoundTag tag) {
        MicroscopeStationSavedData data = new MicroscopeStationSavedData();
        if (!tag.contains("stations", Tag.TAG_LIST)) {
            return data;
        }

        ListTag stationsTag = tag.getList("stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < stationsTag.size(); i++) {
            CompoundTag stationTag = stationsTag.getCompound(i);
            if (!stationTag.contains("pos", Tag.TAG_LONG)) {
                continue;
            }

            StationState state = new StationState();
            ContainerHelper.loadAllItems(stationTag, state.items);
            state.identifyTime = stationTag.getInt("IdentifyTime");
            data.stations.put(BlockPos.of(stationTag.getLong("pos")), state);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag stationsTag = new ListTag();
        for (Map.Entry<BlockPos, StationState> entry : stations.entrySet()) {
            StationState state = entry.getValue();
            if (state.isEmpty()) {
                continue;
            }

            CompoundTag stationTag = new CompoundTag();
            stationTag.putLong("pos", entry.getKey().asLong());
            stationTag.putInt("IdentifyTime", state.identifyTime);
            ContainerHelper.saveAllItems(stationTag, state.items);
            stationsTag.add(stationTag);
        }
        tag.put("stations", stationsTag);
        return tag;
    }

    public void tick(ServerLevel level) {
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, StationState>> iterator = stations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, StationState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            StationState state = entry.getValue();

            if (!BloodDemonArtAlchemyCatalog.isMicroscopeBlock(level.getBlockState(pos))) {
                if (!state.isEmpty()) {
                    Containers.dropContents(level, pos, new StaticStationContainer(state.items));
                }
                iterator.remove();
                changed = true;
                continue;
            }

            if (!canIdentify(level, state)) {
                if (state.identifyTime != 0) {
                    state.identifyTime = 0;
                    changed = true;
                }
                continue;
            }

            state.identifyTime++;
            changed = true;
            if (state.identifyTime >= IDENTIFY_TIME_TOTAL) {
                ItemStack output = microscopeOutput(level, state.items);
                if (!output.isEmpty()) {
                    state.items.set(ITEM_SLOT, output);
                    damageLens(state.items);
                }
                state.identifyTime = 0;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    private static boolean canIdentify(ServerLevel level, StationState state) {
        if (state.items.get(LENS_SLOT).isEmpty() || state.items.get(ITEM_SLOT).isEmpty()) {
            return false;
        }

        SimpleContainer input = new SimpleContainer(2);
        input.setItem(0, state.items.get(LENS_SLOT));
        input.setItem(1, state.items.get(ITEM_SLOT));
        return level.getRecipeManager()
            .getRecipeFor(ModAlchemyRecipes.MICROSCOPE_TYPE, input, level)
            .isPresent();
    }

    private static ItemStack microscopeOutput(ServerLevel level, NonNullList<ItemStack> items) {
        SimpleContainer input = new SimpleContainer(2);
        input.setItem(0, items.get(LENS_SLOT));
        input.setItem(1, items.get(ITEM_SLOT));

        ItemStack dynamicOutput = BloodDemonArtAlchemyCatalog.microscopeOutput(items.get(ITEM_SLOT), level.random);
        if (!dynamicOutput.isEmpty()) {
            return dynamicOutput;
        }

        List<MicroscopeRecipe> matches = new ArrayList<>();
        for (MicroscopeRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModAlchemyRecipes.MICROSCOPE_TYPE)) {
            if (recipe.matches(input, level)) {
                matches.add(recipe);
            }
        }

        if (matches.isEmpty()) {
            return ItemStack.EMPTY;
        }

        MicroscopeRecipe selected = matches.get(level.random.nextInt(matches.size()));
        return selected.assemble(input, level.registryAccess());
    }

    private static void damageLens(NonNullList<ItemStack> items) {
        ItemStack lens = items.get(LENS_SLOT);
        if (lens.isEmpty()) {
            return;
        }

        if (lens.isDamageableItem()) {
            lens.setDamageValue(lens.getDamageValue() + 1);
            if (lens.getDamageValue() >= lens.getMaxDamage()) {
                items.set(LENS_SLOT, ItemStack.EMPTY);
            }
            return;
        }

        lens.shrink(1);
        if (lens.isEmpty()) {
            items.set(LENS_SLOT, ItemStack.EMPTY);
        }
    }

    private Container container(BlockPos pos) {
        return new StationContainer(pos);
    }

    private StationState stateFor(BlockPos pos) {
        return stations.computeIfAbsent(pos, ignored -> new StationState());
    }

    private static final class StationState {
        private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        private int identifyTime;

        private boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private final class StationContainer implements Container {
        private final BlockPos pos;

        private StationContainer(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public int getContainerSize() {
            return SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return stateFor(pos).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < SLOT_COUNT ? stateFor(pos).items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(stateFor(pos).items, slot, amount);
            if (!removed.isEmpty()) {
                resetProgress();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = ContainerHelper.takeItem(stateFor(pos).items, slot);
            if (!removed.isEmpty()) {
                resetProgress();
            }
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return;
            }

            StationState state = stateFor(pos);
            state.items.set(slot, stack);
            int maxStackSize = slot == ITEM_SLOT ? 1 : getMaxStackSize();
            if (!stack.isEmpty() && stack.getCount() > maxStackSize) {
                stack.setCount(maxStackSize);
            }
            resetProgress();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void setChanged() {
            setDirty();
        }

        @Override
        public void clearContent() {
            StationState state = stateFor(pos);
            for (int i = 0; i < state.items.size(); i++) {
                state.items.set(i, ItemStack.EMPTY);
            }
            resetProgress();
        }

        private void resetProgress() {
            StationState state = stateFor(pos);
            state.identifyTime = 0;
            if (state.isEmpty()) {
                stations.remove(pos);
            }
            setDirty();
        }
    }

    private static final class StaticStationContainer implements Container {
        private final NonNullList<ItemStack> items;

        private StaticStationContainer(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ContainerHelper.removeItem(items, slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, ItemStack.EMPTY);
            }
        }
    }
}
