package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.data.DefaultCyberwareUserData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class CyberwareAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CyberwareMod.MOD_ID);

    /**
     * The cyberware installed in an entity. Copying to respawned players is handled manually, since it depends
     * on the {@code cyberware_keepCyberware} game rule.
     */
    public static final Supplier<AttachmentType<CyberwareUserData>> USER_DATA = ATTACHMENT_TYPES.register("user_data",
            () -> AttachmentType.<CyberwareUserData>builder(DefaultCyberwareUserData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, CyberwareUserData>() {
                        @Override
                        public CyberwareUserData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            CyberwareUserData data = new DefaultCyberwareUserData(holder);
                            data.deserializeNBT(provider, tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(CyberwareUserData attachment, HolderLookup.Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .build());

    private CyberwareAttachments() {
    }
}
