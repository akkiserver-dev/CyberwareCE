package flaxbeard.cyberware.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CyberwareNetwork {
    private static final String VERSION = "1";

    private CyberwareNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToClient(CyberwareSyncPayload.TYPE, CyberwareSyncPayload.STREAM_CODEC, CyberwareSyncPayload::handle);
        registrar.playToClient(DodgePayload.TYPE, DodgePayload.STREAM_CODEC, DodgePayload::handle);
        registrar.playToClient(ParticlePayload.TYPE, ParticlePayload.STREAM_CODEC, ParticlePayload::handle);
        registrar.playToClient(SwitchHeldItemPayload.TYPE, SwitchHeldItemPayload.STREAM_CODEC, SwitchHeldItemPayload::handle);
        registrar.playToClient(EngineeringSmashPayload.TYPE, EngineeringSmashPayload.STREAM_CODEC, EngineeringSmashPayload::handle);
        registrar.playToClient(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC, SyncConfigPayload::handle);
        registrar.playToClient(CyberlegFallPayload.TYPE, CyberlegFallPayload.STREAM_CODEC, CyberlegFallPayload::handle);

        registrar.playToServer(SurgeryRemovePayload.TYPE, SurgeryRemovePayload.STREAM_CODEC, SurgeryRemovePayload::handle);
        registrar.playToServer(EngineeringDestroyPayload.TYPE, EngineeringDestroyPayload.STREAM_CODEC, EngineeringDestroyPayload::handle);
        registrar.playToServer(EngineeringSwitchPayload.TYPE, EngineeringSwitchPayload.STREAM_CODEC, EngineeringSwitchPayload::handle);
        registrar.playToServer(SyncHotkeyPayload.TYPE, SyncHotkeyPayload.STREAM_CODEC, SyncHotkeyPayload::handle);
        registrar.playToServer(TriggerActiveAbilityPayload.TYPE, TriggerActiveAbilityPayload.STREAM_CODEC, TriggerActiveAbilityPayload::handle);
        registrar.playToServer(SyncHudDataPayload.TYPE, SyncHudDataPayload.STREAM_CODEC, SyncHudDataPayload::handle);
        registrar.playToServer(OpenRadialMenuPayload.TYPE, OpenRadialMenuPayload.STREAM_CODEC, OpenRadialMenuPayload::handle);
        registrar.playToServer(HudColorPayload.TYPE, HudColorPayload.STREAM_CODEC, HudColorPayload::handle);
        registrar.playToServer(OpenFineManipulatorsPayload.TYPE, OpenFineManipulatorsPayload.STREAM_CODEC, OpenFineManipulatorsPayload::handle);
    }
}
