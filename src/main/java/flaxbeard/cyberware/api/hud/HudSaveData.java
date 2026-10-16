package flaxbeard.cyberware.api.hud;

import net.minecraft.nbt.CompoundTag;

/**
 * Key-value storage for the settings of a HUD element, backed by NBT.
 */
public record HudSaveData(CompoundTag tag) {
    public HudSaveData() {
        this(new CompoundTag());
    }

    public void setString(String key, String value) {
        tag.putString(key, value);
    }

    public String getString(String key) {
        return tag.getString(key);
    }

    public void setInteger(String key, int value) {
        tag.putInt(key, value);
    }

    public int getInteger(String key) {
        return tag.getInt(key);
    }

    public void setBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    public boolean getBoolean(String key) {
        return tag.getBoolean(key);
    }

    public void setFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    public float getFloat(String key) {
        return tag.getFloat(key);
    }
}
