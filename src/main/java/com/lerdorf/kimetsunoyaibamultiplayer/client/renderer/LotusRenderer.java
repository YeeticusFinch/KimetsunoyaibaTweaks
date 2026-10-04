package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LotusBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LotusModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class LotusRenderer extends GeoBlockRenderer<LotusBlockEntity> {
    public LotusRenderer(BlockEntityRendererProvider.Context context) {
        super(new LotusModel<>());
        addRenderLayer(new LotusLilypadLayer(this));
    }

    @Override
    public RenderType getRenderType(LotusBlockEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }
}
