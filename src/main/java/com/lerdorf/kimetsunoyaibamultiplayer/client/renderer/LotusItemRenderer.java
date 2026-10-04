package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LotusModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.LotusBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class LotusItemRenderer extends GeoItemRenderer<LotusBlockItem> {
    public LotusItemRenderer() {
        super(new LotusModel<>());
    }
}
