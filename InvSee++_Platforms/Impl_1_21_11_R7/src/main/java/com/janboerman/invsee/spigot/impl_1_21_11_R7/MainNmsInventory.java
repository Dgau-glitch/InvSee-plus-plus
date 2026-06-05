package com.janboerman.invsee.spigot.impl_1_21_11_R7;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.Scheduler;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import com.janboerman.invsee.spigot.internal.inventory.AbstractNmsInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;

import java.util.ArrayList;
import java.util.List;

class MainNmsInventory extends AbstractNmsInventory<PlayerInventorySlot, MainBukkitInventory, MainNmsInventory> implements Container, MenuProvider {

	private static final int TARGET_INVENTORY_END = 43;
	private static final int CURSOR_SLOT = 43;
	private static final int PERSONAL_START = 45;
	private static final int SNAPSHOT_SIZE = 54;

	protected final List<ItemStack> storageContents;
	private final Scheduler scheduler;

	protected MainNmsInventory(Player target, CreationOptions<PlayerInventorySlot> creationOptions, Scheduler scheduler) {
		super(target.getUUID(), target.getScoreboardName(), creationOptions);
		this.storageContents = NonNullList.withSize(SNAPSHOT_SIZE, ItemStack.EMPTY);
		this.scheduler = scheduler;
		copyFromTarget(target);
		this.maxStack = target.getInventory().getMaxStackSize();
	}

	@Override
	protected MainBukkitInventory createBukkit() {
		return new MainBukkitInventory(this);
	}

	@Override
	public void setMaxStackSize(int size) {
		this.maxStack = size;
	}

	//vanilla
	@Override
	public int getMaxStackSize() {
		return maxStack;
	}

	@Override
	public int defaultMaxStack() {
		return Container.MAX_STACK;
	}

	@Override
	public void shallowCopyFrom(MainNmsInventory from) {
		setMaxStackSize(from.getMaxStackSize());
		this.storageContents.clear();
		this.storageContents.addAll(from.storageContents);
		setChanged();
	}

	private void copyFromTarget(Player target) {
		List<ItemStack> targetContents = target.getInventory().getContents();
		for (int slot = 0; slot < Math.min(TARGET_INVENTORY_END, targetContents.size()); slot++) {
			storageContents.set(slot, targetContents.get(slot).copy());
		}
		storageContents.set(CURSOR_SLOT, target.containerMenu.getCarried().copy());

		List<ItemStack> personalContents = target.inventoryMenu.getCraftSlots().getContents();
		for (int slot = 0; slot < Math.min(9, personalContents.size()); slot++) {
			storageContents.set(PERSONAL_START + slot, personalContents.get(slot).copy());
		}
	}

	//vanilla
	@Override
	public void clearContent() {
		for (int slot = 0; slot < storageContents.size(); slot++) {
			storageContents.set(slot, ItemStack.EMPTY);
		}
	}

