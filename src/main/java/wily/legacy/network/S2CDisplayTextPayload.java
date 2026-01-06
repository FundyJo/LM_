package wily.legacy.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

public record S2CDisplayTextPayload(Component message) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<S2CDisplayTextPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("display_text_s2c"), S2CDisplayTextPayload::new);

    public S2CDisplayTextPayload(CommonNetwork.PlayBuf buf) {
        this(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf.get()));
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf.get(), message);
    }

    @Override
    public void apply(Context context) {
        TopMessage.setSmall(new TopMessage(message, 0xffffffff, 40, true, true, false));
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
