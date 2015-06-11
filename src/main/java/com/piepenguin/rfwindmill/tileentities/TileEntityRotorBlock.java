package com.piepenguin.rfwindmill.tileentities;

import com.piepenguin.rfwindmill.items.ModItems;
import com.piepenguin.rfwindmill.items.RFWItem;
import com.piepenguin.rfwindmill.lib.ModConfiguration;
import com.piepenguin.rfwindmill.lib.Util;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Tile entity for the {@link com.piepenguin.rfwindmill.blocks.RotorBlock}
 * class which stores rotation and scale data for the attached
 * {@link RenderTileEntityRotorBlock} as well as the type of the rotor used
 * to create it.
 */
public class TileEntityRotorBlock extends TileEntity {

    private float rotation = 0.0f;
    private float scale = 0.0f;
    private static final String NBT_ROTOR_TYPE = "RFWRotorType";
    private int type = 0;
    public static String publicName = "tileEntityRotorBlock";
    public static int ticksPerRotation = 100;
    public static int ticksPerClick = 4;
    public int toRotate = 0;
    public boolean turnedByWind;

    @Override
    public void updateEntity() {
        if(worldObj.isRemote) {
            ForgeDirection turbineDir = Util.intToDirection(getBlockMetadata()).getOpposite();
            int parentX = xCoord + turbineDir.offsetX;
            int parentY = yCoord + turbineDir.offsetY;
            int parentZ = zCoord + turbineDir.offsetZ;
            TileEntityWindmillBlock entity = (TileEntityWindmillBlock)worldObj.getTileEntity(parentX, parentY, parentZ);
            if(entity != null) {
                float generation = entity.getCurrentEnergyGeneration();
                if(generation > 0.01) {
                    turnedByWind = true;
                    rotation += generation / entity.getMaximumEnergyGeneration();
                }
                else {
                    turnedByWind = false;
                }
            }
            // Rotate the rotor by one tick of a hand crank action
            if(toRotate > 0) {
                rotation += 360.0f / ticksPerRotation;
                --toRotate;
            }
            scale = 1.0f;
        }
    }

    public void readSyncableDataFromNBT(NBTTagCompound pNbt) {
        type = pNbt.getInteger(NBT_ROTOR_TYPE);
    }

    public void writeSyncableDataToNBT(NBTTagCompound pNbt) {
        pNbt.setInteger(NBT_ROTOR_TYPE, type);
    }

    @Override
    public void readFromNBT(NBTTagCompound pNbt) {
        super.readFromNBT(pNbt);
        readSyncableDataFromNBT(pNbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound pNbt) {
        super.writeToNBT(pNbt);
        writeSyncableDataToNBT(pNbt);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound syncData = new NBTTagCompound();
        writeSyncableDataToNBT(syncData);

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, syncData);
    }

    @Override
    public void onDataPacket(NetworkManager pNet, S35PacketUpdateTileEntity pPacket) {
        readSyncableDataFromNBT(pPacket.func_148857_g());
    }

    /**
     * Get the rotation of the rotor about the axis normal to the face its
     * attached to.
     * @return Rotation in degrees of the rotor
     */
    public float getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }

    /**
     * Get the tier of the rotor used to make the corresponding block.
     * @return Integer corresponding to the rotor tier used to make the rotor
     */
    public int getType() {
        return type;
    }

    /**
     * Set the tier of the rotor used to make the corresponding block.
     * @param pType Tier of the rotor
     */
    public void setType(int pType) {
        type = pType;
    }

    /**
     * Convert the rotor tier into the correct texture depending on the mod
     * configuration files. Can be forced to always display as the iron
     * texture, or (by default) takes a texture relative to the materials used
     * to make the rotor.
     * @return Array index used by {@link RenderTileEntityRotorBlock} to
     * identify the texture
     */
    public int getTexture() {
        if(ModConfiguration.useIronRotorTexture()) {
            return 0;
        }
        switch(type) {
            default:
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return Util.useThermalExpansion() ? 2 : 4;
            case 3:
                return Util.useThermalExpansion() ? 3 : 5;
        }
    }

    /**
     * Converts the rotor tier into the item used to make it. Only ever four
     * rotors since high tier rotors are conditionally made based on config,
     * so this is a trivial function.
     * @return The rotor item used to make the corresponding
     * {@link com.piepenguin.rfwindmill.blocks.RotorBlock}
     */
    public RFWItem getRotorItem() {
        switch(type) {
            default:
            case 0:
                return ModItems.rotor1;
            case 1:
                return ModItems.rotor2;
            case 2:
                return ModItems.rotor3;
            case 3:
                return ModItems.rotor4;
        }
    }

    /**
     * Rotate the rotor when it's turned by hand. Energy generation is handled
     * by the corresponding {@link TileEntityRotorBlock}, only rotation is
     * updated here. Does nothing if the rotor is generating energy from the
     * wind.
     */
    public void handcrank() {
        if(!turnedByWind && toRotate == 0) {
            toRotate += ticksPerClick;
            rotation += 360.0f / ticksPerRotation;
        }
    }
}
