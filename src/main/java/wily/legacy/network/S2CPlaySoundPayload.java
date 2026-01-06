package wily.legacy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.util.client.LegacySoundUtil;

/**
 * Server-to-Client Payload um einen UI-Sound beim Spieler abzuspielen.
 */
public record S2CPlaySoundPayload(ResourceLocation soundId, float volume, float pitch) implements CommonNetwork.Payload {

    public static final CommonNetwork.Identifier<S2CPlaySoundPayload> ID =
        CommonNetwork.Identifier.create(Legacy4J.createModLocation("play_sound_s2c"), S2CPlaySoundPayload::new);

    public S2CPlaySoundPayload(CommonNetwork.PlayBuf buf) {
        this(buf.get().readResourceLocation(), buf.get().readFloat(), buf.get().readFloat());
    }

    /**
     * Erstellt ein Payload aus einem SoundEvent
     */
    public static S2CPlaySoundPayload of(SoundEvent sound, float volume, float pitch) {
        return new S2CPlaySoundPayload(sound.location(), volume, pitch);
    }

    /**
     * Erstellt ein Payload aus einem SoundEvent mit Standard-Pitch
     */
    public static S2CPlaySoundPayload of(SoundEvent sound, float volume) {
        return of(sound, volume, 1.0f);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeResourceLocation(soundId);
        buf.get().writeFloat(volume);
        buf.get().writeFloat(pitch);
    }

    @Override
    public void apply(Context context) {
        // Wird auf dem Client ausgeführt
        SoundEvent sound = SoundEvent.createVariableRangeEvent(soundId);
        LegacySoundUtil.playSimpleUISound(sound, volume, pitch, false);
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}