	//vanilla
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player viewer) {
		return new MainNmsContainer(containerId, this, playerInventory, viewer, creationOptions, scheduler);
	}

	//vanilla
	@Override
	public Component getDisplayName() {
		//return new TextComponent("minecraft:generic_9x6");
		return CraftChatMessage.fromStringOrNull(creationOptions.getTitle().titleFor(Target.byGameProfile(targetPlayerUuid, targetPlayerName)));
	}

	//vanilla
	@Override
	public int getContainerSize() {
		return SNAPSHOT_SIZE;
	}

	//craftbukkit
	@Override
	public List<ItemStack> getContents() {
		return storageContents;
	}

	//vanilla
	@Override
	public ItemStack getItem(int slot) {
		if (slot < 0 || slot >= getContainerSize()) return ItemStack.EMPTY;

		return storageContents.get(slot);
	}

	//vanilla
	@Override
	public boolean isEmpty() {
		for (ItemStack stack : storageContents) {
			if (!stack.isEmpty()) return false;
		}
		return true;
	}

	//craftbukkit
	@Override
	public void onClose(CraftHumanEntity bukkitPlayer) {
		super.onClose(bukkitPlayer);
	}

	//craftbukkit
	@Override
	public void onOpen(CraftHumanEntity bukkitPlayer) {
		super.onOpen(bukkitPlayer);
	}

	//vanilla
	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (slot < 0 || slot >= getContainerSize()) return ItemStack.EMPTY;

		ItemStack stack = storageContents.get(slot);
		if (!stack.isEmpty() && amount > 0) {
			ItemStack oldStackCopy = stack.split(amount);
			if (!oldStackCopy.isEmpty()) {
				setChanged();
			}
			return oldStackCopy;
		} else {
			return ItemStack.EMPTY;
		}
	}

	//vanilla
	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot < 0 || slot >= getContainerSize()) return ItemStack.EMPTY;

		ItemStack stack = storageContents.get(slot);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			storageContents.set(slot, ItemStack.EMPTY);
			return stack;
		}
	}

	//vanilla
	@Override
	public void setChanged() {
		// Snapshot inventory; the container transaction service commits changes to the live target.
	}

	//vanilla
	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot < 0 || slot >= getContainerSize()) return;

		storageContents.set(slot, stack);
		if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
			stack.setCount(getMaxStackSize());
		}

		setChanged();
	}

	//vanilla
	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	List<org.bukkit.inventory.ItemStack> snapshotBukkit() {
		List<org.bukkit.inventory.ItemStack> snapshot = new ArrayList<>(SNAPSHOT_SIZE);
		for (ItemStack stack : storageContents) {
			snapshot.add(CraftItemStack.asBukkitCopy(stack));
		}
		return snapshot;
	}

	void resyncFromBukkitSnapshot(List<org.bukkit.inventory.ItemStack> snapshot) {
		for (int slot = 0; slot < Math.min(SNAPSHOT_SIZE, snapshot.size()); slot++) {
			storageContents.set(slot, CraftItemStack.asNMSCopy(snapshot.get(slot)));
		}
		setChanged();
	}

	static List<org.bukkit.inventory.ItemStack> snapshotTarget(Player target) {
		List<org.bukkit.inventory.ItemStack> snapshot = new ArrayList<>(SNAPSHOT_SIZE);
		List<ItemStack> targetContents = target.getInventory().getContents();
		for (int slot = 0; slot < TARGET_INVENTORY_END; slot++) {
			ItemStack stack = slot < targetContents.size() ? targetContents.get(slot) : ItemStack.EMPTY;
			snapshot.add(CraftItemStack.asBukkitCopy(stack));
		}
		snapshot.add(CraftItemStack.asBukkitCopy(target.containerMenu.getCarried()));
		snapshot.add(CraftItemStack.asBukkitCopy(ItemStack.EMPTY));
		List<ItemStack> personalContents = target.inventoryMenu.getCraftSlots().getContents();
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = slot < personalContents.size() ? personalContents.get(slot) : ItemStack.EMPTY;
			snapshot.add(CraftItemStack.asBukkitCopy(stack));
		}
		return snapshot;
	}

	static void applyToTarget(Player target, List<org.bukkit.inventory.ItemStack> snapshot) {
		List<ItemStack> targetContents = target.getInventory().getContents();
		for (int slot = 0; slot < Math.min(TARGET_INVENTORY_END, targetContents.size()); slot++) {
			target.getInventory().setItem(slot, CraftItemStack.asNMSCopy(snapshot.get(slot)));
		}
		target.containerMenu.setCarried(CraftItemStack.asNMSCopy(snapshot.get(CURSOR_SLOT)));

		List<ItemStack> personalContents = target.inventoryMenu.getCraftSlots().getContents();
		for (int slot = 0; slot < Math.min(9, personalContents.size()); slot++) {
			personalContents.set(slot, CraftItemStack.asNMSCopy(snapshot.get(PERSONAL_START + slot)));
		}
		target.getInventory().setChanged();
	}

}
